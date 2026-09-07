package dev.simulated_team.simulated.util;

import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;
import dev.simulated_team.simulated.backport.veil.api.client.render.framebuffer.AdvancedFbo;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Vector3dc;

/**
 * Draws a group of sub-levels into an off-screen buffer, which is how the
 * Contraption Diagram shows a preview and how the End Sea casts its shadow.
 *
 * <p>Both halves are absent here: there are no sub-levels to draw, and no
 * off-screen buffer to draw them into — see the backport packages. So these draw
 * nothing, and the two features that use them show an empty view. Recorded in
 * FABRIC_PORT_PLAN.md; they come back with the renderer in V2.
 */
public final class SimpleSubLevelGroupRenderer {

    private SimpleSubLevelGroupRenderer() {
    }

    public static void renderGroup(final Level level, final Iterable<? extends SubLevel> group,
                                   final AdvancedFbo target, final Matrix4f modelView, final Matrix4f projection,
                                   final Vector3dc cameraPosition, final Quaterniond orientation,
                                   final float scale, final boolean shadow) {
    }

    public static void renderChain(final SubLevel subLevel, final AdvancedFbo target, final Matrix4f modelView,
                                   final Matrix4f projection, final Vector3dc cameraPosition,
                                   final Quaterniond orientation, final float partialTicks) {
    }
}
