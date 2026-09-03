package dev.simulated_team.simulated.fabric;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlock;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblyContraption;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Loader-neutral content moved from the upstream Simulated registries.
 */
public final class SimulatedFabricContent {
    public static final Item GYROSCOPIC_MECHANISM = new Item(new Item.Properties());
    public static final SequencedAssemblyItem INCOMPLETE_GYROSCOPIC_MECHANISM =
            new SequencedAssemblyItem(new Item.Properties());

    public static final Item ENGINE_ASSEMBLY = new Item(new Item.Properties());
    public static final SequencedAssemblyItem INCOMPLETE_ENGINE_ASSEMBLY =
            new SequencedAssemblyItem(new Item.Properties());

    public static final PhysicsAssemblerBlock PHYSICS_ASSEMBLER = new PhysicsAssemblerBlock(
            BlockBehaviour.Properties.of().strength(2.5F, 6.0F).noOcclusion());

    public static final BlockEntityType<PhysicsAssemblerBlockEntity> PHYSICS_ASSEMBLER_BLOCK_ENTITY =
            BlockEntityType.Builder.of(PhysicsAssemblerBlockEntity::new, PHYSICS_ASSEMBLER).build(null);

    private static Holder.Reference<ContraptionType> physicsAssemblyContraptionType;

    private SimulatedFabricContent() {
    }

    public static void register() {
        final ContraptionType contraptionType = new ContraptionType(PhysicsAssemblyContraption::new);
        physicsAssemblyContraptionType = Registry.registerForHolder(
                CreateBuiltInRegistries.CONTRAPTION_TYPE,
                id("physics_assembly"),
                contraptionType);

        registerItem("gyroscopic_mechanism", GYROSCOPIC_MECHANISM);
        registerItem("incomplete_gyroscopic_mechanism", INCOMPLETE_GYROSCOPIC_MECHANISM);
        registerItem("engine_assembly", ENGINE_ASSEMBLY);
        registerItem("incomplete_engine_assembly", INCOMPLETE_ENGINE_ASSEMBLY);
        registerBlockWithItem("physics_assembler", PHYSICS_ASSEMBLER);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("physics_assembler"), PHYSICS_ASSEMBLER_BLOCK_ENTITY);

        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                id("group"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.simulated.group"))
                        .icon(() -> new ItemStack(PHYSICS_ASSEMBLER))
                        .displayItems((parameters, output) -> {
                            output.accept(PHYSICS_ASSEMBLER);
                            output.accept(GYROSCOPIC_MECHANISM);
                            output.accept(ENGINE_ASSEMBLY);
                        })
                        .build());
    }

    public static ContraptionType physicsAssemblyContraptionType() {
        if (physicsAssemblyContraptionType == null) {
            throw new IllegalStateException("Physics Assembly contraption type has not been registered yet");
        }
        return physicsAssemblyContraptionType.value();
    }

    private static void registerItem(final String path, final Item item) {
        Registry.register(BuiltInRegistries.ITEM, id(path), item);
    }

    private static void registerBlockWithItem(final String path, final PhysicsAssemblerBlock block) {
        final ResourceLocation id = id(path);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
    }

    private static ResourceLocation id(final String path) {
        return new ResourceLocation(SimulatedFabric.MOD_ID, path);
    }
}
