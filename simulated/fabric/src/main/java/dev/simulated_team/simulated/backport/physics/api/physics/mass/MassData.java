package dev.simulated_team.simulated.backport.physics.api.physics.mass;

import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * What the solver knows about a body's mass distribution. Nothing has mass here
 * — see the package documentation — so every reading is zero and upstream's
 * "no body" guards fire.
 */
public interface MassData {

    double getMass();

    double getInverseMass();

    double getInverseNormalMass(Vector3dc point, Vector3dc normal);

    Vector3dc getCenterOfMass();

    Matrix3d getInertiaTensor();

    Matrix3d getInverseInertiaTensor();

    /** A body with no mass: the only kind on this stack. */
    MassData EMPTY = new MassData() {
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
    };
}
