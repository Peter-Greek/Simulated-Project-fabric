package dev.simulated_team.simulated.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblyContraption;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

        // The current Create-backed transport is deliberately temporary. Do not
        // persist its invisible controller relationship across server lifetimes;
        // that is Sable's job once the 1.20.1 backend is ready.
        ServerLifecycleEvents.SERVER_STOPPING.register(SimulatedFabric::recoverTemporaryPhysicsAssemblies);

        final boolean createLoaded = FabricLoader.getInstance().isModLoaded("create");
        LOGGER.info("Starting Create Simulated Fabric port for Homestead 1.20.1; Create loaded={}", createLoaded);
    }

    private static void recoverTemporaryPhysicsAssemblies(final MinecraftServer server) {
        int recovered = 0;
        int failed = 0;

        for (final ServerLevel level : server.getAllLevels()) {
            final List<ControlledContraptionEntity> transports = new ArrayList<>();
            for (final Entity entity : level.getAllEntities()) {
                if (entity instanceof final ControlledContraptionEntity controlled
                        && controlled.getContraption() instanceof PhysicsAssemblyContraption) {
                    transports.add(controlled);
                }
            }

            for (final ControlledContraptionEntity transport : transports) {
                if (PhysicsAssemblerBlockEntity.recoverTemporaryTransport(level, transport)) {
                    recovered++;
                } else {
                    failed++;
                }
            }
        }

        if (recovered > 0 || failed > 0) {
            LOGGER.info("Physics Assembler shutdown recovery: recovered={}, failed={}", recovered, failed);
        }
    }

    private static void registerCommands(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("simulated")
                .then(Commands.literal("status")
                        .executes(context -> {
                            final boolean createLoaded = FabricLoader.getInstance().isModLoaded("create");
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Create: Simulated Fabric port loaded. Create=" + createLoaded
                                            + "; Physics Assembler live Create transport enabled; Sable rigid-body physics pending."), false);
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
                                            + modVersion("kubejs") + "; "
                                            + modVersion("sable") + "; "
                                            + modVersion("sable_companion")), false);
                            return 1;
                        }))
                .then(Commands.literal("assembler_state")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reportTargetedAssemblerState(context.getSource()))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> reportAssemblerState(
                                        context.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                .then(Commands.literal("give_core_items")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            give(context.getSource(), new ItemStack(SimulatedFabricContent.GYROSCOPIC_MECHANISM));
                            give(context.getSource(), new ItemStack(SimulatedFabricContent.ENGINE_ASSEMBLY));
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Gave ported Simulated core items"), false);
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

    private static int reportTargetedAssemblerState(final CommandSourceStack source) throws CommandSyntaxException {
        final BlockPos pos = targetedBlockPos(source);
        if (pos == null) {
            source.sendFailure(Component.literal("Look directly at a Physics Assembler within 8 blocks, or supply coordinates."));
            return 0;
        }
        return reportAssemblerState(source, pos);
    }

    private static BlockPos targetedBlockPos(final CommandSourceStack source) throws CommandSyntaxException {
        final var player = source.getPlayerOrException();
        final HitResult hit = player.pick(8.0D, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        final BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        return source.getLevel().getBlockState(pos).is(SimulatedFabricContent.PHYSICS_ASSEMBLER)
                ? pos
                : null;
    }

    private static int reportAssemblerState(final CommandSourceStack source, final BlockPos pos) {
        final var blockEntity = source.getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof final PhysicsAssemblerBlockEntity assembler)) {
            source.sendFailure(Component.literal("No Physics Assembler block entity at " + shortPos(pos)));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
                "Physics Assembler " + shortPos(pos)
                        + ": state=" + assembler.getAssemblyState()
                        + ", active=" + assembler.hasActiveAssembly()
                        + ", blocks=" + assembler.getActiveBlockCount()
                        + ", interactions=" + assembler.getInteractionCount()
                        + (assembler.getLastError().isEmpty()
                                ? ""
                                : ", error=" + assembler.getLastError())), false);
        return 1;
    }

    private static void give(final CommandSourceStack source, final ItemStack stack) throws CommandSyntaxException {
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
