# Fabric 1.20.1 (Homestead) Port — Completion Plan

Working branch: `fabric-homestead-1.20.1`
Upstream reference: `main` (NeoForge + Fabric, MC 1.21.1, Java 21)

---

## 0. How to read this document

This is a **feature and outcome plan**, not an architecture spec.

- Every checkbox describes **a thing a player or server admin can observe**, or a
  measurable property of the build. It does not say how to implement it.
- Nothing here mandates a class layout, a registration style, a networking
  approach, a mixin strategy, a module split, or a mapping of upstream files to
  new files. Those are entirely the implementing agent's call.
- Section 2 lists **what the 1.20.1 Fabric runtime happens to make available**.
  That is reference material so nobody re-discovers it the hard way. Use it,
  ignore it, or replace it — it is not a requirement.
- Ordering *within* a version is a suggestion. Ordering *between* versions is
  meaningful, because later versions depend on earlier ones being observable.
- If a checklist item turns out to be wrong, impossible, or a bad idea on this
  stack, **change it**. Record why in the version's notes. Do not silently drop
  it.

The goal is: **every feature in the 1.21.1 Simulated Project works on Fabric
1.20.1 in the Homestead pack, correctly and at acceptable performance**, reached
in at most four more major versions.

---

## 1. Where the port stands today

Measured against `main` at the merge base (`b9cb3a8`, upstream 1.3.2).

| Module | Upstream | Ported to Fabric 1.20.1 |
|---|---|---|
| `simulated` | 584 Java files / ~63,400 lines | 745 files / ~72,200 lines |
| `aeronautics` | 202 files / ~18,700 lines | 0 |
| `offroad` | 69 files / ~5,600 lines | 0 |

The port is larger than upstream because it carries what the 1.20.1 stack does
not supply: `backport/` is 128 files of shims — an inert Sable facade, a Veil
replacement, and the 1.20.5+ vanilla APIs upstream assumes.

**File-for-file coverage of upstream `simulated/common` is complete.** Six of
584 upstream files have no counterpart here, and all six are mixins into Sable
classes that do not exist on this stack:

```
mixin/assembly_preventer/ServerSubLevelMixin
mixin/diagram/VanillaSubLevelRenderDispatcherMixin
mixin/end_sea/VanillaSubLevelRenderDispatcherMixin
mixin/rope/ClientSubLevelContainerMixin
mixin/sable_hooks/SableCommonEventsMixin
mixin/tooltip_flag/SessionSearchTreesMixin
```

The last is not a gap: `SessionSearchTrees` does not exist before 1.20.2, and its
job is done by `mixin/search_alias/SessionSearchTreesMixin`, which targets the
`Minecraft` lambda that builds the creative search index on 1.20.1.

**Registered content on the branch today:** 96 blocks, 86 items, 30 advancements,
7 statistics, 39 packet shapes, 29 sound definitions, 22 tag files, 73 recipes,
94 loot tables and 652 lang keys.

**What works in game today:**

- Physics Assembler places, wrenches, drops, and has upstream voxel shapes.
- Empty-hand use runs a Create-parity structure scan (super glue, chassis,
  pistons, gantry, bogeys, double chests, cart assemblers) and assembles the
  result into a real moving Create `ControlledContraptionEntity`.
- The assembler itself travels inside the moving structure; an invisible anchor
  block holds the controller position Create requires.
- Sneak-use disassembles; the assembler is explicitly restored with its saved
  orientation, with rollback if Create's disassembly throws.
- Live assemblies are recovered back into world blocks on server shutdown.
- Clicking the moving assembler or a moving Steering Wheel engages a
  server-authoritative helm: WASD thrust/yaw, space/ctrl climb/descend, swept
  collision, seat riding.
- `/simulated status`, `compatibility`, `assembler_state`, `give_core_items`,
  `give_physics_assembler`, `give_steering_wheel`.
- CI (`Fabric 1.20.1 build`) is green at branch tip.

**What is explicitly a stand-in and is expected to be replaced:**

- Create's contraption entity is standing in for Sable sub-levels. There is no
  rigid-body physics, no mass, no torque, no constraints, no sub-level.
- The invisible anchor block exists only because Create needs a stable
  world-position controller. Sable does not need it.
- The helm is a hand-rolled vehicle controller, not the upstream Steering Wheel
  / Throttle Lever / Handle control model.
- Assemblies are force-disassembled at server shutdown rather than persisted.

**The Sable backport is currently failing.** `Sable 1.20.1 backport` CI has never
passed the `Compile Sable 2.0.4 core` job. See section 3.

---

## 2. What the 1.20.1 Fabric runtime already gives you

*Reference only. None of this is required.*

The pinned Create build — `create-fabric:6.0.8.1+build.1744-mc1.20.1` — nests and
exposes the **modern Create 6 library stack on 1.20.1**:

| Library | Version bundled for 1.20.1 | Upstream 1.21.1 uses |
|---|---|---|
| Ponder | `1.0.91` | `1.0.81` |
| Registrate | `1.3.79-MC1.20.1` | `MC1.21-1.3.0+67` |
| Flywheel | `1.0.5-264` (+ API `1.0.0-215`) | `1.0.6` |
| Porting Lib | `2.3.13+1.20.1` (accessors, base, entity, extensions, networking, models, model_generators, client_events, tags, transfer, tool_actions, brewing) | n/a |
| Forge Config API Port | `8.0.0` | n/a |
| Catnip / Create API | Create 6 generation | Create 6 generation |

Practical consequences worth knowing before planning any subsystem:

- Create 6-era APIs used by upstream (`CreateBuiltInRegistries`,
  `ContraptionType`, `MovingInteractionBehaviour.REGISTRY`,
  `BlockMovementChecks`, `net.createmod.catnip.*`, `net.createmod.ponder.*`)
  **exist on this stack**. Mixin targets into Create internals should largely
  line up with upstream.
- Porting Lib supplies Fabric equivalents for the Forge-shaped APIs upstream
  leans on (capabilities/transfer, tags, model generators, client events), which
  covers most of `multiloader/` and the 14 files that import `net.neoforged`.
- Forge Config API Port means the upstream config spec shape is usable directly
  if desired.
- **Only 14 of 584 upstream `simulated/common` files import NeoForge at all.**
  The codebase is already close to loader-neutral, and upstream already carries a
  `service/` abstraction with 13 service interfaces plus NeoForge implementations.

The real porting cost is **not** the loader. It is the **MC 1.21.1 → 1.20.1
vanilla delta**:

| 1.21.1 API used upstream | Files affected | Not present on 1.20.1 |
|---|---|---|
| `StreamCodec` / `net.minecraft.network.codec` | ~50 | yes (1.20.5+) |
| `RegistryFriendlyByteBuf` | 32 | yes |
| Data components (`net.minecraft.core.component`, `DataComponents`) | ~24 | yes (1.20.5+) — item data is NBT on 1.20.1 |
| `HolderLookup` in datagen/registry paths | 40 | partially |
| `.getFirst()` on `List` (Java 21 SequencedCollection) | 23 | yes — Java 17 target |
| `ResourceLocation.withDefaultNamespace` / `fromNamespaceAndPath` | 4 | yes — constructor on 1.20.1 |
| `useWithoutItem` | 2 | yes — `use` on 1.20.1 |
| Data folder names (`recipe/`, `loot_table/`, `advancement/`) | all datagen output | yes — plural on 1.20.1 |

