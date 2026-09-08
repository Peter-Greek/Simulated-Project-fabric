package dev.simulated_team.simulated.backport.physics.api.physics.mass;

import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/** The live {@link MassData} for one body. Empty here; see the package docs. */
public class MassTracker implements MassData {

    @Override
    public double getMass() {
        return 0.0;
    }

    @Override
    public double getInverseMass() {
        return 0.0;
    }

    @Override
    public double getInverseNormalMass(final Vector3dc point, final Vector3dc normal) {
        return 0.0;
    }

    @Override
    public Vector3dc getCenterOfMass() {
        return new Vector3d();
    }

    @Override
    public Matrix3d getInertiaTensor() {
        return new Matrix3d().zero();
    }

    @Override
    public Matrix3d getInverseInertiaTensor() {
        return new Matrix3d().zero();
    }
}
