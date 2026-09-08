package dev.simulated_team.simulated.backport.veil.api.client.render.post;

/** A post-processing pipeline. Inert; see the package documentation. */
public interface PostPipeline {

    /**
     * A uniform on the pipeline's shader. Upstream calls
     * {@code pipeline.getUniformSafe(name)}; the pipeline is null here — nothing
     * registers one — so ported call sites go through this instead and get a
     * uniform that discards every write.
     */
    static dev.simulated_team.simulated.backport.veil.api.client.render.shader.uniform.ShaderUniform uniform(
            final PostPipeline pipeline, final String name) {
        return new dev.simulated_team.simulated.backport.veil.api.client.render.shader.uniform.ShaderUniform();
    }

    /** The per-pass context a pipeline hands its stages. */
    interface Context {

        default void setFramebuffer(final net.minecraft.resources.ResourceLocation name, final Object framebuffer) {
        }
    }
}
