package dev.simulated_team.simulated.content.end_sea;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Draws the End Sea — the void surface, its fog, and the fade over it.
 *
 * <p><b>Not ported.</b> Upstream draws it with a Veil-compiled shader, reading
 * Veil's depth framebuffer and its camera uniforms. This port replaces Veil
 * rather than backporting it, and none of that stack has a 1.20.1 equivalent
 * that takes upstream's shader sources, so the sea is not drawn.
 *
 * <p>The End Sea biome, its world preset, and the physics data that goes with it
 * are all ported; what is missing is the visual. Recorded in
 * FABRIC_PORT_PLAN.md, and scheduled with the rest of the renderer work.
 */
public class EndSeaRenderer {

    public static void tick() {
    }

    public static void render(final Camera camera, final GameRenderer gameRenderer) {
    }
}
