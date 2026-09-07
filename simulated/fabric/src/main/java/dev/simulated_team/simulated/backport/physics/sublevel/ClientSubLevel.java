package dev.simulated_team.simulated.backport.physics.sublevel;

import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Stand-in for Sable's {@code ClientSubLevel}. Nothing on this stack constructs
 * one — see the package documentation — so this exists to keep upstream's
 * declarations compiling until V2 brings the engine in.
 */
public class ClientSubLevel extends SubLevel {

    protected ClientSubLevel(final ClientLevel level) {
        super(level);
    }
}
