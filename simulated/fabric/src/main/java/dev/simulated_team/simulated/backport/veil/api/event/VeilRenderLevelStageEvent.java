package dev.simulated_team.simulated.backport.veil.api.event;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import dev.simulated_team.simulated.backport.client.DeltaTracker;
import dev.simulated_team.simulated.backport.veil.api.client.render.MatrixStack;
import org.joml.Matrix4fc;

/**
 * The point in the level render at which a listener draws.
 *
 * <p>The stages Simulated uses map onto Fabric's own {@code WorldRenderEvents};
 * see {@link dev.simulated_team.simulated.backport.veil.platform.VeilEventPlatform},
 * which is what actually fires them.
 */
@FunctionalInterface
public interface VeilRenderLevelStageEvent {

    void render(Stage stage, LevelRenderer levelRenderer, MultiBufferSource.BufferSource bufferSource,
                MatrixStack matrixStack, Matrix4fc frustumMatrix, Matrix4fc projectionMatrix,
                int renderTick, DeltaTracker deltaTracker, Camera camera, Frustum frustum);

    enum Stage {
        AFTER_SKY,
        AFTER_SOLID_BLOCKS,
        AFTER_CUTOUT_MIPPED_BLOCKS,
        AFTER_CUTOUT_BLOCKS,
        AFTER_ENTITIES,
        AFTER_BLOCK_ENTITIES,
        AFTER_TRANSLUCENT_BLOCKS,
        AFTER_PARTICLES,
        AFTER_WEATHER,
        AFTER_LEVEL
    }
}
