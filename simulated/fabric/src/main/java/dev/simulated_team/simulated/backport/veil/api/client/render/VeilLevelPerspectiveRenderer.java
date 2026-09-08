package dev.simulated_team.simulated.backport.veil.api.client.render;

/**
 * Whether the level is currently being drawn from a secondary camera.
 *
 * <p><b>Inert.</b> Nothing here renders a second perspective, so the answer is
 * always no and callers take their normal path.
 */
public final class VeilLevelPerspectiveRenderer {

    private VeilLevelPerspectiveRenderer() {
    }

    public static boolean isRenderingPerspective() {
        return false;
    }
}
