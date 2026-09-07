package dev.simulated_team.simulated.backport.veil.api.client.render.framebuffer;

import javax.annotation.Nullable;

/**
 * An off-screen render target.
 *
 * <p><b>Inert.</b> The builder accepts every call and produces a framebuffer
 * that binds to nothing, so a renderer that draws into one draws nowhere. The
 * features that depend on it — the End Sea shadow pass and the Contraption
 * Diagram's rendered preview — are recorded in FABRIC_PORT_PLAN.md.
 */
public class AdvancedFbo {

    public static Builder withSize(final int width, final int height) {
        return new Builder();
    }

    public static void unbind() {
    }

    public void bind(final boolean setViewport) {
    }

    public void bindRead() {
    }

    public int getId() {
        return 0;
    }

    @Nullable
    public Object getDepthTextureAttachment() {
        return null;
    }

    public void clear() {
    }

    public void free() {
    }

    @Nullable
    public Object getColorTextureAttachment(final int index) {
        return null;
    }

    public int getWidth() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }

    public static class Builder {

        public Builder addColorTextureBuffer() {
            return this;
        }

        public Builder setDepthTextureBuffer() {
            return this;
        }

        public Builder setDepthRenderBuffer() {
            return this;
        }

        public AdvancedFbo build(final boolean setup) {
            return new AdvancedFbo();
        }
    }
}
