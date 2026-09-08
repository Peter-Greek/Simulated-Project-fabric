package dev.simulated_team.simulated.content.end_sea;

import dev.simulated_team.simulated.backport.veil.api.client.render.shader.processor.ShaderPreProcessor;

/**
 * Injects the End Sea's distance fade into every shader Veil compiles.
 *
 * <p><b>Not ported.</b> It rewrites GLSL through Veil's shader pre-processor and
 * Ocelot's GLSL parser, neither of which is on this stack. Registration still
 * happens so the wiring reads as upstream's; nothing calls back. See
 * {@link EndSeaRenderer} and FABRIC_PORT_PLAN.md.
 */
public class EndSeaFadeTransformer implements ShaderPreProcessor {
}
