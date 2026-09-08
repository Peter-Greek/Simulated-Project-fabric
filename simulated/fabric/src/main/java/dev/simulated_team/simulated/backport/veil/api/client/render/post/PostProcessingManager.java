package dev.simulated_team.simulated.backport.veil.api.client.render.post;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/** See {@link PostPipeline}: inert. Nothing is registered, nothing runs. */
public class PostProcessingManager {

    @Nullable
    public PostPipeline getPipeline(final ResourceLocation name) {
        return null;
    }

    public void runPipeline(final PostPipeline pipeline) {
    }

    public void runPipeline(final PostPipeline pipeline, final PostPipeline.Context context) {
    }

    public void runPipeline(final PostPipeline pipeline, final boolean clear) {
    }

    public PostPipeline.Context getPostPipelineContext() {
        return new PostPipeline.Context() {
        };
    }

    public void add(final ResourceLocation name, final int priority) {
    }

    public void remove(final ResourceLocation name) {
    }
}
