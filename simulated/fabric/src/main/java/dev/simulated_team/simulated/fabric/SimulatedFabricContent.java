package dev.simulated_team.simulated.fabric;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * First loader-neutral content moved from the upstream Simulated registries.
 *
 * Keep this deliberately small while the 1.20.1 backport is being proven in
 * Homestead. More complex items and blocks are added once their supporting
 * systems have been backported.
 */
public final class SimulatedFabricContent {
    public static final Item GYROSCOPIC_MECHANISM = new Item(new Item.Properties());
    public static final Item ENGINE_ASSEMBLY = new Item(new Item.Properties());

    private SimulatedFabricContent() {
    }

    public static void register() {
        registerItem("gyroscopic_mechanism", GYROSCOPIC_MECHANISM);
        registerItem("engine_assembly", ENGINE_ASSEMBLY);

        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                id("group"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.simulated.group"))
                        .icon(() -> new ItemStack(GYROSCOPIC_MECHANISM))
                        .displayItems((parameters, output) -> {
                            output.accept(GYROSCOPIC_MECHANISM);
                            output.accept(ENGINE_ASSEMBLY);
                        })
                        .build());
    }

    private static void registerItem(final String path, final Item item) {
        Registry.register(BuiltInRegistries.ITEM, id(path), item);
    }

    private static ResourceLocation id(final String path) {
        return new ResourceLocation(SimulatedFabric.MOD_ID, path);
    }
}
