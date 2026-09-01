package dev.simulated_team.simulated.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                                    "Create: Simulated Fabric port is loaded. Create loaded=" + createLoaded), false);
                            return 1;
                        }))
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
                                    "Gave Physics Assembler placement-shell test block"), false);
                            return 1;
                        })));
    }

    private static void give(final CommandSourceStack source, final ItemStack stack) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final var player = source.getPlayerOrException();
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
