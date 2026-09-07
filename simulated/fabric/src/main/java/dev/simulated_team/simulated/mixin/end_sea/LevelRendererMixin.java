package dev.simulated_team.simulated.mixin.end_sea;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.content.end_sea.EndSeaRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The point in the frame where the End Sea is drawn.
 *
 * <p><b>Ported by hand, not by {@code tools/port_upstream.py}.</b> Upstream's
 * callback takes 1.21's {@code renderLevel(DeltaTracker, …)} parameters; 1.20.1
 * passes a {@link PoseStack}, a partial tick and a deadline instead. The
 * injection point — vanilla's own {@code renderDebug} call — is the same one
 * upstream uses, so the seam sits in the same place in the frame.
 *
 * <p>{@link EndSeaRenderer#render} is a no-op on this stack; see its javadoc.
 * The hook is kept so the renderer has somewhere to land.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug("
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                            + "Lnet/minecraft/client/Camera;)V"))
    public void renderLevel(final PoseStack poseStack, final float partialTick, final long finishNanoTime,
                            final boolean renderBlockOutline, final Camera camera,
                            final GameRenderer gameRenderer, final LightTexture lightTexture,
                            final Matrix4f projectionMatrix, final CallbackInfo ci) {
        EndSeaRenderer.render(camera, gameRenderer);
    }
}
