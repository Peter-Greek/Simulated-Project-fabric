package dev.simulated_team.simulated.backport.physics.mixinterface.plot;

import dev.simulated_team.simulated.backport.physics.api.sublevel.SubLevelContainer;

import javax.annotation.Nullable;

/** Sable mixes this onto a level to reach its container. Nothing does here. */
public interface SubLevelContainerHolder {

    @Nullable
    default SubLevelContainer sable$getSubLevelContainer() {
        return null;
    }

    default SubLevelContainer sable$getPlotContainer() {
        return SubLevelContainer.getContainer((net.minecraft.world.level.Level) null);
    }
}
