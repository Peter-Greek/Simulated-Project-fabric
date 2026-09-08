package dev.simulated_team.simulated.backport.physics.api.block;

import dev.simulated_team.simulated.backport.physics.api.physics.handle.RigidBodyHandle;
import dev.simulated_team.simulated.backport.physics.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;

import javax.annotation.Nullable;

/**
 * A block entity that acts on the body it rides — reading its pose, pushing on
 * it, or holding a constraint to another body.
 *
 * <p><b>Inert.</b> Nothing steps a body here, so {@link #sable$physicsTick} is
 * never called and the dependencies a block declares are never walked. The hooks
 * stay so upstream's block entities read as written and start working the moment
 * the engine lands in V2.
 */
public interface BlockEntitySubLevelActor {

    /** Called once per physics step for the body this block entity rides. */
    default void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
    }

    /**
     * Other bodies this one must be solved alongside, because a constraint joins
     * them — a docking connector to its partner, a spring to its far end.
     */
    @Nullable
    default Iterable<SubLevel> sable$getConnectionDependencies() {
        return null;
    }
}
