package dev.simulated_team.simulated.backport.veil.api.client.render.framebuffer;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/** See {@link AdvancedFbo}: inert. No framebuffer is ever registered. */
public class FramebufferManager {

    @Nullable
    public AdvancedFbo getFramebuffer(final ResourceLocation name) {
        return null;
    }
}
