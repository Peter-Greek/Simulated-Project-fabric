package dev.simulated_team.simulated.backport.physics.api.physics.object.rope;

/** A live rope in the solver. Never created here. */
public class RopeHandle {

    /** Which end of a rope an attachment holds. */
    public enum AttachmentPoint {
        START,
        END
    }

    public boolean isValid() {
        return false;
    }

    public void remove() {
    }

    /** Ties a rope end to a body. Nothing is tied while there is no solver. */
    public void attach(final AttachmentPoint point,
                       final dev.simulated_team.simulated.backport.physics.sublevel.SubLevel subLevel,
                       final org.joml.Vector3dc localAnchor) {
    }

    public void detach(final AttachmentPoint point) {
    }
}