An MIT-licensed Fabric reference for loader-side hooks exists at
`bobqianic/create-aeronautics-fabric`. Attribution required if used.

---

## 3. The one genuine blocker: Sable

Sable is the physics engine. **121 of 584 upstream `simulated` files import it**
(`Sable`, `SubLevel`, `ServerSubLevel`, `ClientSubLevel`, `SubLevelHelper`,
`SubLevelContainer`, `SubLevelPhysicsSystem`, `RigidBodyHandle`, force groups,
constraints, `OrientedBoundingBox3d`, JOML conversion, `LevelAccelerator`, …).
No amount of clever scoping avoids it — the physics half of Simulated is
unreachable without it.

Current state of the backport attempt:

- `Backport Sable Companion` **passes**. Companion (math/native side) builds for
  Fabric 1.20.1 / Java 17.
- `Compile Sable 2.0.4 core` **fails**, on two independent classes of error:
  1. **Veil.** Sable 2.0.4 targets Veil `4.3.2`, and the gate probe puts no Veil
     on the classpath at all, so every `foundry.veil.*` reference fails there.
     Veil's 1.20.1 line is versioned `1.0.0.x`, unrelated to the 4.x line, and
     the newest is `1.0.0.296` (27 Dec 2024). Measured against it, 15 of the 28
     Veil types Sable imports are present and 13 are missing — see §2.1.
  2. **1.20.5+ vanilla APIs.** `net.minecraft.network.codec`,
     `net.minecraft.network.protocol.common.custom`, and friends do not exist on
     1.20.1. Sable's packet layer needs the same treatment as Simulated's.
- The gate probe also excludes `sable/mixin/**`, `sublevel/render/**`, `debug/**`,
  `compatibility/**`, `SableClient`, and `fabric/**` — 256 of 534 common files —
  and javac stops counting at a hundred, so its number is an understatement
  twice over.

The honest measurement §2.1 asked for has now been taken, and it **changes the
shape of V2**: the blocker is one shimmable abstraction rather than a pervasive
one. Numbers, decisions and method are in §2.1; the two corrections this section
needed are:

- The earlier note here said the newest `Veil-fabric-1.20.1` was `1.0.0.86` and
  contained no `foundry.veil.platform.registry` or `foundry.veil.api.client.editor`.
  Both parts were wrong. The newest is `1.0.0.296`, and it has both of those.
  What it genuinely lacks is any `foundry.veil.api.network` package at all.
- "The renderer and mixin surface are unmeasured" no longer holds. Both are
  measured in §2.1.

Everything in V1 below is deliberately chosen to be **independent of the Veil
answer**, so the port keeps moving while Sable is being brought up.

---

## 4. Release map

Four majors. Each one ships something a Homestead player can install and use.

| | Name | Theme | Player-visible outcome |
|---|---|---|---|
| **V1** | Simulated Core | Everything in Simulated that does not need physics | Almost all Simulated blocks, items, redstone, sensors, UI, ponder, JEI, advancements, sounds work. Assembly still uses the Create stand-in. |
| **V2** | Sable | Real physics on 1.20.1 | Physics Assembler makes real Sable sub-level contraptions that move, collide, render, and persist. |
| **V3** | Simulated Complete | Everything that needs physics, plus hardening | Ropes, springs, swivel bearings, docking, physics staff, diagram, handles, glue. Multiplayer- and save-safe, performance-tuned. |
| **V4** | Aeronautics + Offroad | The sibling mods and release | Full suite. Bundled jar, pack-ready, released. |

---

## V1 — Simulated Core

**Goal:** every part of Simulated that does not require a sub-level runs on
Fabric 1.20.1 in Homestead. The Physics Assembler keeps its temporary Create
transport; nothing else is a stand-in.

**Ship criteria**

- [ ] The mod loads clean in the full Homestead pack, client and dedicated
      server, with zero missing-model / missing-texture / missing-lang warnings.
- [ ] Every ID listed below is registered, obtainable, craftable, breakable, and
      renders correctly.
- [ ] Save a world, quit, relaunch: every block entity below retains its state.
- [ ] Two players on a dedicated server can use every block below without desync,
      ghost blocks, or client-only state.
- [ ] Nothing that shipped in Homestead `.13` regressed.

### 1.1 Foundations

**Notes log — Homestead `.14`, registration backbone.**

The port now registers content through **Registrate**, not hand-rolled
`Registry.register` calls, and the resource tree is **generated**, not
hand-written. This was the enabling decision for the rest of V1: upstream
describes each block's blockstate, models, loot table, tags, recipe and lang
name in the same builder chain that registers it, so porting a block becomes
copying that chain rather than authoring six JSON files per block per dye
colour. `SimulatedRegistrate`, `CreativeTabItemTransforms` and the creative-tab
item ordering came across from upstream; the physics assembler, its anchor, the
steering wheel and the four ingredient items were moved onto it.

Run `./gradlew :simulated:fabric:runDatagen` after changing registered content
and **commit `simulated/fabric/src/main/generated`** — CI builds from the
committed output and does not regenerate it.

Deviations forced by the 1.20.1 stack, all recorded rather than dropped:

- `SimulatedRegistrate.navTarget` / `.propertyTooltip` are not ported yet. They
  register into custom registries whose element types (`NavigationTarget`,
  `BlockPropertiesTooltip.Entry`) do not exist here yet; they land with the
  Navigation Table. Upstream builds those registries through Veil's
  `RegistrationProvider`, which is also not on this stack — 1.20.1 Fabric needs
  `FabricRegistryBuilder` instead.
- `SimpleResourceManager` was rewritten without Veil's `CodecReloadListener`;
  behaviour is the same.
- `SimulatedSection` carries colours as ARGB ints rather than Veil `Colorc`, and
  its title round-trips through `Component.Serializer` because component codecs
  arrived in 1.20.5. The JSON shape upstream writes is unchanged.
- `SimulatedCreativeTab` ports section grouping, ordering, visibility and row
  padding. The per-section **banner rendering is not ported** — it needs
  `GuiGraphics#blitSprite` and `Minecraft#getGuiSprites`, both 1.20.2+. Nothing
  in this mod depends on it; it matters when Aeronautics and Offroad add their
  own sections in V4, so it is a V4 item, not a silent drop.
- Datagen runs as a **client** run configuration, hand-written in
  `build.gradle`. Loom 1.8.13's `fabricApi.configureDataGeneration()` only
  builds a dedicated-server run, and blockstate generation touches client-only
  classes such as `BlockModelRotation`.
- The datagen `ExistingFileHelper` is built with validation disabled rather than
  via `withResourcesFromArg()`, which reaches for `Minecraft.getInstance()`.
  Nothing checks that a referenced parent model exists; the game's own
  missing-model warnings are what V1 ships on.

**Notes log — Homestead `.15`, four defects from in-game testing of `.14`.**

