package dev.simulated_team.simulated.fabric;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerMovingInteraction;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblyContraption;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelMovingInteraction;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric-side registration that Registrate does not cover: Create's contraption
 * type and moving-interaction registries, and the creative tab itself.
 */
public final class SimulatedFabricContent {
    private static Holder.Reference<ContraptionType> physicsAssemblyContraptionType;

    private SimulatedFabricContent() {
    }

    public static void register() {
        final ContraptionType contraptionType = new ContraptionType(PhysicsAssemblyContraption::new);
        physicsAssemblyContraptionType = Registry.registerForHolder(
                CreateBuiltInRegistries.CONTRAPTION_TYPE,
                id("physics_assembly"),
                contraptionType);

        Simulated.init();

        MovingInteractionBehaviour.REGISTRY.register(
                SimBlocks.PHYSICS_ASSEMBLER.get(),
                new PhysicsAssemblerMovingInteraction());
        MovingInteractionBehaviour.REGISTRY.register(
                SimBlocks.STEERING_WHEEL.get(),
                new SteeringWheelMovingInteraction());

        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                id("group"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.simulated.group"))
                        .icon(() -> new ItemStack(SimBlocks.PHYSICS_ASSEMBLER.get()))
                        .displayItems((parameters, output) -> SimulatedCreativeTab.processItems(
                                stack -> output.accept(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY),
                                stack -> output.accept(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY)))
                        .build());
    }

    public static ContraptionType physicsAssemblyContraptionType() {
        if (physicsAssemblyContraptionType == null) {
            throw new IllegalStateException("Physics Assembly contraption type has not been registered yet");
        }
        return physicsAssemblyContraptionType.value();
    }

    private static ResourceLocation id(final String path) {
        return new ResourceLocation(Simulated.MOD_ID, path);
    }
}
