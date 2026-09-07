package dev.simulated_team.simulated.content.end_sea;

import dev.simulated_team.simulated.backport.client.DeltaTracker;
import dev.simulated_team.simulated.backport.veil.api.client.render.MatrixStack;
import dev.simulated_team.simulated.backport.veil.api.client.render.framebuffer.AdvancedFbo;
import dev.simulated_team.simulated.backport.veil.api.event.VeilRenderLevelStageEvent;
import dev.simulated_team.simulated.content.blocks.void_anchor.VoidAnchorBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * The shadow contraptions cast down onto the End Sea.
 *
 * <p><b>Not ported</b>, for the same reason as {@link EndSeaRenderer}: it renders
 * a shadow map into a Veil framebuffer and applies it through a Veil
 * post-processing pipeline. {@link #isEnabled()} therefore reports false, and
 * every caller — including the Void Anchor's renderer — takes its no-shadow
 * path, which is the same path a player without the shader option takes
 * upstream.
 */
public class EndSeaShadowRenderer {

    public static final float SHADOW_VOLUME_RADIUS = 256f / 2f;

    private static final Vector3d LAST_RENDER_ORIGIN = new Vector3d();

    public static boolean isEnabled() {
        return false;
    }

    public static void renderShadowMap(final VeilRenderLevelStageEvent.Stage stage,
                                       final LevelRenderer levelRenderer,
                                       final MultiBufferSource.BufferSource bufferSource,
                                       final MatrixStack matrixStack, final Matrix4fc frustumMatrix,
                                       final Matrix4fc projectionMatrix, final int renderTick,
                                       final DeltaTracker deltaTracker, final Camera camera,
                                       final Frustum frustum) {
    }

    public static void renderVoidAnchors(final Camera camera) {
    }

    public static @Nullable AdvancedFbo getShadowsFramebuffer() {
        return null;
    }

    public static boolean renderingShadowMap() {
        return false;
    }

    public static Vector3dc getLastRenderOrigin() {
        return LAST_RENDER_ORIGIN;
    }

    public static void addVoidAnchor(final VoidAnchorBlockEntity voidAnchor) {
    }
}
