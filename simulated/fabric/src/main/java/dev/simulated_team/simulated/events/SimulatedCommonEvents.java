package dev.simulated_team.simulated.events;

import dev.simulated_team.simulated.backport.physics.api.sublevel.ServerSubLevelContainer;
import dev.simulated_team.simulated.backport.physics.api.sublevel.SubLevelContainer;
import dev.simulated_team.simulated.backport.physics.platform.SableEventPlatform;
import dev.simulated_team.simulated.backport.physics.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.backport.physics.sublevel.system.SubLevelTrackingSystem;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeTrackingSystem;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysicsData;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerServerHandler;
import dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability.LodestoneTrackingMap;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffSubLevelObserver;
import dev.simulated_team.simulated.content.worldgen.SimulatedWorldPreset;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.index.SimWorldPresets;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SimulatedCommonEvents {
    /**
     * Called at the end of the given ServerLevel's tick
     *
     * @param level The level ticking.
     */
    public static void onServerTickEnd(final ServerLevel level) {
        RedstoneMagnetBlockEntity.GLOBAL_REDSTONE_MAGNET_MAP.tick(level);
        DockingConnectorBlockEntity.MAGNET_CONTROLLER.tick(level);

	    final LodestoneTrackingMap lodestoneMap = LodestoneTrackingMap.getOrLoad(level);
		if (lodestoneMap != null) {
			lodestoneMap.tick();
		}
    }

    public static void onWorldLoad(final LevelAccessor level) {

    }

    /**
     * Called whenever a block is modified in the given level
     */
    public static void onBlockModifiedEvent(final LevelAccessor level, final BlockPos breakPos) {
    }

    public static void onServerStopped(final MinecraftServer server) {

    }

    public static void onPlayerLoggedIn(final Player player) {
        for (final Map.Entry<ResourceLocation, SimulatedWorldPreset> entry : SimWorldPresets.PRESETS.entrySet()) {
            final ServerLevel level = (ServerLevel) player.level();

            if (player instanceof final ServerPlayer serverPlayer) {
                final ResourceLocation worldPreset = ((PrimaryLevelDataExtension) level.getServer().getWorldData()).getPreset();

                if (entry.getValue().id().equals(worldPreset)) {
                    entry.getValue().onPlayerJoin(level, serverPlayer);
                }
            }
        }

        PhysicsStaffServerHandler.sendAllData(player);
    }

    public static void onChunkLoad(final LevelAccessor level, final ChunkAccess chunk, final boolean newChunk) {
        for (final Map.Entry<ResourceLocation, SimulatedWorldPreset> entry : SimWorldPresets.PRESETS.entrySet()) {
            if(level instanceof final ServerLevel serverLevel) {
                final ResourceLocation worldPreset = ((PrimaryLevelDataExtension) serverLevel.getServer().getWorldData()).getPreset();

                if(entry.getValue().id().equals(worldPreset)) {
                    entry.getValue().onChunkLoad(serverLevel, chunk, newChunk);
                }
            }
        }
    }

    public static void onPhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        final ServerLevel level = physicsSystem.getLevel();
        RedstoneMagnetBlockEntity.GLOBAL_REDSTONE_MAGNET_MAP.physicsTick(timeStep, level);
        DockingConnectorBlockEntity.MAGNET_CONTROLLER.physicsTick(timeStep, level);
        EndSeaPhysicsData.physicsTick(timeStep, level);
        ServerLevelRopeManager.getOrCreate(level).physicsTick(physicsSystem, timeStep);

        LaunchedPlungerServerHandler.physicsTickAllPlungers(physicsSystem, timeStep);
        PhysicsStaffServerHandler.get(level).physicsTick(physicsSystem);
    }

    public static @Nullable InteractionResult rightClickBlock(final Level level, final BlockPos pos, final Player player, final ItemStack useStack) {
        if (SimBlocks.SPRING.has(level.getBlockState(pos)) && useStack.is(SimTags.Items.SPRING_ADJUSTER)) {
            if (SpringBlock.tryAdjustSpring(level, pos, player)) {
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.FAIL;
            }
        }
        return null;
    }

    public static void onPostPhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        DiagramEntity.postPhysicsTick(physicsSystem.getLevel());
    }

    private static void onContainerReady(final Level level, final SubLevelContainer subLevelContainer) {
        if (!(subLevelContainer instanceof final ServerSubLevelContainer serverContainer)) {
            return;
        }

        serverContainer.addObserver(new PhysicsStaffSubLevelObserver(serverContainer.getLevel()));
        
        final SubLevelTrackingSystem trackingSystem = serverContainer.trackingSystem();
        trackingSystem.addTrackingPlugin(new ServerRopeTrackingSystem(serverContainer.getLevel()));
    }

    public static void register() {
        SableEventPlatform.INSTANCE.onSubLevelContainerReady(SimulatedCommonEvents::onContainerReady);
    }

    /**
     * Upstream gives Create's Extendo Grip and Cardboard Sword punch-strength and
     * punch-cooldown modifiers, through 1.20.5 default data components and two
     * <em>Sable</em> attributes. Neither exists here: 1.20.1 has no default
     * components at all, and the attributes come with the physics engine in V2.
     *
     * <p>So the two modifiers are not applied. Nothing else in Simulated reads
     * them — they only matter to Sable's own punch handling — and this is
     * recorded in FABRIC_PORT_PLAN.md rather than dropped silently.
     */
    public static void modifyDefaultComponents(final BiConsumer<ItemLike, Consumer<Object>> modify) {
        SimulatedRegistrate.onAddDefaultComponents(modify);
    }
}
