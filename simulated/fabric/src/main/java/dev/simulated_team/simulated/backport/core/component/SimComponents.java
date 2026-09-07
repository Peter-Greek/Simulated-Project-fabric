package dev.simulated_team.simulated.backport.core.component;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The component operations 1.20.5 put on {@code ItemStack} itself.
 *
 * <p>Ported call sites read {@code SimComponents.get(stack, TYPE)} where
 * upstream reads {@code stack.get(TYPE)}. Rewriting the call site rather than
 * mixing the methods onto {@code ItemStack} keeps every component access
 * greppable and keeps the port out of vanilla's own class.
 */
public final class SimComponents {

    private SimComponents() {
    }

    @Nullable
    public static <T> T get(final ItemStack stack, final DataComponentType<T> type) {
        return stack.isEmpty() ? null : type.accessor().get(stack);
    }

    public static <T> T getOrDefault(final ItemStack stack, final DataComponentType<T> type, final T fallback) {
        final T value = get(stack, type);
        return value != null ? value : fallback;
    }

    public static <T> void set(final ItemStack stack, final DataComponentType<T> type, @Nullable final T value) {
        if (stack.isEmpty()) {
            return;
        }
        if (value == null) {
            type.accessor().remove(stack);
        } else {
            type.accessor().set(stack, value);
        }
    }

    /**
     * Writes a component straight into an item's root tag, for the places that
     * only have the tag and not the stack — vanilla's compass, which hands
     * {@code addLodestoneTags} a {@link CompoundTag}, is the one caller.
     *
     * <p>Only meaningful for a component that keeps its value in the mod's own
     * compound; one mapped onto a vanilla home has no representation here and
     * is ignored.
     */
    public static <T> void setOnTag(final CompoundTag itemTag, final DataComponentType<T> type, final T value) {
        final Codec<T> codec = type.codec();
        if (codec == null) {
            return;
        }
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(encoded -> {
            if (!itemTag.contains(DataComponentType.ROOT)) {
                itemTag.put(DataComponentType.ROOT, new CompoundTag());
            }
            itemTag.getCompound(DataComponentType.ROOT).put(type.name(), encoded);
        });
    }

    public static <T> boolean has(final ItemStack stack, final DataComponentType<T> type) {
        return !stack.isEmpty() && type.accessor().has(stack);
    }

    public static <T> void remove(final ItemStack stack, final DataComponentType<T> type) {
        if (!stack.isEmpty()) {
            type.accessor().remove(stack);
        }
    }
}
