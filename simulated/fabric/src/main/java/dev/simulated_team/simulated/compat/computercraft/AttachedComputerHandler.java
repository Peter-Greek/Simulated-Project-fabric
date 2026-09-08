package dev.simulated_team.simulated.compat.computercraft;

import dan200.computercraft.api.peripheral.IComputerAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The computers attached to one peripheral.
 *
 * <p>Upstream uses ComputerCraft's own {@code AttachedComputerSet}, which its
 * 1.20.1 API does not have. The set is kept here instead — a peripheral is
 * attached and detached from the game thread, and events are queued to every
 * attached computer, which is all that class does.
 */
public class AttachedComputerHandler {

    private final Set<IComputerAccess> attachedComputers =
            Collections.newSetFromMap(new WeakHashMap<>());

    public void attach(final IComputerAccess computer) {
        this.attachedComputers.add(computer);
    }

    public void detach(final IComputerAccess computer) {
        this.attachedComputers.remove(computer);
    }

    public void queueEvent(final String event, @Nullable final Object... args) {
        for (final IComputerAccess computer : this.attachedComputers) {
            computer.queueEvent(event, args);
        }
    }
}
