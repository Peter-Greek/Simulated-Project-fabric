package dev.simulated_team.simulated.backport.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Stand-in for {@code net.minecraft.network.RegistryFriendlyByteBuf} (1.20.5+).
 *
 * <p>Sable uses it almost entirely as a type parameter. The one place it reaches
 * for the registry access is the UDP server, and its two other construction
 * sites pass {@code null} for it — so a plain {@link FriendlyByteBuf} carrying an
 * optional {@link RegistryAccess} reproduces every use.
 */
public class RegistryFriendlyByteBuf extends FriendlyByteBuf {

    private final RegistryAccess registryAccess;

    public RegistryFriendlyByteBuf(final ByteBuf source, final RegistryAccess registryAccess) {
        super(source);
        this.registryAccess = registryAccess;
    }

    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }
}
