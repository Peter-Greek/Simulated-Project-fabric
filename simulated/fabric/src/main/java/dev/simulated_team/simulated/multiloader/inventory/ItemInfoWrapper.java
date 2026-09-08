package dev.simulated_team.simulated.multiloader.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An info wrapper that holds an item type, and its associated data. Primarily used for Simulated's multiloader inventory structure. <p>
 * In order to generate a wrapper from a given item easily, <b>{@link ItemInfoWrapper#generateFromStack(ItemStack) generateFromStack()}</b> can be used. <p>
 * In order to generate a new item from a given wrapper easily, <b>{@link ItemInfoWrapper#generateFromInfo(ItemInfoWrapper) generateFromInfo()}</b> can be used.
 *
 * <p>Upstream carries the data as a {@code DataComponentPatch}, which is 1.20.5+.
 * Here it is the stack's NBT, which is the same information on this version, and
 * copying it in and out keeps the wrapper the immutable snapshot it is upstream.
 *
 * @param type The item type of this wrapper
 * @param tag  The data of this wrapper, or null if the item carries none
 */
public record ItemInfoWrapper(Item type, @Nullable CompoundTag tag) {

    /**
     * Generates a new wrapper from the given item
     *
     * @param stack The item stack to gather information from.
     * @return A <b>new</b> {@link ItemInfoWrapper} containing the type and data from the given item.
     */
    public static ItemInfoWrapper generateFromStack(final ItemStack stack) {
        final CompoundTag tag = stack.getTag();
        return new ItemInfoWrapper(stack.getItem(), tag == null ? null : tag.copy());
    }

    /**
     * Generates a new {@link ItemStack} from the given wrapper.
     *
     * @param info The {@link ItemInfoWrapper} to use information from.
     * @return A <b>new</b> {@link ItemStack} containing data from the given wrapper.
     */
    public static @NotNull ItemStack generateFromInfo(final ItemInfoWrapper info) {
        final ItemStack newStack = info.type().getDefaultInstance();
        if (info.tag() != null) {
            newStack.setTag(info.tag().copy());
        }
        return newStack;
    }
}
