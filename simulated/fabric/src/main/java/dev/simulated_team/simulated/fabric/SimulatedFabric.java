package dev.simulated_team.simulated.fabric;

import com.mojang.brigadier.CommandDispatcher;
import dev.simulated_team.simulated.content.blocks.physics_assembler.FabricAssemblyPlan;
import dev.simulated_team.simulated.content.blocks.physics_assembler.FabricAssemblyScanner;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlock;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PreparedAssembly;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric bootstrap for the Minecraft 1.20.1 / Homestead port.
 */
public final class SimulatedFabric implements ModInitializer {
    public static final String MOD_ID = "simulated";
    public static final Logger LOGGER = LoggerFactory.getLogger("Create Simulated");

    @Override
    public void onInitialize() {
        SimulatedFabricContent.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));

        final boolean createLoaded = FabricLoader.getInstance().isModLoaded("create");
        LOGGER.info("Starting Create Simulated Fabric port for Homestead 1.20.1; Create loaded={}", createLoaded);
    }

    private static void registerCommands(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("simulated")
                .then(Commands.literal("status")
                        .executes(context -> {
                            final boolean createLoaded = FabricLoader.getInstance().isModLoaded("create");
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Create: Simulated Fabric port loaded. Create=" + createLoaded
                                            + "; scanner + persistent preflight lifecycle enabled; Sable physics handoff not enabled yet."), false);
                            return 1;
                        }))
                .then(Commands.literal("compatibility")
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Homestead runtime: "
                                            + modVersion("fabricloader") + "; "
                                            + modVersion("fabric-api") + "; "
                                            + modVersion("create") + "; "
                                            + modVersion("flywheel") + "; "
                                            + modVersion("jei") + "; "
                                            + modVersion("sodium") + "; "
                                            + modVersion("iris") + "; "
                                            + modVersion("indium") + "; "
                                            + modVersion("kubejs")), false);
                            return 1;
                        }))
                .then(Commands.literal("assembler_state")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> reportAssemblerState(
                                        context.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                .then(Commands.literal("assembler_preflight")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> runAssemblerPreflight(
                                        context.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                .then(Commands.literal("give_core_items")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            give(context.getSource(), new ItemStack(SimulatedFabricContent.GYROSCOPIC_MECHANISM));
                            give(context.getSource(), new ItemStack(SimulatedFabricContent.ENGINE_ASSEMBLY));
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Gave the first ported Simulated items"), false);
                            return 1;
                        }))
                .then(Commands.literal("give_physics_assembler")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            give(context.getSource(), new ItemStack(SimulatedFabricContent.PHYSICS_ASSEMBLER));
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Gave Physics Assembler"), false);
                            return 1;
                        })));
    }

    private static int reportAssemblerState(final CommandSourceStack source, final BlockPos pos) {
        final var blockEntity = source.getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof final PhysicsAssemblerBlockEntity assembler)) {
            source.sendFailure(Component.literal("No Physics Assembler block entity at " + shortPos(pos)));
            return 0;
        }

        final PreparedAssembly prepared = assembler.getPreparedAssembly();
        final String snapshot = prepared == null
                ? "none"
                : prepared.blockCount() + " blocks, " + prepared.dimensions()
                        + ", bounds " + shortPos(prepared.min()) + " -> " + shortPos(prepared.max())
                        + ", " + prepared.preflightSummary()
                        + ", signature=" + Long.toUnsignedString(prepared.signature(), 16);
        source.sendSuccess(() -> Component.literal(
                "Physics Assembler " + shortPos(pos)
                        + ": state=" + assembler.getAssemblyState()
                        + ", snapshot=" + snapshot
                        + ", stableValidations=" + assembler.getStableValidationCount()
                        + ", interactions=" + assembler.getInteractionCount()
                        + (assembler.getLastError().isEmpty()
                                ? ""
                                : ", error=" + assembler.getLastError())), false);
        return 1;
    }

    private static int runAssemblerPreflight(final CommandSourceStack source, final BlockPos pos) {
        final var level = source.getLevel();
        final var state = level.getBlockState(pos);
        final var blockEntity = level.getBlockEntity(pos);
        if (!state.is(SimulatedFabricContent.PHYSICS_ASSEMBLER)
                || !(blockEntity instanceof final PhysicsAssemblerBlockEntity assembler)) {
            source.sendFailure(Component.literal("No Physics Assembler at " + shortPos(pos)));
            return 0;
        }

        final Direction stickyFacing = PhysicsAssemblerBlock.getStickyFacing(state);
        final BlockPos startPos = pos.relative(stickyFacing);
        final FabricAssemblyScanner.ScanResult scan = FabricAssemblyScanner.scan(level, pos, startPos);
        if (!scan.successful()) {
            source.sendFailure(Component.literal(
                    "Read-only preflight failed: " + scan.error() + " at " + shortPos(scan.problemPos())
                            + ". Saved assembler state was not changed."));
            return 0;
        }

        final FabricAssemblyPlan plan = FabricAssemblyPlan.capture(level, scan);
        final PreparedAssembly live = PreparedAssembly.capture(level, startPos, stickyFacing, scan, plan);
        final PreparedAssembly saved = assembler.getPreparedAssembly();
        final boolean matchesSaved = live.equals(saved);
        source.sendSuccess(() -> Component.literal(
                "Read-only preflight " + shortPos(pos)
                        + ": " + live.blockCount() + " blocks, size=" + live.dimensions()
                        + ", bounds " + shortPos(live.min()) + " -> " + shortPos(live.max())
                        + ", " + live.preflightSummary()
                        + ", signature=" + Long.toUnsignedString(live.signature(), 16)
                        + ", matchesSaved=" + matchesSaved
                        + (plan.hasDeferredCreateContraptions()
                                ? ", WARNING=intersecting moving Create contraption expansion is not ported yet"
                                : "")
                        + ". Saved assembler state was not changed."), false);
        return 1;
    }

    private static void give(final CommandSourceStack source, final ItemStack stack) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final var player = source.getPlayerOrException();
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static String modVersion(final String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> modId + "=" + container.getMetadata().getVersion().getFriendlyString())
                .orElse(modId + "=not-loaded");
    }

    private static String shortPos(final BlockPos pos) {
        if (pos == null) {
            return "?";
        }
        return "[" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "]";
    }
}
