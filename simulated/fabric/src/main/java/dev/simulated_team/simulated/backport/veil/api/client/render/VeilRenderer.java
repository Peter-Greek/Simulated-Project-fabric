package dev.simulated_team.simulated.backport.veil.api.client.render;

import dev.simulated_team.simulated.backport.veil.api.client.render.framebuffer.FramebufferManager;
import dev.simulated_team.simulated.backport.veil.api.client.render.post.PostProcessingManager;

/** See {@link VeilRenderSystem}: inert. */
public class VeilRenderer {

    private final FramebufferManager framebufferManager = new FramebufferManager();
    private final PostProcessingManager postProcessingManager = new PostProcessingManager();
    private final DynamicBufferManager dynamicBufferManager = new DynamicBufferManager();

    public CameraMatrices getCameraMatrices() {
        return new CameraMatrices();
    }

    public FramebufferManager getFramebufferManager() {
        return this.framebufferManager;
    }

    public PostProcessingManager getPostProcessingManager() {
        return this.postProcessingManager;
    }

    public DynamicBufferManager getDynamicBufferManger() {
        return this.dynamicBufferManager;
    }
}
