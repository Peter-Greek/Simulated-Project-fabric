package dev.simulated_team.simulated.content.blocks.physics_assembler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Persistable metadata for a scanned assembly. The actual block set is always
 * rebuilt before a future Sable handoff so stale world data is never trusted.
 */
public record PreparedAssembly(int blockCount,
                               BlockPos min,
                               BlockPos max,
                               BlockPos startPos,
                               Direction stickyFacing,
                               long signature) {
    private static final long MIX_A = 0x9E3779B97F4A7C15L;
    private static final long MIX_B = 0xC2B2AE3D27D4EB4FL;

    public static PreparedAssembly capture(final Level level,
                                           final BlockPos startPos,
                                           final Direction stickyFacing,
                                           final FabricAssemblyScanner.ScanResult scan) {
        return new PreparedAssembly(
                scan.blocks().size(),
                scan.min().immutable(),
                scan.max().immutable(),
                startPos.immutable(),
                stickyFacing,
                fingerprint(level, scan.blocks()));
    }

    public int sizeX() {
        return max.getX() - min.getX() + 1;
    }

    public int sizeY() {
        return max.getY() - min.getY() + 1;
    }

    public int sizeZ() {
        return max.getZ() - min.getZ() + 1;
    }

    public String dimensions() {
        return sizeX() + "x" + sizeY() + "x" + sizeZ();
    }

    public CompoundTag write() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("BlockCount", blockCount);
        tag.putLong("Min", min.asLong());
        tag.putLong("Max", max.asLong());
        tag.putLong("StartPos", startPos.asLong());
        tag.putInt("StickyFacing", stickyFacing.get3DDataValue());
        tag.putLong("Signature", signature);
        return tag;
    }

    public static PreparedAssembly read(final CompoundTag tag) {
        return new PreparedAssembly(
                tag.getInt("BlockCount"),
                BlockPos.of(tag.getLong("Min")),
                BlockPos.of(tag.getLong("Max")),
                BlockPos.of(tag.getLong("StartPos")),
                Direction.from3DDataValue(tag.getInt("StickyFacing")),
                tag.getLong("Signature"));
    }

    private static long fingerprint(final Level level, final Set<BlockPos> blocks) {
        long xor = 0L;
        long sum = 0L;

        for (final BlockPos pos : blocks) {
            final long stateHash = Integer.toUnsignedLong(level.getBlockState(pos).toString().hashCode());
            long value = pos.asLong() ^ Long.rotateLeft(stateHash * MIX_A, 17);
            value ^= value >>> 33;
            value *= MIX_B;
            value ^= value >>> 29;
            xor ^= value;
            sum += value * MIX_A;
        }

        return xor ^ Long.rotateLeft(sum, 23) ^ ((long) blocks.size() * MIX_B);
    }
}
