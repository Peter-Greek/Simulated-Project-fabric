package dev.simulated_team.simulated.backport.core.component;

import com.mojang.serialization.Codec;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Stand-in for {@code net.minecraft.core.component.DataComponentType} (1.20.5+).
 *
 * <p>1.20.5 replaced an item's free-form NBT with typed components. On 1.20.1
 * the NBT is all there is, so a component type here is a name plus the codec
 * that reads and writes it — which is exactly what upstream declares anyway —
 * and {@link SimComponents} does the reading and writing.
 *
 * <p>A type either lives in the mod's own compound on the stack (the default) or
 * maps onto wherever 1.20.1 already kept that data, for the vanilla components
 * in {@link DataComponents}. Which one it is is the {@link Accessor}.
 */
public class DataComponentType<T> {

    /** How a component reaches the stack it lives on. */
    public interface Accessor<T> {

        @Nullable
        T get(ItemStack stack);

        void set(ItemStack stack, T value);

        boolean has(ItemStack stack);

        void remove(ItemStack stack);
    }

    /** The one compound every mod-owned component lives inside, on the stack's tag. */
    public static final String ROOT = "simulated_components";

    private final String name;
    private final Codec<T> codec;
    private final StreamCodec<?, T> streamCodec;
    private final Accessor<T> accessor;

    DataComponentType(final String name, @Nullable final Codec<T> codec,
                      @Nullable final StreamCodec<?, T> streamCodec,
                      @Nullable final Accessor<T> accessor) {
        this.name = name;
        this.codec = codec;
        this.streamCodec = streamCodec;
        this.accessor = accessor != null ? accessor : new TagAccessor<>(name, codec);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /** Named after registration, so the storage key is stable across a world save. */
    public DataComponentType<T> named(final String name) {
        return new DataComponentType<>(name, this.codec, this.streamCodec,
                this.codec == null ? this.accessor : new TagAccessor<>(name, this.codec));
    }

    public String name() {
        return this.name;
    }

    @Nullable
    public Codec<T> codec() {
        return this.codec;
    }

    @Nullable
    public StreamCodec<?, T> streamCodec() {
        return this.streamCodec;
    }

    public Accessor<T> accessor() {
        return this.accessor;
    }

    @Override
    public String toString() {
        return "DataComponentType[" + this.name + "]";
    }

    public static class Builder<T> {

        private Codec<T> codec;
        private StreamCodec<?, T> streamCodec;
        private DataComponentType.Accessor<T> accessor;

        public Builder<T> persistent(final Codec<T> codec) {
            this.codec = codec;
            return this;
        }

        public Builder<T> networkSynchronized(final StreamCodec<?, T> streamCodec) {
            this.streamCodec = streamCodec;
            return this;
        }

        /** For the vanilla components, which already have a home in 1.20.1 NBT. */
        public Builder<T> backedBy(final DataComponentType.Accessor<T> accessor) {
            this.accessor = accessor;
            return this;
        }

        public DataComponentType<T> build() {
            return new DataComponentType<>("unnamed", this.codec, this.streamCodec, this.accessor);
        }
    }

    /**
     * The default: the value is codec-encoded into one compound on the stack,
     * so every ported component shares a single tag and nothing collides with
     * another mod's NBT.
     */
    static final class TagAccessor<T> implements Accessor<T> {

        private final String key;
        private final Codec<T> codec;

        TagAccessor(final String key, final Codec<T> codec) {
            this.key = key;
            this.codec = codec;
        }

        @Nullable
        @Override
        public T get(final ItemStack stack) {
            final CompoundTag root = this.root(stack);
            if (root == null || !root.contains(this.key)) {
                return null;
            }
            return this.codec.parse(net.minecraft.nbt.NbtOps.INSTANCE, root.get(this.key))
                    .result().orElse(null);
        }

        @Override
        public void set(final ItemStack stack, final T value) {
            this.codec.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, value).result().ifPresent(
                    encoded -> stack.getOrCreateTagElement(DataComponentType.ROOT).put(this.key, encoded));
        }

        @Override
        public boolean has(final ItemStack stack) {
            final CompoundTag root = this.root(stack);
            return root != null && root.contains(this.key);
        }

        @Override
        public void remove(final ItemStack stack) {
            final CompoundTag root = this.root(stack);
            if (root != null) {
                root.remove(this.key);
                if (root.isEmpty()) {
                    stack.removeTagKey(DataComponentType.ROOT);
                }
            }
        }

        @Nullable
        private CompoundTag root(final ItemStack stack) {
            final CompoundTag tag = stack.getTag();
            return tag != null && tag.contains(DataComponentType.ROOT) ? tag.getCompound(DataComponentType.ROOT) : null;
        }
    }
}