- **The Steering Wheel item had no icon.** Its item model parents upstream's
  `block/steering_wheel/item`, which is an OBJ model declaring NeoForge's
  `neoforge:obj` loader and referencing an `.obj` and `.mtl` the build never
  copied — `processResources` pulled only `*.json` from that directory. The
  whole directory is copied now, and JSON models are filtered on copy to rewrite
  `neoforge:obj`/`forge:obj` to `porting_lib:obj` and `flip_v` to Porting Lib's
  `flipV`. The rewrite is generic, so every OBJ model brought across from here on
  is covered without touching the upstream files.
- **The wheel rim was missing in world.** `block.json` is only the casing; the
  rim is the `wheel` OBJ partial, which upstream draws from a block entity
  renderer because it turns and because it overhangs its own block. A minimal
  `SteeringWheelBlockEntity` and `SteeringWheelRenderer` now draw it, using
  upstream's transform unchanged. Cut down from upstream: no kinetic shaft stub,
  no plank-material swapping, no Flywheel visual, and the rim does not yet turn.
  Because there is no visual, the renderer does **not** bail out when
  visualisation is supported, unlike upstream's — it is the only thing drawing
  the rim on any backend. The kinetic block entity is still V1 §1.4.
- **The Create wrench did nothing to the Steering Wheel.** Upstream's block
  implements `IRotate`, which extends `IWrenchable`; the port's shell block
  implemented neither. It implements `IWrenchable` now, on Create's defaults:
  wrenching the top or bottom face turns it, sneak-wrenching removes it, and a
  side face does nothing, as for Create's own horizontal blocks.
- **The helm drove the wrong way.** Two separate bugs in the stand-in vehicle
  controller, both now fixed:
  1. Forward was computed from the contraption's own rotation angle alone, so
     the craft always started driving world-south no matter which way the wheel
     pointed. The helm now takes the control block's facing as a
     contraption-local heading and puts it through Create's own
     `applyRotation`, so the heading tracks the craft as it yaws and matches
     exactly what Create renders and collides against. Clicking a Steering Wheel
     drives the way that wheel points; clicking the moving assembler drives the
     way the assembler faces.
  2. Yaw was applied with the wrong sign. `VecHelper.rotate` takes local +Z
     toward +X, so a rising contraption angle turns the craft anticlockwise —
     pressing D turned it left, and the craft travelled opposite to the way it
     visibly turned. The angle delta is negated now.

Confirmed in game on `.15`: icon, wheel rim, wrench and steering all correct.
One thing tested and deliberately left alone — **the pilot sits on top of the
wheel**. That is not upstream behaviour: upstream's Steering Wheel seats nobody,
it is a control block you stand at, and the riding comes from this port's
stand-in helm. The seat exists because a player who is not riding the Create
contraption slides off it as it moves; there is no contraption-relative standing
until Sable. It goes away in V3 along with the rest of the hand-rolled helm.

Two fixes fell out of moving to the generated resources:

- The **Steering Wheel had no loot table** and dropped nothing when broken. It
  has one now.
- The Steering Wheel item model now parents `block/steering_wheel/item`, the
  upstream custom item model, rather than the in-world block model.

**Notes log — Homestead `.17`, the V1 body of work, and a mixin verifier.**

The bulk of V1 is written: content, index, network, ponder, compat, config,
client and mixin packages are all present, and file-for-file coverage of upstream
is complete but for the six Sable mixins listed in section 1. Line counts per
package sit at or slightly above upstream everywhere. `backport/physics` is an
85-file, 2,675-line **inert Sable facade** — every handle reports a body at rest
at the origin and discards writes — which is what lets the 121 upstream files
that import Sable compile and register here without the engine.

**The mixin surface is now verified statically, by `tools/mixin_audit/run.py`.**

This mattered because the port's 80 mixins were copied from a 1.21.1 codebase,
where the same method can have a different signature, a different body, and so
different local ordinals. Mixin reports only the *first* such mismatch, and only
when the game applies it, so finding them by launching costs one boot per defect.
The tool reads the compiled mixins with ASM and reports every mismatch in one
pass, checking, against the real 1.20.1 classes: that the target class resolves;
that every `@Shadow`, `@Accessor` and `@Invoker` names a member that exists; that
each injector's `method` selector matches, including `*` and `prefix*` globs;
that each `@At(INVOKE/FIELD)` target occurs in that method's bytecode at the
requested ordinal, honouring `opcode`; that an `@Inject` handler repeats the
target's own parameters; that a `@WrapOperation` or `@Redirect` handler mirrors
the wrapped instruction; and that every `@Local(ordinal)` resolves to a local of
that type which is *live* at the injection point, computed from the target's
local variable table.

It found and fixed three mixins that were fatal on this stack. All three were
verified by reintroducing the defect and confirming the tool reports it:

- **`MouseHandlerMixin#turnPlayer` crashed the client during init.** 1.21.1's
  `turnPlayer(double)` takes the frame time; 1.20.1's takes nothing, so the
  handler had one parameter too many. The two turn deltas also move: at the call
  to `LocalPlayer#turn` the live doubles are, by slot, the timestamp, the frame
  delta, the two deltas and the three sensitivity terms, making the deltas
  ordinals 2 and 3 rather than 4 and 5. Note that the old ordinals **would still
  have resolved** — there are seven live doubles — and would have silently fed
  the sensitivity terms in as mouse movement. A mismatch the tool cannot catch,
  and a good argument for reading the bytecode rather than trusting a copy.
- **`MouseHandlerMixin#onScroll` named ordinals that do not exist.** 1.21.1
  scales both scroll axes; 1.20.1 scales only the vertical one into a single
  local and never reads the horizontal. The handler now takes that one local and
  reports the horizontal delta as zero, which is what
  `SimulatedCommonClientEvents` already does for the keyboard scroll path.
- **`SpriteContentsTickerMixin` crashed during the first texture stitch.**
  MixinExtras compares a `@WrapOperation` receiver against the owner class
  exactly. The handler took `Object`, because `SpriteContents$Ticker` is private
  on this stack; naming the mixin's own type does not work either. The access
  widener opens the class and the handler names it.

Also closed in `.17`:

- **`Registry 'simulated:property_tooltip' was empty after loading`,** an ERROR
  on every dedicated-server boot. The registry's entries are tooltip functions
  filled in from client setup, so a server fills it with nothing. It is now built
  behind a holder class that only client code touches, so the server never
  creates it. Nothing is lost: it is unsynced, and nothing outside the tooltip
  path reads it.
- **Four serverbound packets trusted the client.** All 23 were read individually.
  `HoneyGlueChangeBoundsPacket` took an entity id and a box with neither checked,
  so any honey glue anywhere could be resized to anything or killed; it now
  requires a finite box and a reachable entity. `HoneyGlueSpawnPacket` took two
  positions and never checked the player was near either — `HoneyGlueMaxSizing`
  bounds the size but says nothing about where the box is — so both corners are
  now reach-checked. `RopeBreakPacket` resolved a strand by id and destroyed it
  from any distance; it now reach-checks the strand's anchor.
  `PhysicsStaffDragPacket` skipped the held-item check its sibling
  `PhysicsStaffActionPacket` performs, which made that check bypassable.
  The other nineteen were already guarded, several through
  `SimBlockEntityConfigurationPacket` rather than at the call site.
