package dev.simulated_team.simulated.mixin.hold_interaction;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import dev.simulated_team.simulated.index.SimClickInteractions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fades the goggle overlay out while the player is holding a steering wheel, so
 * it does not sit over the helm.
 *
 * <p><b>Ported by hand, not by {@code tools/port_upstream.py}.</b> Create's
 * {@code renderOverlay} takes {@code (GuiGraphics, DeltaTracker, int, int)}
 * upstream and {@code (GuiGraphics, float partialTicks, int, int)} here, so the
 * partial tick is read straight off the argument rather than out of a tracker.
 * The anchor — the single {@code Mth.clamp(float, float, float)} that computes
 * the overlay's fade — is the same call in both versions.
 */
@Mixin(GoggleOverlayRenderer.class)
public class GoggleOverlayRendererMixin {
    @Shadow public static int hoverTicks;

    @Inject(method = "renderOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", shift = At.Shift.BEFORE))
    private static void decrementRenderTicks(final CallbackInfo ci) {
        if (SimClickInteractions.STEERING_WHEEL_MANAGER.isActive()) {
            hoverTicks = Mth.clamp(hoverTicks - 2, 0, 24);
        }
    }

    @WrapOperation(method = "renderOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F"))
    private static float fixPartialTicks(final float value, final float min, final float max,
                                         final Operation<Float> original,
                                         @Local(argsOnly = true) final float partialTicks) {
        if (SimClickInteractions.STEERING_WHEEL_MANAGER.isActive()) {
            return Mth.clamp(hoverTicks - partialTicks, 0, 24) / 24;
        }
        return original.call(value, min, max);
    }

    @Inject(method = "renderOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F"), cancellable = true)
    private static void dontRenderTheText(final GuiGraphics guiGraphics, final float partialTicks,
                                          final int width, final int height, final CallbackInfo ci) {
        if (hoverTicks - partialTicks <= 0) {
            ci.cancel();
        }
    }
}
