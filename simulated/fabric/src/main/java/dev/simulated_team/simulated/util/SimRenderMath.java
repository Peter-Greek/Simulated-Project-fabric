package dev.simulated_team.simulated.util;

import net.minecraft.client.Minecraft;

/**
 * Render-side numbers upstream reads out of vanilla classes that keep them
 * private on 1.20.1.
 */
public final class SimRenderMath {

    private SimRenderMath() {
    }

    /**
     * The camera's vertical field of view, in degrees — the same value upstream
     * reads, through the access widener that opens {@code GameRenderer#getFov}.
     */
    public static double fieldOfView() {
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft.gameRenderer.getFov(minecraft.gameRenderer.getMainCamera(),
                net.createmod.catnip.animation.AnimationTickHolder.getPartialTicks(), true);
    }
}