- The mixin audit runs in CI, beside `check_resources.py`.

**What `.17` does not establish.** The audit proves each mixin *matches* the
class it targets; it cannot prove the injected logic is *right*, as the
`turnPlayer` ordinals above show. Nothing in V1 below has been played. The
sections that follow stay open until someone has.

**Notes log — Homestead `.18`, the first pack launch, and a defect class the
development client cannot find.**

`.17` crashed on launch in Homestead, at mixin apply, before any crash report was
written. One root cause, in `torsion_spring.ComparatorBlockMixin`:

```
SugarApplicationException: Unable to find matching local!
  @Local(name = "direction") ... in target method
  net/minecraft/class_2286::method_9991
```

**Why the development client did not catch this, and could not have.** A
`@Local(name = ...)` matches on the target's local variable table. Loom hands
development a Minecraft jar carrying *mapped* local names, so `"direction"`
resolves there and the game starts. The shipped client is Mojang's obfuscated
jar: it still carries the table, but the names in it are obfuscated, so the same
mixin cannot find `"direction"` and dies. Development is the one environment in
which this bug does not reproduce — running the dev client is not evidence about
the pack, and `.17`'s clean dev boot was worth less than it appeared.

The fix is positional. `getInputSignal` has exactly one `Direction` in scope at
the wrapped call, so `@Local(ordinal = 0)` names the same local in both
environments. Ordinals are positional and survive obfuscation; names do not.

**The audit now covers this class of defect**, and reproduces the crash without
launching anything — it reports the offending mixin *and* computes the ordinal to
replace the name with. It also draws the distinction that matters rather than
banning the construct: a name-addressed local is only a defect when the target is
a vanilla class. Mod jars are not obfuscated and ship real local names, so the
port's other five name-addressed locals are fine. That was not assumed — each was
checked against the Create jar **in the pack**, not the development copy:
`context`, `level` and `target` in the display link and schematic printer mixins,
and `impactId` in `KineticStats`, which the shipped jar carries in two scopes.

The refmap was ruled out as a factor: the built jar carries 106 mapped references
across 63 classes, and the log shows the target itself resolved correctly to
`class_2286::method_9991`. Only the local name failed.

Boxes below carry three states:

| | Meaning |
|---|---|
| `[ ]` | not written, or an observation nobody has made yet |
| `[-]` | **written, and machine-checked as far as it can be, but never played** |
| `[x]` | *shown* true — by the generated output, by a dedicated-server boot, or by `tools/check_resources.py` / `tools/mixin_audit/run.py` |

`[-]` is the state most of V1 is in as of `.17`. It means the content is
registered, its resources resolve, its mixins match the classes they target, and
it compiles and packages — and that nobody has yet stood in front of it in game.
Anything whose truth needs a person looking at the game cannot go past `[-]`
until they have.

- [x] All Simulated registries live (blocks, items, block entities, entities,
      menus, particles, sounds, data serializers, stats, tags, recipe types).
      96 blocks, 86 items, and the seven statistics come back from the
      registries on a dedicated-server boot.
- [x] Server config exists and is honoured: assembly limits, kinetics, stress,
      equipment, physics section (may be inert until V2). `simulated-server.toml`
      is written with upstream's sections and defaults.
- [x] Client config exists and is honoured: block and item client options.
      `simulated-client.toml` likewise.
- [x] Config changes apply without a restart wherever upstream allows it —
      `ConfigBase.onLoad` and `onReload` run off Forge Config API Port's
      `ModConfigEvents`, which is the same pair of callbacks NeoForge fires.
- [x] Networking layer carries every upstream packet shape (39 packets across
      assembly, diagram, end sea, handle, honey glue, typewriter, lodestone,
      nameplate, physics assembler, physics staff, rope, spring, plunger,
      throttle, merging glue, steering wheel). 39 classes, 37 registrations,
      matching upstream exactly.
- [x] Packets are validated server-side: no client-supplied position, entity, or
      inventory index is trusted without a range/ownership check. All 23
      serverbound packets have now been read individually; the four that trusted
      the client are fixed. See the `.17` note below.
- [-] Creative tab reproduces upstream ordering, sections, and hidden-item rules.
      The section grouping, ordering, row padding and the `SEARCH_ONLY` /
      `INVISIBLE` rules are line-for-line upstream's; only the per-section banner
      render is absent, and that is a V4 item for the reason recorded above.
      Stays open because whether the tab *looks* right needs a person.
- [x] Every upstream `simulated:` tag (blocks, items, plus the `c:`, `create:`,
      `minecraft:`, `sable:` tag contributions) is present — 22 tag files, and
      every `c:` tag they and the recipes name resolves against what the
      Homestead stack actually ships. See the `.16` note below.
- [x] Recipes: crafting, mechanical crafting, sequenced assembly, filling,
      processing, and the portable-engine dyeing recipe. 73 recipes and their 66
      unlock advancements.
- [x] Loot tables for every block — 94, the two without being the Paired Docking
      Connector and the Physics Assembler Anchor, which upstream declares
      `noLootTable()`.
- [x] All 30 upstream advancements, with their triggers registered. (The plan
      said 60; upstream's `SimAdvancements` declares 30, and the Fabric copy has
      all of them.) Whether each trigger *fires* is in-game testing.
- [x] All 7 upstream statistics registered, and present in the menu before first
      award, as upstream's bootstrap arranges. Whether each increments is
      in-game testing.
- [x] Sound events and subtitles registered; no missing-sound log spam. 29
      definitions in a generated `sounds.json`, every `.ogg` resolving.
- [x] Full `en_us` lang parity, including tooltips, and the Crowdin languages
      carried over. 652 keys, containing all 233 of upstream's hand-written ones
      and every literal key the source names.

### 1.2 Redstone and logic blocks

- [-] Linked Typewriter — block, screen, key binding, frequency binding, Linked
      Controller integration, persistence through break/place.
- [-] Directional Linked Receiver — directional signal scaling.
- [-] Modulating Linked Receiver — signal-strength scaling, configuration screen.
- [-] Redstone Accumulator — input delay setting.
- [-] Redstone Inductor — copy/invert modes.
- [-] Redstone Magnet.
- [-] Analog Transmission.
- [-] Directional Gearshift.
- [-] Throttle Lever.

### 1.3 Sensors and instruments

- [-] Altitude Sensor (+ configuration packet).
- [-] Velocity Sensor.
- [-] Gimbal Sensor.
- [-] Optical Sensor.
- [-] Laser Pointer and Laser Sensor, including the beam.
- [-] Navigation Table + navigation targets, including lodestone-compass
      compatibility.
- [-] Docking Connector and Paired Docking Connector, including alignment,
      pairing, and the `a_calculated_connection` advancement.
- [-] All 10 display sources feed Create display links correctly.

### 1.4 Kinetics and machinery

- [-] Auger Shaft and Auger Cogwheel, including auger groups.
- [-] Portable Engine, all 16 dye colours, including fuelling and the
      `portable_engines_fed` statistic.
