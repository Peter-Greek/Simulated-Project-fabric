package dev.simulated_team.simulated.backport.physics.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Sable's side-safe accessors. The client-level lookup is real; it is ordinary
 * Minecraft, not physics.
 */
public final class SableDistUtil {

    private SableDistUtil() {
    }

    @Nullable
    public static Level getClientLevel() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return null;
        }
        return ClientAccess.level();
    }

    /** Nested so a dedicated server never loads {@code net.minecraft.client}. */
    private static final class ClientAccess {
        @Nullable
        static Level level() {
            return net.minecraft.client.Minecraft.getInstance().level;
        }
    }
}
