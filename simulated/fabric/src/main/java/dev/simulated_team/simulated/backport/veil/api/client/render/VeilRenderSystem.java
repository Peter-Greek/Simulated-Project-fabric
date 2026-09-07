package dev.simulated_team.simulated.backport.veil.api.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import java.util.function.Supplier;

/**
 * Veil's renderer entry point.
 *
 * <p><b>Inert.</b> Everything reachable through {@link #renderer()} is part of
 * the deferred rendering stack, which this port does not have. Callers get an
 * object that reports nothing configured, and take their fallback path.
 */
public final class VeilRenderSystem {

    private static final VeilRenderer RENDERER = new VeilRenderer();

    private VeilRenderSystem() {
    }

    public static VeilRenderer renderer() {
        return RENDERER;
    }

    public static void setShader(final Supplier<ShaderInstance> shader) {
    }

    public static void setShader(final net.minecraft.resources.ResourceLocation shader) {
    }
}