- [-] Symmetric Sail, all 16 dye colours.
- [-] Torsion Spring.
- [-] Nameplate, all 16 dye colours — naming, dye recolour, honeycomb lock.
- [-] Handle, iron and copper plus all 16 dye colours — grab, sneak-grab, scroll
      distance. (Contraption-relative grabbing may stay inert until V3.)
- [-] Steering Wheel — upstream block entity, animation, analog output. Replaces
      the current shell.
- [-] Extra-kinetics behaviour: auto-orientation, goggle tooltips, dynamic
      stress, custom stress-impact tooltips.

### 1.5 Items and entities

- [-] Contraption Diagram item + entity + screen (may show placeholder physics
      data until V2).
- [-] Creative Physics Staff — registered, renders, held-item model. (Function
      lands in V3.)
- [-] Plunger Launcher + Launched Plunger entity, including backtank air
      pressure.
- [-] Honey Glue item + entity + bounds sync.
- [-] Merging Glue block + item.
- [-] Spring item + block.
- [-] Rope Coupling item.
- [ ] Void Anchor. `VoidAnchorBlockEntity` and `VoidAnchorRenderer` are ported,
      but **upstream never registers a Void Anchor block either** — on `main` the
      same two classes are orphans with no `SimBlocks` entry, no block entity type
      and no resources. The port matches upstream exactly, so there is nothing to
      test here until upstream finishes it. Left unchecked deliberately.

### 1.6 World and client

- [-] `Airship Ready` world preset.
- [-] `End Sea` world preset and biome, including rendering, fade, and shadow.
- [-] Particle types.
- [-] Keybindings registered and shown under a Simulated category, with the
      current placeholder flight keys either implemented or removed from lang.
- [-] All 11 ponder scene groups render and play: auger shaft, docking
      connector, honey glue, items, kinetics, physics assembler, redstone, rope,
      sensors, swivel bearing, symmetric sail. (Physics-dependent scenes may be
      staged for V3 if they cannot be made truthful yet — list which.)
- [-] Ponder tags and index entries.
- [-] JEI integration: hidden items, custom categories, the portable-engine
      dyeing recipe view.
- [ ] Sodium and Iris in the Homestead pack cause no rendering breakage.

### 1.7 Mod compatibility

- [-] ComputerCraft peripherals: altitude sensor, directional link, docking
      connector, gimbal sensor, linked typewriter, modulating link, nameplate,
      nav table, optical sensor, swivel bearing, torsion spring, velocity
      sensor, plus wired modem support.
- [-] Nature's Compass and Explorer's Compass navigation targets.
- [-] Extendo Grip, Diving Boots, and Schematicannon compatibility fixes.
- [-] Every compat is soft: absent mod ⇒ no crash, no log spam.

### 1.8 Carry-over defects from the current branch

**Cleared before this plan was committed:**

- [x] Deleted the unreferenced `FabricAssemblyPlan` and `PreparedAssembly`
      preflight scaffolding, superseded by the working assemble path. Git
      history retains them.
- [x] Kept `FabricAssemblyScanner.ScanStats` — its counters were computed on
      every scan and read by nothing after those classes went away. The
      scan-rule summary is now reported by `/simulated assembler_state`, and
      carried onto the anchor block entity so a live assembly still reports it.
- [x] Deleted the dead `fabric_test_block` blockstate, block model, and item
      model.
- [x] Dropped the `key.categories.simulated` and `key.simulated.flight_*` lang
      entries; no keybinds were registered for them and the helm uses vanilla
      movement keys. Added the missing `physics_assembler_anchor` name.
- [x] Flight sessions are cleared on player disconnect, which also releases the
      craft's controlling-player flag so someone else can take the helm.
- [x] Steering Wheel floor and ceiling outlines were two identical boxes. Both
      now use upstream's mount-plus-wheel geometry, collision is mount-only as
      upstream, and placement picks floor/ceiling from the player's vertical
      look direction rather than the clicked face.
- [x] Helms now advance on the server tick instead of on packet arrival, so
      thrust and braking are frame-rate and packet-rate independent. The client
      transmits input only when it changes, and the server only broadcasts a
      contraption position when it actually moved.
- [x] The client position packet now checks the contraption type, not just the
      entity class — a stale or reused entity id could previously teleport an
      unrelated Create contraption such as a bearing or a windmill.
- [x] Assembly glue lookup ran one entity query per candidate neighbour pair.
      It now runs one query per visited position, priming a cache that all
      eighteen neighbour checks read. Results are identical: a pair reaches at
      most one block past its anchor, so a radius of 17 around the position
      covers every box the per-pair search would have used.
- [x] Scan results now preserve discovery order. They were copied into an
      unordered set, which made an identical structure assemble differently
      between runs and port bugs harder to reproduce.

**Two items from the first review pass were wrong and are recorded here so
nobody re-opens them:**

- The chassis call passes `null` as Create's `forcedMovement` **`Direction`**,
  not as a player. `null` is correct for an omnidirectional physics assembly.
- The custom position packet does **not** fight vanilla entity tracking.
  `ControlledContraptionEntity` overrides both `lerpTo` and `moveTo` as no-ops,
  so vanilla movement packets are inert for it and the custom packet is the only
  client-side position source. `AbstractContraptionEntity` also refreshes
  `xo/yo/zo` at the start of each tick, so a mid-tick `setPos` still renders
  interpolated. The packet is necessary, not redundant.

**Still open:**

- [ ] Measure the assembly scan on a large structure — several thousand blocks
      with heavy glue — and record the number before V1 ships. The per-position
      query above is a large improvement but is not a measurement.
- [x] Fabric scanner special cases brought across, now that their blocks exist:
      honey glue (seeded before the walk and cached per position, at the
      configured `honeyGlueRange`), swivel bearing attachment, chain conveyor
      revalidation, the `SimBlockMovementChecks` additional-blocks and
      attachment hooks this port's own blocks register through, and the
      configured `maxBlocksMoved` in place of a hard-coded 128,000.

      **Two items on the old list were wrong and are recorded so nobody
      re-opens them.** Neither is a scanner concern upstream:

      - *Merging glue* appears nowhere in upstream's `SimAssemblyContraption`.
        It is a block that merges two sub-levels, which is a Sable concept; it
        has nothing to do with structure discovery.
      - *Assembly preventer* is `DisassemblyPrevention`, which gates
        **dis**assembly, not the scan. It was unreferenced on this branch; it is
        now called from `PhysicsAssemblerBlockEntity.disassembleActive` where
        upstream calls it. It asks Sable which sub-level contains a position, so
        against the inert facade it always permits — the call site exists so the
        rule arrives with V2 rather than being rediscovered.

      One deviation recorded rather than dropped: honey glue entities decide
      **which blocks join** an assembly, but they do not travel with it. The
      stand-in transport is a Create `ControlledContraptionEntity`, which carries
      Create's own `SuperGlueEntity` and knows nothing about honey glue. Carrying
      them lands with the Sable contraption in V2.

---

## V2 — Sable

**Goal:** real rigid-body physics on Fabric 1.20.1. The Physics Assembler stops
being a Create contraption and becomes a Sable sub-level.

**Ship criteria**

- [ ] A Sable build loads in the Homestead pack on Fabric 1.20.1, client and
      dedicated server.
