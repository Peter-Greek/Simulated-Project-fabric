package dev.simulated_team.simulated.network;

import dev.simulated_team.simulated.util.SimReach;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared checks performed before any client-selected world position is read. */
public final class PacketValidation {
    private PacketValidation() {}

    public static boolean canInteract(Player player, BlockPos pos) {
        return canInteract(player, pos, SimReach.blockInteractionRange(player) + 2);
    }

    public static boolean canInteract(Player player, BlockPos pos, double range) {
        return player.isAlive() && !player.isSpectator() && player.mayBuild()
                && player.level().isLoaded(pos) && !player.level().isOutsideBuildHeight(pos)
                && player.level().getWorldBorder().isWithinBounds(pos)
                && player.level().mayInteract(player, pos)
                && player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= range * range;
    }

    public static boolean canInteract(Player player, Entity entity) {
        return entity != null && !entity.isRemoved() && entity.level() == player.level()
                && canInteract(player, entity.blockPosition());
    }

    public static boolean finite(AABB bounds) {
        return Double.isFinite(bounds.minX) && Double.isFinite(bounds.minY) && Double.isFinite(bounds.minZ)
                && Double.isFinite(bounds.maxX) && Double.isFinite(bounds.maxY) && Double.isFinite(bounds.maxZ);
    }

    public static boolean validKey(int key) {
        return key >= 32 && key <= 348;
    }
}
