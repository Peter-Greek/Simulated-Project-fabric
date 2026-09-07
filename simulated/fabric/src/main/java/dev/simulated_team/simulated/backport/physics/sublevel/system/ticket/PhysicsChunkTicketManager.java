package dev.simulated_team.simulated.backport.physics.sublevel.system.ticket;

/** Keeps chunks loaded around a simulated body. Nothing to keep loaded here. */
public class PhysicsChunkTicketManager {

    /** Registers a simulated object so its chunks stay loaded. Nothing simulates. */
    public void addObject(@javax.annotation.Nullable final Object object) {
    }

    public void removeObject(@javax.annotation.Nullable final Object object) {
    }

    /** Whether a chunk is loaded enough to simulate in. Nothing simulates here. */
    public static boolean isChunkLoadedEnough(final net.minecraft.server.level.ServerLevel level,
                                              final int chunkX, final int chunkZ) {
        return false;
    }

    /** Whether an object's chunks would stay loaded. Nothing keeps any loaded. */
    public boolean wouldBeLoaded(final net.minecraft.server.level.ServerLevel level,
                                 @javax.annotation.Nullable final Object object) {
        return false;
    }
}