- [ ] Assembling with the Physics Assembler produces a real physics body that
      falls, tips, collides with the world, and comes to rest.
- [ ] The contraption renders correctly with vanilla, Flywheel, Sodium, and Iris.
- [ ] Save, quit, relaunch: the contraption is still there, in the same place,
      with the same state, unassembled.
- [ ] Two players see the same contraption in the same place, with no rubber-
      banding at normal ping.
- [ ] The temporary Create transport, the invisible anchor block, the shutdown
      force-disassembly, and the hand-rolled helm are all gone.

### 2.1 Size the work honestly (do this first)

- [x] Sable CI compiles with the renderer, mixins, client, and loader code
      **included** — not excluded — so the error count is real.
- [x] Veil is on the classpath in some form, so `foundry.veil.*` errors reflect
      an actual API gap rather than a missing dependency.
- [x] Written decision recorded on the Veil question (backport / replace / shim),
      with the measured cost of each.
- [x] Written decision recorded on Sable's 1.20.5+ networking APIs.
- [x] The renderer and mixin surface is measured and reported, not deferred.

**Notes log — the measurement, `2026-09-06`.**

`sable-backport/prepare_sable.py` now takes flags, and `Sable 1.20.1 backport`
runs three configurations instead of one. `Compile Sable 2.0.4 core` is unchanged
and is still the gate. The two new `Measure Sable 2.0.4 surface` jobs report and
do not gate, because the count is expected to be non-zero for as long as V2 is in
progress; what matters is that it is honest and that it moves. Each writes a
breakdown to the job summary through `sable-backport/summarise_errors.py`.

| Configuration | Excluded | Veil | Errors |
|---|---|---|---|
| `Compile Sable 2.0.4 core` (the gate) | renderer, mixins, client, loader | absent | **349** |
| gate exclusions, Veil added | renderer, mixins, client, loader | `1.0.0.296` | **302** |
| `--full` | nothing | `1.0.0.296` | **495** |
| `--full --shim` | nothing | `1.0.0.296` + backported packet layer | **267** |

The gate prints `100 errors`, which is javac's default cap rather than a count —
it was reproduced locally at that cap, and 349 is what the same configuration
reports once `-Xmaxerrs` is raised. Even 349 is only the headless core: the
exclusions hide 256 of the 534 files in `common`, 48% of them and 38% of the
lines.

**The single most useful thing this measurement found:** the errors are not
spread evenly across the port. 208 of the core's 302 are in the three `network/`
packages, and that is the shallowest problem of the lot. Sable's packets use a
tiny, entirely mechanical slice of the modern API:

| Type | Files | What Sable actually uses |
|---|---|---|
| `StreamCodec` | 27 | `of`, `composite` (arity 1–3), `ofMember`, `apply` |
| `RegistryFriendlyByteBuf` | 23 | a type parameter; two of its three construction sites pass a null registry access |
| `CustomPacketPayload` | 20 | the nested `Type` holder and `type()` |
| `PacketContext` (Veil) | 24 | `level()` and `player()`, nothing else |
| `ByteBufCodecs` | 6 | 6 primitives, `fromCodec`, `collection`, `optional` |
| `VeilPacketManager` (Veil) | 6 | `create`, `registerClientbound`, `registerServerbound`, `PacketSink`, `player`, `server` |

So it was written: `sable-backport/shim/`, six types, 605 lines, supplying
those on 1.20.1 over Fabric's own play networking. `prepare_sable.py --shim`
rewrites the six imports and the two vanilla `STREAM_CODEC` field constants —
a shim cannot add static fields to `UUIDUtil` or `ResourceLocation` — and copies
the shim in. Nothing else in Sable changes. **Errors fall from 495 to 267, and
the shim itself compiles clean.** `network/packets`, `network/tcp`,
`physics/config`, `physics/floating_block`, `sublevel/system` and `util` all go
to zero in one move. `network/udp` drops from 18 to 5; what survives there is the
UDP fast path reaching for `ProtocolSwapHandler`, `DisconnectionDetails` and
`CommonListenerCookie`, which are connection-plumbing changes from 1.20.2/1.20.5
rather than packet-shape ones, and are their own small job.

**Decision — Veil: replace, do not backport.** Backporting Veil 4.3.2 to 1.20.1
was never the right shape of job, and the measurement says it is not needed.
Of the 28 Veil types Sable imports, 15 are already in `Veil-fabric-1.20.1`
`1.0.0.296`, including `foundry.veil.platform.registry` and
`foundry.veil.api.client.editor`, which this plan previously recorded as absent.
The 13 that are missing split cleanly in two:

- **Networking — 2 types, `VeilPacketManager` and `PacketContext`, used by 24
  files.** Veil's 1.20.1 line has no `foundry.veil.api.network` package at all.
  Measured cost to replace: done, part of the 605-line shim above.
- **Renderer — 11 types**: `VertexArray`, `VertexArrayBuilder`, `ShaderBlock`,
  `DynamicShaderBlock`, `ShaderUniform`, `VanillaShaderCompiler`,
  `VeilRenderProfiler`, `RenderProfilerCounter`, `SingleWindowInspector`,
  `IrisCompat`, `SodiumCompat`. These are the real Veil dependency, and they are
  a renderer problem, not a physics one. See below.

**Decision — Sable's 1.20.5+ networking APIs: shim them, do not rewrite Sable.**
The alternative was editing 40-odd Sable files to use `FriendlyByteBuf` and
Fabric channels directly. That is more code, has to be re-done on every Sable
version bump, and would leave the port unable to track upstream. The shim keeps
Sable's sources byte-identical apart from six import lines, and it encodes on the
wire exactly as upstream does — `fromCodec` still goes through NBT — so a 1.20.1
and a 1.21.1 client would agree on the bytes. One caveat recorded rather than
hidden: the shim's transport is Fabric-specific and currently sits in Sable's
`common` module, which the probe compiles into `:fabric` anyway. A production
build wants it behind the loader split.

**Renderer surface, measured.** 32 files and 4,582 lines of renderer
(`sublevel/render` 21 files / 3,375 lines, `render/**` 11 files / 1,207 lines),
plus 27 render-path mixins. After the shim it accounts for **124 of the 267
remaining errors**, from three causes:

- Veil 4's GLSL processing stack. `io.github.ocelot.glslprocessor` is 33 of the
  errors on its own; it is a separate Ocelot library that Veil 4 pulls in and
  that Veil `1.0.0.296` does not bundle. Three shader preprocessors depend on it.
- The 11 missing Veil renderer types above.
- Two vanilla renames: `SectionRenderDispatcher` (1.20.2 renamed
  `ChunkRenderDispatcher`, 10 errors) and `DeltaTracker` (1.21, 10 files).

This is the honest hard part of V2, and it is the part the old exclusions were
hiding. It does not block physics — it blocks *seeing* physics.

**Mixin surface, measured, with the caveat that matters.** 229 mixins, 13,092
lines, across 49 feature groups; `entity` alone is 71. 208 of them declare a
`@Mixin` target: **199 vanilla targets resolve to a class that still exists on
1.20.1**, 7 do not (`EnchantingTableBlockEntity`, `Leashable`,
`PathfindingContext`, `OptionsScreen`, `ProjectileDispenseBehavior`, and two on
`SectionRenderDispatcher.RenderSection`), and 4 could not be resolved from
imports.

