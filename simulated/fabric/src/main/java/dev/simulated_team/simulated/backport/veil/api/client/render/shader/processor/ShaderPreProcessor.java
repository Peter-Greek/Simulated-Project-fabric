package dev.simulated_team.simulated.backport.veil.api.client.render.shader.processor;

/**
 * A source transform applied to shaders before compilation. Inert: nothing
 * compiles shaders here, so a registered pre-processor never runs.
 */
public interface ShaderPreProcessor {

    /** The context a pre-processor is handed. Members are added as needed. */
    interface Context {
    }
}
