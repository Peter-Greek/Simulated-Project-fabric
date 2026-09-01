package dev.simulated_team.simulated.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric bootstrap for the Minecraft 1.20.1 / Homestead port.
 *
 * This currently exposes a deliberately isolated test block and commands so
 * the loader/build/resource path can be verified in the real Homestead pack
 * before upstream Simulated systems are backported one subsystem at a time.
 */
public final class SimulatedFabric implements ModInitializer {
    public static final String MOD_ID = "simulated";
    public static final Logger LOGGER = LoggerFactory.getLogger("Create Simulated");

    public static final Block FABRIC_TEST_BLOCK = new Block(
            BlockBehaviour.Properties.of().strength(2.0F, 6.0F));

    @Override
    public void onInitialize() {
        final ResourceLocation testBlockId = id("fabric_test_block");
        Registry.register(BuiltInRegistries.BLOCK, testBlockId, FABRIC_TEST_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, testBlockId,
                new BlockItem(FABRIC_TEST_BLOCK, new Item.Properties()));

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
                .then(Commands.literal("give_test_block")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            final ItemStack stack = new ItemStack(FABRIC_TEST_BLOCK.asItem());
                            final boolean inserted = context.getSource().getPlayerOrException().getInventory().add(stack);
                            if (!inserted) {
                                context.getSource().getPlayerOrException().drop(stack, false);
                            }
                            context.getSource().sendSuccess(() -> Component.literal(
                                    "Gave simulated:fabric_test_block"), false);
                            return 1;
                        })));
    }

    private static ResourceLocation id(final String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