That number is encouraging and it is also nearly meaningless on its own, so:
**compiling proves almost nothing about a mixin.** The target class existing says
nothing about the injection point existing. Behind those 229 files are 166
`@Inject`, 71 `@Redirect`, 54 `@WrapOperation`, 24 `@Overwrite`, 17
`@ModifyReturnValue`, and 331 `method =` targets against 171 `@At` descriptors,
every one of which is resolved at *apply* time against 1.20.1 bytecode. A green
compile with a red game is the expected failure mode here. V2 must not treat
mixin work as measurable by CI compile alone — it is measurable by the game
starting.

**Optional-mod compatibility is not a physics problem.** 73 of the 267 remaining
errors are `mixin/compatibility` and its helpers, against Shoulder Surfing,
Sodium 0.6 (`net.caffeinemc.mods.sodium`, a package that does not exist on 1.20.1
— Sodium there is `me.jellysquid.mods.sodium`), Iris, Jade, Vista, Moonlight,
Exposure and CC:Tweaked. None is required for physics, and Homestead ships few of
them. Treat the whole directory as droppable and revisit per mod; Sodium and Iris
specifically come back as V2 ship criteria because Homestead does ship those.

**Where that leaves V2.** After the shim, of 267 errors: 124 renderer, 73
optional-mod compat, and roughly 70 spread thin across small vanilla renames
(`mixin/recoil` 5, `mixin/entity` 10, `mixin/options` 3, `mixin/enchanting_table`
3, the `network/udp` fast path 5+4, the Forge/NeoForge config port 5, and the
loader entrypoints in `fabric/**` 14). The physics core — sub-levels, rigid
bodies, constraints, storage, block properties, dimension physics — is no longer
the problem. **The renderer is V2's real cost, and the mixin apply pass is its
real risk.**

Reproduce any of it:

```bash
git clone https://github.com/ryanhcode/sable.git .sable
git -C .sable checkout 22b8ce976dc2877eae3a1a4d2646a57d1ea559ff
python3 sable-backport/prepare_sable.py .sable <companion-jar-dir> --full --shim
(cd .sable && ./gradlew :fabric:compileJava --no-daemon --console=plain) 2>&1 | tee compile.log
python3 sable-backport/summarise_errors.py compile.log
```

### 2.2 Sable running

- [ ] Sable Companion ships as a usable 1.20.1 artifact, not just a CI probe.
- [ ] Sable core compiles, loads, and initialises on Fabric 1.20.1.
- [ ] Sub-levels create, tick, and dispose.
- [ ] Sub-level storage, chunk holding, and removal reasons work.
- [ ] Rigid bodies: mass data, forces, force groups, torque.
- [ ] Constraints: fixed, free, joint axes.
- [ ] Oriented bounding boxes and collision shapes.
- [ ] Physics block properties and the block-property tag data.
- [ ] Dimension physics data (gravity, air density) per dimension.
- [ ] Entities can stand on, ride, and be carried by sub-levels.
- [ ] Particles interact with sub-levels.
- [ ] Sable commands and debug output usable by an admin.
- [ ] Sub-level rendering: vanilla path, Flywheel path, Sodium compat, Iris
      compat, water occlusion.
- [ ] Sable config is exposed and documented.

### 2.3 Simulated on Sable

- [ ] Physics Assembler assembles into a Sable sub-level using the full upstream
      scan rules, including all special cases listed in 1.8.
- [ ] Disassembly returns blocks and block entities to the world with contents,
      orientation, and NBT intact.
- [ ] Assembly failures surface upstream's messages and the failure packet, not
      ad-hoc strings.
- [ ] Assembly-preventer blocks are honoured.
- [ ] Assembly limits from config are honoured.
- [ ] Sub-level block entities tick; block entity sub-level actors run.
- [ ] Kinetic networks survive assembly and keep running inside the contraption.
- [ ] Inventories, tanks, and energy inside a contraption remain accessible.
- [ ] Contraptions persist across save/load and across chunk unload/reload.
- [ ] Contraptions survive a server restart without being force-disassembled.
- [ ] Contraption naming (Nameplate) applies to the real physics body.
- [ ] The Contraption Diagram shows real forces, mass, and torque.

---

## V3 — Simulated Complete

**Goal:** every remaining Simulated feature — the ones that only make sense with
physics — plus the hardening pass that makes it safe to run on a real server.

**Ship criteria**

- [ ] Every ID and behaviour in upstream `simulated` exists and behaves as
      upstream, or has a written, deliberate deviation.
- [ ] A build-heavy multi-contraption world runs at acceptable tick time on a
      dedicated server with several players — target agreed and measured.
- [ ] No known crash, dupe, void-loss, or save-corruption path.

### 3.1 Physics-dependent content

- [ ] Swivel Bearing + Swivel Bearing Link Block: rotation, constraints, mass
      handling, honey-glue and glue special cases, wrench behaviour.
- [ ] Rope system: Rope Winch, Rope Connector, strands, rope riding, rope
      breaking, client and server strand rendering, `learning_the_ropes`.
- [ ] Spring block and Spring item: strength adjustment, resting-length
      adjustment, item bounce.
- [ ] Torsion Spring physics behaviour.
- [ ] Plunger Launcher: fired plungers connect and pull contraptions together.
- [ ] Creative Physics Staff: lock, drag, rotate, distance scroll, beam render,
      multi-user drag sessions.
- [ ] Handle: contraption-relative grabbing, fall-break, `get_a_grip` and
      `got_a_grip`.
- [ ] Honey Glue and Merging Glue full behaviour, including merging two
      contraptions and the direction-error messages.
- [ ] Absorber — decide: implement, or ship as the explicit placeholder upstream
      ships. Record which.
- [ ] Steering Wheel, Throttle Lever, Linked Typewriter, and Handle drive real
      contraption control — replacing the V1 stand-in helm entirely.
- [ ] Docking Connector physically couples two contraptions.
- [ ] All sensors read real physics values (altitude, velocity, gimbal).
- [ ] End Sea physics and fade behaviour.
- [ ] Physics-dependent ponder scenes are truthful.

### 3.2 Hardening

- [ ] Server restart with live contraptions: clean, no loss, no duplication.
- [ ] Chunk unload/reload under a moving contraption: no loss, no duplication.
- [ ] Player disconnect while piloting, riding, grabbing, or dragging: clean.
- [ ] Dimension change with a contraption present: clean.
- [ ] `/kill`, `/setblock`, and world-edit style operations against a contraption
      do not corrupt the save.
- [ ] Contraption inside a contraption, and contraption colliding with
      contraption, behave or fail gracefully.
- [ ] Backwards compatibility: worlds saved by V2 load in V3.
- [ ] No client-authoritative state anywhere: every mutation is server-validated.
- [ ] Rate limits on all client-initiated packets.

### 3.3 Performance

- [ ] Assembly scan cost measured and acceptable at the configured block limit.
- [ ] Physics step cost measured per contraption and in aggregate.
- [ ] Network bandwidth per contraption per player measured; no per-tick
      full-state broadcasts.
