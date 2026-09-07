package dev.simulated_team.simulated.fabric;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerMovingInteraction;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblyContraption;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelMovingInteraction;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.backport.physics.api.command.SubLevelArgumentType;
import dev.simulated_team.simulated.index.fabric.FabricSimStats;
import dev.simulated_team.simulated.index.fabric.SimFabricRecipeTypes;
import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric-side registration that Registrate does not cover: Create's contraption
 * type and moving-interaction registries, and the creative tab itself.
 */
public final class SimulatedFabricContent {
    private static Holder.Reference<ContraptionType> physicsAssemblyContraptionType;

    private static CreativeModeTab creativeTab;

    private SimulatedFabricContent() {
    }

    public static void register() {
        final ContraptionType contraptionType = new ContraptionType(PhysicsAssemblyContraption::new);
        physicsAssemblyContraptionType = Registry.registerForHolder(
                CreateBuiltInRegistries.CONTRAPTION_TYPE,
                id("physics_assembly"),
                contraptionType);

        // Upstream registers the statistics before Simulated.init() too:
        // block entities award them, so the ids have to exist first.
        FabricSimStats.register();

        Simulated.init();

        // Registrate on 1.20.1 Fabric queues every entry and only writes it to
        // the game registries when told to. Nothing downstream of here — the
        // moving-interaction registry, the tab icon — can see a block until it
        // has run, and an unregistered entry reads back as air rather than
        // failing, so this has to happen before the first get().
        Simulated.getRegistrate().register();

        // Upstream registers this through a NeoForge DeferredRegister of its
        // own, outside Registrate; it has the same standing here.
        SimFabricRecipeTypes.register();

        // The sub-level argument has to be in the registry even though it
        // never resolves one: the server serialises the whole command tree
        // to every client on join, and an unregistered argument type fails
        // that outright. Using /simulated lock says so, which is the honest
        // answer until V2.
        ArgumentTypeRegistry.registerArgumentType(
                id("sub_levels"),
                SubLevelArgumentType.class,
                SingletonArgumentInfo.contextFree(SubLevelArgumentType::subLevels));

        MovingInteractionBehaviour.REGISTRY.register(
                SimBlocks.PHYSICS_ASSEMBLER.get(),
                new PhysicsAssemblerMovingInteraction());
        MovingInteractionBehaviour.REGISTRY.register(
                SimBlocks.STEERING_WHEEL.get(),
                new SteeringWheelMovingInteraction());

        creativeTab = Registry.register(
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

    /** The tab the loader-neutral code reaches through {@code SimTabService}. */
    public static CreativeModeTab creativeTab() {
        if (creativeTab == null) {
            throw new IllegalStateException("Simulated's creative tab has not been registered yet");
        }
        return creativeTab;
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
