package dev.simulated_team.simulated.backport.physics.api.physics;

import dev.simulated_team.simulated.backport.physics.api.physics.constraint.FixedConstraintConfiguration;
import dev.simulated_team.simulated.backport.physics.api.physics.constraint.FixedConstraintHandle;
import dev.simulated_team.simulated.backport.physics.api.physics.constraint.FreeConstraintConfiguration;
import dev.simulated_team.simulated.backport.physics.api.physics.constraint.FreeConstraintHandle;
import dev.simulated_team.simulated.backport.physics.api.physics.constraint.RotaryConstraintConfiguration;
import dev.simulated_team.simulated.backport.physics.api.physics.constraint.RotaryConstraintHandle;
import dev.simulated_team.simulated.backport.physics.companion.math.Pose3dc;
import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;

import javax.annotation.Nullable;

/**
 * The solver.
 *
 * <p><b>Inert.</b> Every constraint request is declined with {@code null} —
 * upstream null-checks each one, and takes its "not constrained" path — and
 * every command to move or wake a body does nothing, because there are no
 * bodies. See the package documentation.
 */
public class PhysicsPipeline {

    @Nullable
    public FreeConstraintHandle addConstraint(@Nullable final SubLevel a, @Nullable final SubLevel b,
                                              final FreeConstraintConfiguration configuration) {
        return null;
    }

    @Nullable
    public FixedConstraintHandle addConstraint(@Nullable final SubLevel a, @Nullable final SubLevel b,
                                               final FixedConstraintConfiguration configuration) {
        return null;
    }

    @Nullable
    public RotaryConstraintHandle addConstraint(@Nullable final SubLevel a, @Nullable final SubLevel b,
                                                final RotaryConstraintConfiguration configuration) {
        return null;
    }

    public void teleport(final SubLevel subLevel, final Pose3dc pose) {
    }

    public void teleport(final SubLevel subLevel, final org.joml.Vector3dc position,
                         final org.joml.Quaterniondc orientation) {
    }

    public void wakeUp(final SubLevel subLevel) {
    }
}
