package dev.simulated_team.simulated.fabric.service;

import com.tterrag.registrate.builders.EntityBuilder;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.service.SimEntityService;
import dev.simulated_team.simulated.util.SimReach;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Entity odds and ends that differ per loader.
 *
 * <p>Two deviations, both recorded in FABRIC_PORT_PLAN.md: persistent entity
 * data is Porting Lib's rather than NeoForge's, and there is no fake-player
 * marker on this stack — Fabric has no equivalent of {@code isFakePlayer}, so a
 * machine acting as a player is treated as a real one.
 */
public class FabricSimEntityService implements SimEntityService {

    @Override
    public CompoundTag getCustomData(final Entity entity) {
        return ((io.github.fabricators_of_create.porting_lib.entity.extensions.EntityExtensions) entity)
                .getCustomData();
    }

    @Override
    public double getPlayerReach(final Player player) {
        return SimReach.blockInteractionRange(player);
    }

    @Override
    public boolean isFake(final Player player) {
        return false;
    }

    @Override
    public <T extends Entity, P> EntityBuilder<T, P> loaderEntityTransform(
            final EntityBuilder<T, P> builder, final SimEntityTypes.EntityLoaderData data) {
        return builder.properties(p -> {
            if (data.immuneToFire()) {
                p.fireImmune();
            }

            p.trackRangeBlocks(data.clientTrackingRange());
            p.trackedUpdateRate(data.updateFrequency());
            p.dimensions(net.minecraft.world.entity.EntityDimensions.scalable(data.width(), data.height()));
            p.forceTrackedVelocityUpdates(data.sendVelocity());
        });
    }
}