- [ ] Rendering measured with vanilla, Flywheel, Sodium, and Iris.
- [ ] No per-tick allocation hot spots in assembly, physics, or sync paths.
- [ ] Profiled against a realistic Homestead world, not a superflat test world.
- [ ] Results written down in this repo so V4 can compare.

---

## V4 — Aeronautics, Offroad, and release

**Goal:** the full suite, packaged and released.

**Ship criteria**

- [ ] All three mods work together in Homestead.
- [ ] A player can build and fly a balloon, fly a propeller aircraft, and drive a
      wheeled vehicle.
- [ ] Released artifact, versioned, with a changelog.

### 4.1 Aeronautics

- [ ] Hot Air Envelope, all 16 colours, plus Envelope Encased Shaft, all 16.
- [ ] Hot Air Burner.
- [ ] Steam Vent.
- [ ] Lifting gas, balloon volume/graph/map simulation, balloon effects.
- [ ] Gust entity.
- [ ] Propeller Bearing and Gyroscopic Propeller Bearing, plus propeller bearing
      contraption and its sounds.
- [ ] Smart Propeller, Andesite Propeller, Wooden Propeller.
- [ ] Mounted Potato Cannon, including projectile behaviour.
- [ ] Levitite, Pearlescent Levitite, Levitite Blend, Levitite Blend Bucket, End
      Stone Powder, and the blend crystallization process.
- [ ] Aviator's Goggles.
- [ ] Cloud Skipper music disc and the situational music hook.
- [ ] Aeronautics ponder scenes, display sources, particles, config, advancements.
- [ ] Aeronautics rendering, including the Iris/Sodium/vanilla render paths.

### 4.2 Offroad

- [ ] Wheel Mount.
- [ ] Small Tire, Tire, Large Tire, Monstrous Tire — including placement rules
      and the minimum-friction option.
- [ ] Borehead Bearing and borehead contraption, including multi-block mining
      destruction progress.
- [ ] Rock Cutting Wheel.
- [ ] Offroad ponder scenes, config, network packets, advancements.

### 4.3 Release

- [ ] Bundled jar equivalent to `aeronautics-bundled`, or a documented decision
      not to bundle.
- [ ] Homestead pack integration verified end to end on a real server.
- [ ] Version numbering settled — the port has been on
      `1.3.2-fabric-homestead.N`; decide what ships publicly.
- [ ] Changelog, README, and this plan updated to reflect reality.
- [ ] Licensing and attribution reviewed, including anything taken from
      `bobqianic/create-aeronautics-fabric`.
- [ ] Crowdin translations wired up for the Fabric build.
- [ ] Release CI produces the published artifact.

---

## 5. Cross-cutting expectations

Applies to every version. Not architecture — outcomes.

- [ ] **CI stays green.** A red branch tip is a stop-the-line event.
- [ ] **Every version is installable.** No version ships in a state where the
      pack cannot launch.
- [ ] **Dedicated server is a first-class target,** tested every version, not
      just integrated singleplayer.
- [ ] **No feature is silently dropped.** If something cannot be ported, it gets
      a line in this document saying so and why.
- [ ] **Deviations from upstream behaviour are written down,** including the ones
      that are improvements.
- [ ] **Version bumps are meaningful.** Bump when a ship criterion set is met,
      not per commit.
- [ ] **Log hygiene.** No per-tick logging, no spam on a clean load.
- [ ] **Performance is measured, not asserted.** "Feels fine" is not a result.

---

## 6. Definition of done for the whole port

- [ ] Every block, item, entity, menu, recipe, tag, advancement, statistic,
      sound, particle, world preset, display source, ponder scene, and compat
      hook that exists in upstream `simulated`, `aeronautics`, and `offroad`
      exists and works on Fabric 1.20.1 in Homestead — or has a written,
      deliberate exception.
- [ ] Physics is real Sable physics, not a Create stand-in.
- [ ] Multiplayer, persistence, and restart behaviour are correct.
- [ ] Performance is measured and acceptable on a real server under real load.
- [ ] Released.

---

## 7. Notes log

Append findings, decisions, reversals, and measurements here as work proceeds.
This is the record of *why* the plan changed, which matters more than the plan.

- `2026-09-06` — Plan created. Branch at `1.3.2-fabric-homestead.13`, commit
  `077b7ec`. Fabric CI green; Sable CI red at `Compile Sable 2.0.4 core`.
  Confirmed Create Fabric `6.0.8.1+build.1744-mc1.20.1` nests Ponder `1.0.91`,
  Registrate `1.3.79-MC1.20.1`, Flywheel `1.0.5-264`, Porting Lib `2.3.13`, and
  Forge Config API Port `8.0.0` for 1.20.1. Confirmed no Veil 4.x exists for
  1.20.1 on `maven.blamejared.com` (newest `Veil-fabric-1.20.1` is `1.0.0.86`).
- `2026-09-06` — Cleared the carry-over defects in 1.8 in the same commit as this
  plan. Two findings from the first review pass were checked against the Create
  6.0.8.1 sources and turned out to be wrong; both are recorded in 1.8 rather
  than deleted, so they do not get re-reported later.
- `2026-09-06` — Homestead `.14`/`.15` landed: content registers through
  Registrate and the resource tree is generated. Full write-up in V1 §1.1.
- `2026-09-06` — **Took the V2 §2.1 measurement, and it changed the shape of V2.**
  Numbers and decisions are in §2.1; what belongs here is why the old picture was
  wrong. The Sable gate probe reports `100 errors`, which reads like an error
  count and is javac's default cap. The same configuration reports 349 once
  `-Xmaxerrs` is raised, and the gate also excludes 256 of 534 files, so the true
  figure is 495. Two things follow. First, **never quote a javac count without raising
  `-Xmaxerrs`** — the new measurement jobs set it to 100000. Second, the recorded
  claim that no usable Veil exists for 1.20.1 was wrong in both particulars: the
  newest is `1.0.0.296`, not `1.0.0.86`, and it does contain
  `foundry.veil.platform.registry` and `foundry.veil.api.client.editor`, which
  this plan had listed as missing. It was checked by downloading the jar and
  diffing its class list against Sable's imports rather than by reading a version
  listing. The one real Veil gap on the networking side is that its 1.20.1 line
  has no `foundry.veil.api.network` package at all.
- `2026-09-06` — Wrote `sable-backport/shim/` rather than only costing it, because
  §2.1 asks for the *measured* cost of each option and an estimate would not have
  been that. 605 lines took the whole surface from 495 errors to 267 and compiles
  clean itself. The residual is 124 renderer, 73 optional-mod compat, ~70 spread
  thin. Recorded as a deviation: the shim's transport is Fabric-specific and
  currently lives in Sable's `common` module, which only works because the probe
  compiles `common` into `:fabric`. A production build needs it behind the loader
  split. Also recorded, because it is the thing most likely to be forgotten: 199
  of Sable's mixins target classes that still exist on 1.20.1, and that fact is
  nearly worthless — injection points resolve at apply time, not compile time, so
  a green compile here predicts very little about the game starting.
