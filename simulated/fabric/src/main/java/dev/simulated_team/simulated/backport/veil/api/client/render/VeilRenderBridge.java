package dev.simulated_team.simulated.backport.veil.api.client.render;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;

/**
 * Bridges a Veil-compiled shader into a vanilla render type.
 *
 * <p><b>Reduced.</b> Veil compiles shaders from the resource pack and hands back
 * a state shard bound to the compiled program. There is no Veil here, and no
 * equivalent on this stack that can take Veil's shader sources, so this returns
 * vanilla's position–colour–texture–lightmap shader instead. Geometry, colour,
 * and texturing are upstream's; the bespoke effects those shaders provide — the
 * laser glow, the lens, the staff overlay, the rope and spring shading — are
 * not. Replacing them is recorded in FABRIC_PORT_PLAN.md with the rest of the
 * renderer work.
 */
public final class VeilRenderBridge extends RenderStateShard {

    /**
     * {@code ShaderStateShard} is protected in {@link RenderStateShard}, so this
     * class extends it purely to be able to construct one, the same way
     * vanilla's own render types do.
     */
    private VeilRenderBridge() {
        super("simulated_veil_bridge", () -> {
        }, () -> {
        });
    }

    public static RenderStateShard.ShaderStateShard shaderState(final ResourceLocation shader) {
        return new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentShader);
    }
}
