package dev.simulated_team.simulated.backport.physics.api.sublevel;

import dev.simulated_team.simulated.backport.physics.sublevel.ClientSubLevel;
import net.minecraft.client.multiplayer.ClientLevel;

import javax.annotation.Nullable;
import java.util.UUID;

/** Client half of {@link SubLevelContainer}; holds no sub-levels here. */
public class ClientSubLevelContainer extends SubLevelContainer {

    private static final ClientSubLevelContainer EMPTY = new ClientSubLevelContainer();

    protected ClientSubLevelContainer() {
    }

    public static ClientSubLevelContainer getContainer(final ClientLevel level) {
        return EMPTY;
    }

    public static ClientSubLevelContainer getContainer(final net.minecraft.world.level.Level level) {
        return EMPTY;
    }

    @Nullable
    @Override
    public ClientSubLevel getSubLevel(final UUID id) {
        return null;
    }
}
