package dev.simulated_team.simulated.backport.core.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Stand-in for {@code net.minecraft.world.item.component.CustomData} (1.20.5+),
 * the immutable wrapper 1.20.5 puts around a stack's leftover NBT.
 *
 * <p>On 1.20.1 that data is the tag itself. The wrapper is kept so upstream's
 * {@code copyTag()} calls read as written, and it copies on the way in and out
 * for the same reason vanilla's does: a caller must not be able to mutate what
 * is on the stack without going back through {@link #set}.
 */
public final class CustomData {

    private final CompoundTag tag;

    public CustomData(final CompoundTag tag) {
        this.tag = tag.copy();
    }

    public CompoundTag copyTag() {
        return this.tag.copy();
    }

    public boolean isEmpty() {
        return this.tag.isEmpty();
    }

    /** Upstream's static setter, which writes a raw compound onto the stack. */
    public static void set(final DataComponentType<CustomData> type, final ItemStack stack, final CompoundTag tag) {
        SimComponents.set(stack, type, new CustomData(tag));
    }
}
