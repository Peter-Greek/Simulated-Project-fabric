package dev.simulated_team.simulated.backport.veil.api.client.render;

import org.joml.Matrix4f;

/** See {@link VeilRenderSystem}: inert. Identity matrices. */
public class CameraMatrices {

    public Matrix4f getProjectionMatrix() {
        return new Matrix4f();
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f();
    }
}
