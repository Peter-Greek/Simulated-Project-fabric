package dev.simulated_team.simulated.backport.veil.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Veil's own transform stack. It is a {@link PoseStack} with a few extra
 * conveniences; Simulated only ever pushes, pops, translates, and reads the top
 * entry, so this wraps a real {@link PoseStack} and forwards.
 */
public class MatrixStack {

    private final PoseStack delegate;

    public MatrixStack() {
        this(new PoseStack());
    }

    public MatrixStack(final PoseStack delegate) {
        this.delegate = delegate;
    }

    public void pushPose() {
        this.delegate.pushPose();
    }

    public void popPose() {
        this.delegate.popPose();
    }

    public void translate(final double x, final double y, final double z) {
        this.delegate.translate(x, y, z);
    }

    public PoseStack.Pose last() {
        return this.delegate.last();
    }

    /** Veil's own names for push and pop. */
    public void matrixPush() {
        this.delegate.pushPose();
    }

    public void matrixPop() {
        this.delegate.popPose();
    }

    public PoseStack toPoseStack() {
        return this.delegate;
    }

    public void rotate(final org.joml.Quaternionf rotation) {
        this.delegate.mulPose(rotation);
    }
}
