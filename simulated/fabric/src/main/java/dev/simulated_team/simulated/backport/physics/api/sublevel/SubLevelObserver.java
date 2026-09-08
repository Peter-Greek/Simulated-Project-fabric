package dev.simulated_team.simulated.backport.physics.api.sublevel;

import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;

/** Notified as sub-levels come and go. Never called: none are created. */
public interface SubLevelObserver {

    default void onSubLevelAdded(final SubLevel subLevel) {
    }

    default void onSubLevelRemoved(final SubLevel subLevel,
                                   final dev.simulated_team.simulated.backport.physics.sublevel.storage
                                           .SubLevelRemovalReason reason) {
    }

    /** Called each tick with the container the observer watches. */
    default void tick(final dev.simulated_team.simulated.backport.physics.api.sublevel.SubLevelContainer subLevels) {
    }
}
