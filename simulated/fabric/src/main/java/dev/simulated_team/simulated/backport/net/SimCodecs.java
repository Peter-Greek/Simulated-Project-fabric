package dev.simulated_team.simulated.backport.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * The stream codecs vanilla hangs off its own types as static fields from
 * 1.20.5 onward — {@code BlockPos.STREAM_CODEC}, {@code UUIDUtil.STREAM_CODEC},
 * {@code Direction.STREAM_CODEC}, {@code ResourceLocation.STREAM_CODEC}.
 *
 * <p>A shim cannot add fields to a vanilla class, so those four references are
 * rewritten to point here as ported files come across. The wire shape is
 * vanilla's, so a 1.20.1 client and a 1.21.1 client agree on the bytes.
 */
public final class SimCodecs {

    private SimCodecs() {
    }

    /** Replaces {@code BlockPos.STREAM_CODEC}. Vanilla packs a position into one long. */
    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = StreamCodec.of(
            (buf, value) -> buf.writeLong(value.asLong()),
            buf -> BlockPos.of(buf.readLong()));

    /** Replaces {@code UUIDUtil.STREAM_CODEC}. */
    public static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeLong(value.getMostSignificantBits());
                buf.writeLong(value.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong()));

    /** Replaces {@code Direction.STREAM_CODEC}. Vanilla writes the ordinal as a var-int. */
    public static final StreamCodec<ByteBuf, Direction> DIRECTION = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeVarInt(value.ordinal()),
            buf -> Direction.values()[new FriendlyByteBuf(buf).readVarInt()]);

    /**
     * Replaces {@code ItemStack.OPTIONAL_STREAM_CODEC}. 1.20.1's buffer already
     * round-trips an empty stack, so the plain read and write cover both.
     */
    public static final StreamCodec<ByteBuf, net.minecraft.world.item.ItemStack> ITEM_STACK = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeItem(value),
            buf -> new FriendlyByteBuf(buf).readItem());

    /** Replaces {@code ResourceLocation.STREAM_CODEC}. */
    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION = StreamCodec.of(
            (buf, value) -> new FriendlyByteBuf(buf).writeResourceLocation(value),
            buf -> new FriendlyByteBuf(buf).readResourceLocation());

    /**
     * Replaces {@code ResourceKey.streamCodec(Registries.DIMENSION)}. A
     * dimension key is written as its id, which is what vanilla does.
     */
    public static <T> StreamCodec<ByteBuf, net.minecraft.resources.ResourceKey<T>> resourceKey(
            final net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registry) {
        return RESOURCE_LOCATION.map(
                id -> net.minecraft.resources.ResourceKey.create(registry, id),
                net.minecraft.resources.ResourceKey::location);
    }

    public static StreamCodec<ByteBuf, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> dimension() {
        return resourceKey(net.minecraft.core.registries.Registries.DIMENSION);
    }

    /** As {@link #pair}, for Catnip's own pair, which Simulated's handlers use. */
    public static <B, L, R> StreamCodec<B, net.createmod.catnip.data.Pair<L, R>> catnipPair(
            final StreamCodec<? super B, L> left, final StreamCodec<? super B, R> right) {
        return StreamCodec.of(
                (buf, value) -> {
                    left.encode(buf, value.getFirst());
                    right.encode(buf, value.getSecond());
                },
                buf -> net.createmod.catnip.data.Pair.of(left.decode(buf), right.decode(buf)));
    }

    /** Replaces {@code Pair.streamCodec}, which arrived with the codec API. */
    public static <B, L, R> StreamCodec<B, com.mojang.datafixers.util.Pair<L, R>> pair(
            final StreamCodec<? super B, L> left, final StreamCodec<? super B, R> right) {
        return StreamCodec.of(
                (buf, value) -> {
                    left.encode(buf, value.getFirst());
                    right.encode(buf, value.getSecond());
                },
                buf -> com.mojang.datafixers.util.Pair.of(left.decode(buf), right.decode(buf)));
    }

}
