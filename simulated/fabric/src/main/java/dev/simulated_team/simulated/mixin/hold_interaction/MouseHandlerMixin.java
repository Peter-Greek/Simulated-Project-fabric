package dev.simulated_team.simulated.mixin.hold_interaction;

import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    // 1.20.1's turnPlayer() takes no argument -- 1.21.1 passes it the frame time --
    // and the two turn deltas sit at different local ordinals as a result. At the
    // call to LocalPlayer#turn the live doubles are, by slot: the timestamp, the
    // frame delta, the two deltas, and the three sensitivity terms, so the deltas
    // are ordinals 2 and 3 here rather than 4 and 5.
    @Inject(method = "turnPlayer", cancellable = true,
            at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void simulated$turnPlayer(final CallbackInfo ci,
                                      @Local(ordinal = 2) final double j, @Local(ordinal = 3) final double k,
                                      @Local(ordinal = 0) final int l) {
        if (SimDistUtil.getClientPlayer() != null && !SimDistUtil.getClientPlayer().isSpectator()) {
            final InteractCallback.Result status = SimulatedCommonClientEvents.onMouseMove(j, k * l);
            if (status.cancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onPress",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;", ordinal = 0),
            cancellable = true)
    private void simulated$preOnPress(final long windowPointer, final int button, final int action, final int modifiers, final CallbackInfo ci, @Local(ordinal = 1, argsOnly = true) final int i, @Local(argsOnly = true, ordinal = 0) final long l) {
        if (SimDistUtil.getClientPlayer() != null && !SimDistUtil.getClientPlayer().isSpectator()) {
            final InteractCallback.Result status = SimulatedCommonClientEvents.onBeforeMouseInput(InteractCallback.Input.mouse(button), modifiers, action);
            if (status.cancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;", ordinal = 0),
            cancellable = true)
    // 1.20.1 scrolls on one axis only: onScroll scales the vertical offset by the
    // sensitivity option into a single local, and never reads the horizontal one.
    // 1.21.1 scales both, which is where upstream's ordinals 3 and 4 come from.
    // The horizontal delta is reported as zero, as SimulatedCommonClientEvents
    // already does for the keyboard scroll path.
    private void simulated$preOnScroll(final long l, final double d, final double e, final CallbackInfo ci, @Local(ordinal = 2) final double deltaY) {
        if (SimDistUtil.getClientPlayer() != null && !SimDistUtil.getClientPlayer().isSpectator()) {
            final InteractCallback.Result status = SimulatedCommonClientEvents.onMouseScroll(0, deltaY);
            if (status.cancelled()) {
                ci.cancel();
            }
        }
    }
}