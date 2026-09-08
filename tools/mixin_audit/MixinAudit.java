// Checks this port's mixins against the 1.20.1 classes they actually target.
//
// The port's mixins were copied from a 1.21.1 codebase, where the same method
// can have a different signature, a different body, and therefore different
// local variable ordinals. Mixin only reports the first such mismatch, and only
// when the game applies it, so finding them by launching is one boot per defect.
// This reads the compiled mixins with ASM instead and reports every mismatch in
// one pass, without running anything.
//
// What it checks, per mixin:
//   - the target class resolves on the dev classpath
//   - every @Shadow field and method exists on the target
//   - every @Accessor and @Invoker names a member that exists
//   - every injector's `method =` selector matches a real target method
//   - every @At(INVOKE/FIELD) target appears in that method's bytecode, at the
//     requested ordinal
//   - every @Inject handler's leading parameters match the target's parameters
//   - every @Local(ordinal) resolves to a local of that type which is actually
//     live at the injection point
//   - no @Local(name) addresses a vanilla local, whose name survives only in the
//     development jar and is obfuscated in a real installation
//
// Usage: java -cp <asm jars> MixinAudit <mixin-classes-dir> <classpath-file>
package tools.mixin_audit;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class MixinAudit {

    // ---- annotation descriptors -------------------------------------------------

    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String REDIRECT = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
    private static final String MODIFY_CONSTANT = "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;";
    private static final String MODIFY_ARG = "Lorg/spongepowered/asm/mixin/injection/ModifyArg;";
    private static final String MODIFY_VARIABLE = "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;";
    private static final String WRAP_OPERATION =
            "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";
    private static final String MODIFY_EXPR =
            "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;";
    private static final String MODIFY_RETURN =
            "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;";
    private static final String WRAP_WITH_CONDITION =
            "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;";
    private static final String LOCAL = "Lcom/llamalad7/mixinextras/sugar/Local;";
    private static final String SHARE = "Lcom/llamalad7/mixinextras/sugar/Share;";
    private static final String CANCELLABLE_CI =
            "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
    private static final String CI_RETURNABLE =
            "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;";
    private static final String COERCE = "Lorg/spongepowered/asm/mixin/injection/Coerce;";
    private static final String OPERATION =
            "Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;";

    private static final Set<String> INJECTORS = new LinkedHashSet<>(Arrays.asList(
            INJECT, REDIRECT, MODIFY_CONSTANT, MODIFY_ARG, MODIFY_VARIABLE,
            WRAP_OPERATION, MODIFY_EXPR, MODIFY_RETURN, WRAP_WITH_CONDITION));

    // ---- classpath --------------------------------------------------------------

    /** Every jar on the dev classpath, kept open so classes can be read on demand. */
    private final List<ZipFile> jars = new ArrayList<>();
    /** internal name -> the jar holding it. First jar to declare a class wins. */
    private final Map<String, ZipFile> index = new HashMap<>();
    private final Map<String, ClassNode> cache = new HashMap<>();

    private final List<String> problems = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private int mixinCount;
    private int checkCount;

    public static void main(final String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: MixinAudit <mixin-classes-dir> <classpath-file>");
            System.exit(2);
        }
        final MixinAudit audit = new MixinAudit();
        audit.loadClasspath(Paths.get(args[1]));
        audit.run(Paths.get(args[0]));
        audit.report();
    }

    private void loadClasspath(final Path listing) throws IOException {
        for (final String line : Files.readAllLines(listing)) {
            final String path = line.trim();
            if (path.isEmpty() || !path.endsWith(".jar")) continue;
            final Path p = Paths.get(path);
            if (!Files.exists(p)) continue;
            try {
                final ZipFile zip = new ZipFile(p.toFile());
                this.jars.add(zip);
                final Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry e = entries.nextElement();
                    final String name = e.getName();
                    if (name.endsWith(".class")) {
                        this.index.putIfAbsent(name.substring(0, name.length() - 6), zip);
                    }
                }
            } catch (final IOException ignored) {
                // A jar that will not open is not this tool's problem to report.
            }
        }
    }

    private ClassNode resolve(final String internalName) {
        if (this.cache.containsKey(internalName)) return this.cache.get(internalName);
        ClassNode node = null;
        final ZipFile zip = this.index.get(internalName);
        if (zip != null) {
            try (InputStream in = zip.getInputStream(zip.getEntry(internalName + ".class"))) {
                final ClassNode n = new ClassNode();
                new ClassReader(in).accept(n, ClassReader.EXPAND_FRAMES);
                node = n;
            } catch (final IOException ignored) {
            }
        }
        this.cache.put(internalName, node);
        return node;
    }

    // ---- driving ----------------------------------------------------------------

    private void run(final Path classesDir) throws IOException {
        final List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> ignored = Files.newDirectoryStream(classesDir)) {
            Files.walk(classesDir)
                    .filter(p -> p.toString().endsWith(".class"))
                    .sorted()
                    .forEach(files::add);
        }
        for (final Path f : files) {
            final ClassNode mixin = new ClassNode();
            try (InputStream in = Files.newInputStream(f)) {
                new ClassReader(in).accept(mixin, ClassReader.EXPAND_FRAMES);
            }
            audit(mixin);
        }
    }

    private void audit(final ClassNode mixin) {
        final AnnotationNode mixinAnn = findAnn(mixin.visibleAnnotations, MIXIN);
        final AnnotationNode mixinAnnI = mixinAnn != null ? mixinAnn : findAnn(mixin.invisibleAnnotations, MIXIN);
        if (mixinAnnI == null) return;
        this.mixinCount++;

        final List<String> targets = new ArrayList<>();
        final Object value = annValue(mixinAnnI, "value");
        if (value instanceof List) {
            for (final Object o : (List<?>) value) {
                if (o instanceof Type) targets.add(((Type) o).getInternalName());
            }
        }
        final Object stringTargets = annValue(mixinAnnI, "targets");
        if (stringTargets instanceof List) {
            for (final Object o : (List<?>) stringTargets) {
                targets.add(String.valueOf(o).replace('.', '/'));
            }
        }
        if (targets.isEmpty()) {
            problem(mixin, "@Mixin names no target");
            return;
        }

        final List<ClassNode> resolved = new ArrayList<>();
        for (final String t : targets) {
            final ClassNode node = resolve(t);
            if (node == null) {
                // A mixin into another mod's class that is not on the compile
                // classpath cannot be checked; say so rather than passing it.
                note(mixin, "target " + t.replace('/', '.') + " is not on the compile classpath; not checked");
            } else {
                resolved.add(node);
            }
        }
        if (resolved.isEmpty()) return;

        for (final FieldNode field : mixin.fields) {
            if (findAny(field.visibleAnnotations, field.invisibleAnnotations, SHADOW) != null) {
                for (final ClassNode target : resolved) checkShadowField(mixin, target, field);
            }
        }
        for (final MethodNode method : mixin.methods) {
            final AnnotationNode shadow = findAny(method.visibleAnnotations, method.invisibleAnnotations, SHADOW);
            if (shadow != null) {
                for (final ClassNode target : resolved) checkShadowMethod(mixin, target, method);
                continue;
            }
            final AnnotationNode accessor = findAny(method.visibleAnnotations, method.invisibleAnnotations, ACCESSOR);
            if (accessor != null) {
                for (final ClassNode target : resolved) checkAccessor(mixin, target, method, accessor);
                continue;
            }
            final AnnotationNode invoker = findAny(method.visibleAnnotations, method.invisibleAnnotations, INVOKER);
            if (invoker != null) {
                for (final ClassNode target : resolved) checkInvoker(mixin, target, method, invoker);
                continue;
            }
            for (final String injector : INJECTORS) {
                final AnnotationNode ann = findAny(method.visibleAnnotations, method.invisibleAnnotations, injector);
                if (ann != null) {
                    for (final ClassNode target : resolved) checkInjector(mixin, target, method, ann, injector);
                    break;
                }
            }
        }
    }

    // ---- member checks ----------------------------------------------------------

    private void checkShadowField(final ClassNode mixin, final ClassNode target, final FieldNode field) {
        this.checkCount++;
        for (final FieldNode f : target.fields) {
            if (f.name.equals(field.name)) {
                if (!f.desc.equals(field.desc)) {
                    problem(mixin, "@Shadow field " + field.name + " is " + pretty(field.desc)
                            + " here but " + pretty(f.desc) + " on " + shortName(target));
                }
                return;
            }
        }
        if (inheritedField(target, field.name) != null) return;
        problem(mixin, "@Shadow field " + field.name + " does not exist on " + shortName(target));
    }

    private void checkShadowMethod(final ClassNode mixin, final ClassNode target, final MethodNode method) {
        this.checkCount++;
        if (hasMethod(target, method.name, method.desc, true)) return;
        problem(mixin, "@Shadow method " + method.name + pretty(method.desc)
                + " does not exist on " + shortName(target));
    }

    private void checkAccessor(final ClassNode mixin, final ClassNode target,
                               final MethodNode method, final AnnotationNode ann) {
        this.checkCount++;
        String name = (String) annValue(ann, "value");
        if (name == null || name.isEmpty()) name = inferAccessorName(method.name);
        if (name == null) {
            note(mixin, "@Accessor " + method.name + " has no inferable target name; not checked");
            return;
        }
        for (final FieldNode f : target.fields) if (f.name.equals(name)) return;
        if (inheritedField(target, name) != null) return;
        problem(mixin, "@Accessor target field " + name + " does not exist on " + shortName(target));
    }

    private void checkInvoker(final ClassNode mixin, final ClassNode target,
                              final MethodNode method, final AnnotationNode ann) {
        this.checkCount++;
        String name = (String) annValue(ann, "value");
        if (name == null || name.isEmpty()) name = inferInvokerName(method.name);
        if (name == null) {
            note(mixin, "@Invoker " + method.name + " has no inferable target name; not checked");
            return;
        }
        final boolean ctor = name.equals("<init>");
        for (final MethodNode m : target.methods) {
            if (m.name.equals(name) || (ctor && m.name.equals("<init>"))) return;
        }
        if (hasMethod(target, name, null, true)) return;
        problem(mixin, "@Invoker target method " + name + " does not exist on " + shortName(target));
    }

    // ---- injector checks --------------------------------------------------------

    private void checkInjector(final ClassNode mixin, final ClassNode target, final MethodNode handler,
                               final AnnotationNode ann, final String kind) {
        this.checkCount++;
        final Object methodValue = annValue(ann, "method");
        final List<String> selectors = new ArrayList<>();
        if (methodValue instanceof List) {
            for (final Object o : (List<?>) methodValue) selectors.add(String.valueOf(o));
        }
        if (selectors.isEmpty()) {
            note(mixin, handler.name + " has no `method` selector; not checked");
            return;
        }

        for (final String selector : selectors) {
            final String rawName = selector.contains("(")
                    ? selector.substring(0, selector.indexOf('('))
                    : selector;
            final String wantDesc = selector.contains("(") ? selector.substring(selector.indexOf('(')) : null;
            // A fully-qualified selector (Lowner;name(desc)) points at another class.
            if (rawName.startsWith("L") && rawName.contains(";")) {
                note(mixin, handler.name + " uses a fully qualified selector; not checked");
                continue;
            }
            final String name = rawName.contains(":") ? rawName.substring(rawName.lastIndexOf(':') + 1) : rawName;

            // Mixin accepts a glob selector: "*" for every method, "prefix*" for a
            // family of them. Upstream uses both to hit overrides across a block
            // hierarchy, so they have to match the same way here.
            final List<MethodNode> matches = new ArrayList<>();
            for (final MethodNode m : target.methods) {
                if (!selectorMatches(name, m.name)) continue;
                if (wantDesc != null && !m.desc.equals(wantDesc)) continue;
                // A generic override leaves an erased bridge twin. Mixin selects the
                // real method, so matching the bridge would report a false mismatch.
                // Synthetic alone is not a reason to skip: lambda bodies are
                // synthetic and are legitimate targets, which is how this port
                // reaches the creative search index on 1.20.1.
                if ((m.access & org.objectweb.asm.Opcodes.ACC_BRIDGE) != 0) continue;
                matches.add(m);
            }
            if (matches.isEmpty()) {
                if (name.endsWith("*")) {
                    // A glob is allowed to select nothing on one of several targets.
                    note(mixin, handler.name + ": glob `" + selector + "` matches no method on "
                            + shortName(target));
                    continue;
                }
                final String had = describeOverloads(target, name);
                problem(mixin, handler.name + ": target method `" + selector + "` does not exist on "
                        + shortName(target) + (had.isEmpty() ? "" : " (has " + had + ")"));
                continue;
            }
            final boolean glob = name.endsWith("*");
            if (glob) {
                // Mixin requires the injection point in at least one of the family,
                // not in every member of it.
                final int before = this.problems.size();
                final List<String> perMethod = new ArrayList<>();
                boolean any = false;
                for (final MethodNode targetMethod : matches) {
                    final int mark = this.problems.size();
                    checkAgainstTarget(mixin, target, targetMethod, handler, ann, kind);
                    if (this.problems.size() == mark) any = true;
                    else perMethod.addAll(this.problems.subList(mark, this.problems.size()));
                }
                while (this.problems.size() > before) this.problems.remove(this.problems.size() - 1);
                if (!any) {
                    problem(mixin, handler.name + ": glob `" + selector + "` matched "
                            + matches.size() + " method(s) on " + shortName(target)
                            + " but the injection point was found in none of them");
                }
            } else {
                for (final MethodNode targetMethod : matches) {
                    checkAgainstTarget(mixin, target, targetMethod, handler, ann, kind);
                }
            }
        }
    }

    private void checkAgainstTarget(final ClassNode mixin, final ClassNode target, final MethodNode targetMethod,
                                    final MethodNode handler, final AnnotationNode ann, final String kind) {
        final List<Integer> offsets = injectionOffsets(mixin, target, targetMethod, handler, ann);

        if (INJECT.equals(kind)) {
            checkInjectSignature(mixin, targetMethod, handler);
        }
        if (WRAP_OPERATION.equals(kind)) {
            checkWrappedShape(mixin, targetMethod, handler, ann, true);
        }
        if (REDIRECT.equals(kind)) {
            // A @Redirect handler has the same shape as a @WrapOperation one
            // without the trailing Operation.
            checkWrappedShape(mixin, targetMethod, handler, ann, false);
        }
        checkLocals(mixin, target, targetMethod, handler, offsets);
    }

    /**
     * An {@code @Inject} handler must repeat the target's own parameters before its
     * CallbackInfo. This is the check that catches a 1.21.1 signature that gained or
     * lost a parameter in 1.20.1 -- the MouseHandler#turnPlayer case.
     */
    private void checkInjectSignature(final ClassNode mixin, final MethodNode targetMethod, final MethodNode handler) {
        final Type[] targetArgs = Type.getArgumentTypes(targetMethod.desc);
        final Type[] handlerArgs = Type.getArgumentTypes(handler.desc);

        int ciAt = -1;
        for (int i = 0; i < handlerArgs.length; i++) {
            final String d = handlerArgs[i].getDescriptor();
            if (d.equals(CANCELLABLE_CI) || d.equals(CI_RETURNABLE)) { ciAt = i; break; }
        }
        if (ciAt < 0) {
            problem(mixin, handler.name + ": @Inject handler has no CallbackInfo parameter");
            return;
        }
        // Mixin allows a handler to take no target parameters at all.
        if (ciAt == 0) return;
        if (ciAt != targetArgs.length) {
            problem(mixin, handler.name + ": @Inject handler takes " + ciAt + " parameter(s) before CallbackInfo but "
                    + targetMethod.name + pretty(targetMethod.desc) + " takes " + targetArgs.length
                    + " -- expected (" + join(targetArgs) + ")");
            return;
        }
        for (int i = 0; i < ciAt; i++) {
            if (!handlerArgs[i].equals(targetArgs[i]) && !isCoerced(handler, i)) {
                problem(mixin, handler.name + ": @Inject parameter " + i + " is " + handlerArgs[i].getClassName()
                        + " but the target's is " + targetArgs[i].getClassName());
                return;
            }
        }
    }

    /**
     * Resolves the {@code @At} to the bytecode offsets it selects. An empty result
     * with a reported problem means the instruction the mixin is anchored to is not
     * in the 1.20.1 method at all.
     */
    private List<Integer> injectionOffsets(final ClassNode mixin, final ClassNode target,
                                           final MethodNode targetMethod, final MethodNode handler,
                                           final AnnotationNode ann) {
        final List<Integer> offsets = new ArrayList<>();
        final Object atValue = annValue(ann, "at");
        final List<AnnotationNode> ats = new ArrayList<>();
        if (atValue instanceof AnnotationNode) ats.add((AnnotationNode) atValue);
        else if (atValue instanceof List) {
            for (final Object o : (List<?>) atValue) if (o instanceof AnnotationNode) ats.add((AnnotationNode) o);
        }
        if (ats.isEmpty()) return offsets;

        for (final AnnotationNode at : ats) {
            final String point = String.valueOf(annValue(at, "value"));
            final String memberTarget = (String) annValue(at, "target");
            final Integer ordinalBoxed = (Integer) annValue(at, "ordinal");
            final int ordinal = ordinalBoxed == null ? -1 : ordinalBoxed;

            if ("HEAD".equals(point)) { offsets.add(0); continue; }
            if ("RETURN".equals(point)) { offsets.addAll(returnOffsets(targetMethod)); continue; }
            if ("TAIL".equals(point)) {
                final List<Integer> returns = returnOffsets(targetMethod);
                if (!returns.isEmpty()) offsets.add(returns.get(returns.size() - 1));
                continue;
            }
            if (memberTarget == null) continue;
            if (!"INVOKE".equals(point) && !"FIELD".equals(point)
                    && !"INVOKE_ASSIGN".equals(point) && !"NEW".equals(point)) {
                continue;
            }

            final Integer wantOpcode = (Integer) annValue(at, "opcode");
            final List<Integer> found = new ArrayList<>();
            final InsnList insns = targetMethod.instructions;
            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                final String ref = memberReference(insn);
                if (ref == null || !ref.equals(memberTarget)) continue;
                if (wantOpcode != null && insn.getOpcode() != wantOpcode) continue;
                found.add(insns.indexOf(insn));
            }
            if (found.isEmpty()) {
                problem(mixin, handler.name + ": @At(" + point + ") target `" + memberTarget
                        + "` does not occur in " + shortName(target) + "." + targetMethod.name
                        + pretty(targetMethod.desc));
                continue;
            }
            if (ordinal >= 0) {
                if (ordinal >= found.size()) {
                    problem(mixin, handler.name + ": @At ordinal " + ordinal + " for `" + memberTarget
                            + "` but it occurs only " + found.size() + " time(s) in "
                            + shortName(target) + "." + targetMethod.name);
                    continue;
                }
                offsets.add(found.get(ordinal));
            } else {
                offsets.addAll(found);
            }
        }
        return offsets;
    }

    /**
     * Checks every {@code @Local} on the handler. MixinExtras numbers implicit
     * ordinals over the locals of that type which are live at the injection point,
     * in slot order -- which is exactly what shifts when a method's shape changes
     * between versions.
     */
    private void checkLocals(final ClassNode mixin, final ClassNode target, final MethodNode targetMethod,
                             final MethodNode handler, final List<Integer> offsets) {
        if (handler.invisibleParameterAnnotations == null && handler.visibleParameterAnnotations == null) return;
        final Type[] handlerArgs = Type.getArgumentTypes(handler.desc);

        for (int i = 0; i < handlerArgs.length; i++) {
            final AnnotationNode local = paramAnn(handler, i, LOCAL);
            if (local == null) continue;
            this.checkCount++;

            final Boolean argsOnlyBoxed = (Boolean) annValue(local, "argsOnly");
            final boolean argsOnly = argsOnlyBoxed != null && argsOnlyBoxed;
            final Integer ordinalBoxed = (Integer) annValue(local, "ordinal");
            final Integer indexBoxed = (Integer) annValue(local, "index");
            final Object named = annValue(local, "name");
            final boolean byName = named instanceof List && !((List<?>) named).isEmpty();

            if (byName) {
                checkNamedLocal(mixin, target, targetMethod, handler,
                        String.valueOf(((List<?>) named).get(0)), handlerArgs[i], offsets);
                continue;
            }
            if (indexBoxed != null) continue;                  // addressed by slot, not ordinal
            if (ordinalBoxed == null) continue;                // implicit: MixinExtras requires uniqueness, not an index
            final int ordinal = ordinalBoxed;
            final Type want = handlerArgs[i];

            if (argsOnly) {
                final Type[] targetArgs = Type.getArgumentTypes(targetMethod.desc);
                int seen = 0;
                for (final Type t : targetArgs) {
                    if (t.equals(want)) {
                        if (seen == ordinal) { seen = -1; break; }
                        seen++;
                    }
                }
                if (seen != -1) {
                    problem(mixin, handler.name + ": @Local(argsOnly, ordinal = " + ordinal + ") of type "
                            + want.getClassName() + " -- " + targetMethod.name + pretty(targetMethod.desc)
                            + " has only " + seen + " argument(s) of that type");
                }
                continue;
            }

            if (targetMethod.localVariables == null || targetMethod.localVariables.isEmpty()) {
                note(mixin, handler.name + ": " + shortName(target) + "." + targetMethod.name
                        + " carries no local variable table; @Local ordinals not checked");
                continue;
            }
            if (offsets.isEmpty()) continue; // the @At problem is already reported

            for (final int offset : offsets) {
                final List<LocalVariableNode> live = liveLocals(targetMethod, offset, want);
                if (ordinal >= live.size()) {
                    final StringBuilder sb = new StringBuilder();
                    for (final LocalVariableNode lv : live) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(lv.name).append("@").append(lv.index);
                    }
                    problem(mixin, handler.name + ": @Local(ordinal = " + ordinal + ") of type "
                            + want.getClassName() + " at " + shortName(target) + "." + targetMethod.name
                            + " -- only " + live.size() + " such local(s) live there ["
                            + sb + "]");
                }
            }
        }
    }

    /**
     * A {@code @Local(name = ...)} matches on the target's local variable table.
     *
     * <p>That table is a debug attribute, and whether it carries usable names
     * depends entirely on who compiled the target. Mod jars such as Create ship
     * real names, so naming a local there is fine. Vanilla is the trap: Loom hands
     * development a jar with mapped local names, so the mixin resolves and the game
     * starts, while at runtime the obfuscated jar keeps the table and loses the
     * names, and the same mixin dies with "Unable to find matching local". A defect
     * of this shape cannot be found by launching the development client -- that is
     * the one environment where it works.
     */
    private void checkNamedLocal(final ClassNode mixin, final ClassNode target, final MethodNode targetMethod,
                                 final MethodNode handler, final String name, final Type want,
                                 final List<Integer> offsets) {
        if (target.name.startsWith("net/minecraft/")) {
            final String suggestion = suggestOrdinal(targetMethod, name, want, offsets);
            problem(mixin, handler.name + ": @Local(name = \"" + name + "\") targets vanilla "
                    + shortName(target) + "." + targetMethod.name
                    + " -- local names are obfuscated at runtime, so this resolves in development"
                    + " and fails in a real installation. Address it positionally instead"
                    + (suggestion == null ? "." : ": " + suggestion));
            return;
        }

        if (targetMethod.localVariables == null) {
            note(mixin, handler.name + ": " + shortName(target) + "." + targetMethod.name
                    + " carries no local variable table; @Local(name) not checked");
            return;
        }
        boolean found = false;
        for (final LocalVariableNode lv : targetMethod.localVariables) {
            if (lv.name.equals(name) && lv.desc.equals(want.getDescriptor())) { found = true; break; }
        }
        if (!found) {
            problem(mixin, handler.name + ": @Local(name = \"" + name + "\") of type "
                    + want.getClassName() + " does not name a local of that type in "
                    + shortName(target) + "." + targetMethod.name);
            return;
        }
        for (final int offset : offsets) {
            boolean live = false;
            for (final LocalVariableNode lv : liveLocals(targetMethod, offset, want)) {
                if (lv.name.equals(name)) { live = true; break; }
            }
            if (!live) {
                problem(mixin, handler.name + ": @Local(name = \"" + name + "\") is not in scope at the"
                        + " injection point in " + shortName(target) + "." + targetMethod.name);
                return;
            }
        }
    }

    /** The ordinal a named local would have, so the report says what to write instead. */
    private String suggestOrdinal(final MethodNode targetMethod, final String name, final Type want,
                                  final List<Integer> offsets) {
        if (targetMethod.localVariables == null || offsets.isEmpty()) return null;
        final List<LocalVariableNode> live = liveLocals(targetMethod, offsets.get(0), want);
        for (int i = 0; i < live.size(); i++) {
            if (live.get(i).name.equals(name)) {
                return live.size() == 1
                        ? "it is the only " + want.getClassName() + " in scope there, so @Local(ordinal = 0)"
                        : "@Local(ordinal = " + i + ")";
            }
        }
        return null;
    }

    /** Locals of {@code want} live at {@code offset}, ordered by slot, as MixinExtras orders them. */
    private List<LocalVariableNode> liveLocals(final MethodNode method, final int offset, final Type want) {
        final InsnList insns = method.instructions;
        final List<LocalVariableNode> live = new ArrayList<>();
        for (final LocalVariableNode lv : method.localVariables) {
            if (!lv.desc.equals(want.getDescriptor())) continue;
            final int start = insns.indexOf(lv.start);
            final int end = insns.indexOf(lv.end);
            if (offset >= start && offset < end) live.add(lv);
        }
        live.sort(Comparator.comparingInt(a -> a.index));
        return live;
    }

    // ---- helpers ----------------------------------------------------------------

    private static String memberReference(final AbstractInsnNode insn) {
        if (insn instanceof MethodInsnNode) {
            final MethodInsnNode m = (MethodInsnNode) insn;
            return "L" + m.owner + ";" + m.name + m.desc;
        }
        if (insn instanceof FieldInsnNode) {
            final FieldInsnNode f = (FieldInsnNode) insn;
            return "L" + f.owner + ";" + f.name + ":" + f.desc;
        }
        if (insn instanceof TypeInsnNode && insn.getOpcode() == org.objectweb.asm.Opcodes.NEW) {
            return "L" + ((TypeInsnNode) insn).desc + ";";
        }
        return null;
    }

    /**
     * Instruction indices of every return in the method. RETURN and TAIL inject
     * before a return opcode, not at the end of the instruction list -- the list
     * ends with the labels that close the local variable ranges, so measuring from
     * there makes every local look already out of scope.
     */
    private static List<Integer> returnOffsets(final MethodNode method) {
        final List<Integer> out = new ArrayList<>();
        final InsnList insns = method.instructions;
        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            final int op = insn.getOpcode();
            if (op >= org.objectweb.asm.Opcodes.IRETURN && op <= org.objectweb.asm.Opcodes.RETURN) {
                out.add(insns.indexOf(insn));
            }
        }
        return out;
    }

    private boolean hasMethod(final ClassNode target, final String name, final String desc, final boolean up) {
        for (final MethodNode m : target.methods) {
            if (m.name.equals(name) && (desc == null || m.desc.equals(desc))) return true;
        }
        if (!up) return false;
        for (final String parent : parents(target)) {
            final ClassNode p = resolve(parent);
            if (p != null && hasMethod(p, name, desc, true)) return true;
        }
        return false;
    }

    private FieldNode inheritedField(final ClassNode target, final String name) {
        for (final String parent : parents(target)) {
            final ClassNode p = resolve(parent);
            if (p == null) continue;
            for (final FieldNode f : p.fields) if (f.name.equals(name)) return f;
            final FieldNode deeper = inheritedField(p, name);
            if (deeper != null) return deeper;
        }
        return null;
    }

    private static List<String> parents(final ClassNode node) {
        final List<String> out = new ArrayList<>();
        if (node.superName != null) out.add(node.superName);
        if (node.interfaces != null) out.addAll(node.interfaces);
        return out;
    }

    private static String describeOverloads(final ClassNode target, final String name) {
        final List<String> out = new ArrayList<>();
        for (final MethodNode m : target.methods) if (m.name.equals(name)) out.add(name + pretty(m.desc));
        return String.join(", ", out);
    }

    private static boolean isCoerced(final MethodNode handler, final int index) {
        return paramAnn(handler, index, COERCE) != null;
    }

    private static AnnotationNode paramAnn(final MethodNode method, final int index, final String desc) {
        final AnnotationNode a = paramAnnIn(method.invisibleParameterAnnotations, method, index, desc);
        if (a != null) return a;
        return paramAnnIn(method.visibleParameterAnnotations, method, index, desc);
    }

    private static AnnotationNode paramAnnIn(final List<AnnotationNode>[] lists, final MethodNode method,
                                             final int index, final String desc) {
        if (lists == null) return null;
        // Parameter annotation arrays can be offset when the compiler emits a count
        // that excludes synthetic parameters; align from the end.
        final int argc = Type.getArgumentTypes(method.desc).length;
        final int shift = argc - lists.length;
        final int i = index - shift;
        if (i < 0 || i >= lists.length || lists[i] == null) return null;
        for (final AnnotationNode a : lists[i]) if (a.desc.equals(desc)) return a;
        return null;
    }

    private static AnnotationNode findAnn(final List<AnnotationNode> list, final String desc) {
        if (list == null) return null;
        for (final AnnotationNode a : list) if (a.desc.equals(desc)) return a;
        return null;
    }

    private static AnnotationNode findAny(final List<AnnotationNode> a, final List<AnnotationNode> b, final String desc) {
        final AnnotationNode found = findAnn(a, desc);
        return found != null ? found : findAnn(b, desc);
    }

    private static Object annValue(final AnnotationNode ann, final String key) {
        if (ann == null || ann.values == null) return null;
        for (int i = 0; i + 1 < ann.values.size(); i += 2) {
            if (key.equals(ann.values.get(i))) {
                final Object v = ann.values.get(i + 1);
                if (v instanceof String[]) return ((String[]) v)[1]; // enum constant
                return v;
            }
        }
        return null;
    }

    private static String inferAccessorName(final String method) {
        for (final String prefix : new String[]{"get", "set", "is"}) {
            if (method.startsWith(prefix) && method.length() > prefix.length()) {
                final String rest = method.substring(prefix.length());
                return Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
            }
        }
        return null;
    }

    private static String inferInvokerName(final String method) {
        for (final String prefix : new String[]{"invoke", "call"}) {
            if (method.startsWith(prefix) && method.length() > prefix.length()) {
                final String rest = method.substring(prefix.length());
                return Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
            }
        }
        if (method.startsWith("new") || method.startsWith("create")) return "<init>";
        return null;
    }

    /**
     * A {@code @WrapOperation} handler must mirror the wrapped instruction: for a
     * non-static member, the owning instance first, then that member's own
     * arguments, then the Operation. MixinExtras compares the receiver against the
     * owner exactly, so a handler that widens it to Object -- which is tempting
     * when the owner is a private class -- is rejected at apply time.
     */
    private void checkWrappedShape(final ClassNode mixin, final MethodNode targetMethod,
                                   final MethodNode handler, final AnnotationNode ann,
                                   final boolean expectOperation) {
        final Object atValue = annValue(ann, "at");
        final List<AnnotationNode> ats = new ArrayList<>();
        if (atValue instanceof AnnotationNode) ats.add((AnnotationNode) atValue);
        else if (atValue instanceof List) {
            for (final Object o : (List<?>) atValue) if (o instanceof AnnotationNode) ats.add((AnnotationNode) o);
        }
        if (ats.size() != 1) return;
        final AnnotationNode at = ats.get(0);
        final String point = String.valueOf(annValue(at, "value"));
        final String memberTarget = (String) annValue(at, "target");
        if (memberTarget == null) return;

        // Find the wrapped instruction so its opcode says whether there is a receiver.
        // A field that is both read and written in the method appears twice, and the
        // annotation's `opcode` is what picks the one being wrapped.
        final Integer wantOpcode = (Integer) annValue(at, "opcode");
        AbstractInsnNode wrapped = null;
        for (AbstractInsnNode insn = targetMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            final String ref = memberReference(insn);
            if (ref == null || !ref.equals(memberTarget)) continue;
            if (wantOpcode != null && insn.getOpcode() != wantOpcode) continue;
            wrapped = insn;
            break;
        }
        if (wrapped == null) return; // already reported by the @At check

        final List<Type> expected = new ArrayList<>();
        if (wrapped instanceof MethodInsnNode) {
            final MethodInsnNode m = (MethodInsnNode) wrapped;
            if (m.getOpcode() != org.objectweb.asm.Opcodes.INVOKESTATIC) {
                expected.add(Type.getObjectType(m.owner));
            }
            expected.addAll(Arrays.asList(Type.getArgumentTypes(m.desc)));
        } else if (wrapped instanceof FieldInsnNode) {
            final FieldInsnNode f = (FieldInsnNode) wrapped;
            final int op = f.getOpcode();
            if (op == org.objectweb.asm.Opcodes.GETFIELD || op == org.objectweb.asm.Opcodes.PUTFIELD) {
                expected.add(Type.getObjectType(f.owner));
            }
            if (op == org.objectweb.asm.Opcodes.PUTFIELD || op == org.objectweb.asm.Opcodes.PUTSTATIC) {
                expected.add(Type.getType(f.desc));
            }
        } else {
            return;
        }

        final Type[] handlerArgs = Type.getArgumentTypes(handler.desc);
        // Trailing Operation, plus any @Local/@Share sugar after it, is not part of
        // the wrapped instruction's own shape.
        final String label = expectOperation ? "@WrapOperation" : "@Redirect";
        int cut;
        if (expectOperation) {
            cut = -1;
            for (int i = 0; i < handlerArgs.length; i++) {
                if (handlerArgs[i].getDescriptor().equals(OPERATION)) { cut = i; break; }
            }
            if (cut < 0) {
                problem(mixin, handler.name + ": @WrapOperation handler has no Operation parameter");
                return;
            }
        } else {
            // A @Redirect may append the enclosing method's own parameters after the
            // redirected member's; only the leading ones are constrained.
            cut = Math.min(handlerArgs.length, expected.size());
            if (handlerArgs.length < expected.size()) {
                problem(mixin, handler.name + ": @Redirect handler takes " + handlerArgs.length
                        + " parameter(s) but redirecting `" + memberTarget + "` needs at least "
                        + expected.size() + " (" + join(expected.toArray(new Type[0])) + ")");
                return;
            }
        }
        if (expectOperation && cut != expected.size()) {
            problem(mixin, handler.name + ": @WrapOperation handler takes " + cut
                    + " parameter(s) before Operation but wrapping `" + memberTarget + "` needs "
                    + expected.size() + " (" + join(expected.toArray(new Type[0])) + ")");
            return;
        }
        for (int i = 0; i < cut; i++) {
            if (!handlerArgs[i].equals(expected.get(i)) && !isCoerced(handler, i)) {
                problem(mixin, handler.name + ": " + label + " parameter " + i + " is "
                        + handlerArgs[i].getClassName() + " but `" + memberTarget
                        + "` requires " + expected.get(i).getClassName()
                        + (expectOperation ? " -- MixinExtras compares this type exactly" : ""));
                return;
            }
        }
    }

    /** Mixin's name selector: an exact name, "*", or a "prefix*" glob. */
    private static boolean selectorMatches(final String selector, final String methodName) {
        if (selector.equals("*")) return true;
        if (selector.endsWith("*")) return methodName.startsWith(selector.substring(0, selector.length() - 1));
        return selector.equals(methodName);
    }

    private static String shortName(final ClassNode node) {
        final String n = node.name;
        return n.substring(n.lastIndexOf('/') + 1);
    }

    private static String pretty(final String desc) {
        if (desc.startsWith("(")) {
            return "(" + join(Type.getArgumentTypes(desc)) + ")";
        }
        return Type.getType(desc).getClassName();
    }

    private static String join(final Type[] types) {
        final List<String> parts = new ArrayList<>();
        for (final Type t : types) {
            final String n = t.getClassName();
            parts.add(n.contains(".") ? n.substring(n.lastIndexOf('.') + 1) : n);
        }
        return String.join(", ", parts);
    }

    private void problem(final ClassNode mixin, final String message) {
        this.problems.add(shortName(mixin) + ": " + message);
    }

    private void note(final ClassNode mixin, final String message) {
        this.notes.add(shortName(mixin) + ": " + message);
    }

    private void report() {
        System.out.println("audited " + this.mixinCount + " mixin(s), " + this.checkCount + " check(s)");
        if (!this.notes.isEmpty()) {
            System.out.println();
            System.out.println(this.notes.size() + " not checked:");
            for (final String n : this.notes) System.out.println("  - " + n);
        }
        System.out.println();
        if (this.problems.isEmpty()) {
            System.out.println("no mismatches found");
        } else {
            System.out.println(this.problems.size() + " mismatch(es):");
            for (final String p : this.problems) System.out.println("  ! " + p);
        }
        System.exit(this.problems.isEmpty() ? 0 : 1);
    }
}
