package dev.simulated_team.simulated.multiloader.inventory.fabric;

import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Simulated's inventory view, over a Fabric item storage.
 *
 * <p>Upstream wraps NeoForge's {@code IItemHandler}, which takes a
 * {@code simulate} flag on every call. Fabric's transfer API instead runs a
 * transaction and rolls it back, so a simulated operation here opens a nested
 * transaction and aborts it. The counts a caller sees are the same either way.
 */
public class InventoryLoaderWrapperImpl extends InventoryLoaderWrapper {

    private final Storage<ItemVariant> storage;

    public InventoryLoaderWrapperImpl(final Storage<ItemVariant> storage) {
        this.storage = storage;
    }

    @Override
    public ItemStack extractAny(final int maxAmount, final boolean simulate, final boolean exact) {
        try (Transaction transaction = Transaction.openOuter()) {
            final ItemVariant found = StorageUtil.findExtractableResource(this.storage, transaction);
            if (found == null) {
                return ItemStack.EMPTY;
            }

            final long extracted = this.storage.extract(found, maxAmount, transaction);
            if (exact && extracted < maxAmount) {
                return ItemStack.EMPTY;
            }
            if (extracted <= 0) {
                return ItemStack.EMPTY;
            }

            if (!simulate) {
                transaction.commit();
                this.fireCallback(true);
            }

            return found.toStack((int) extracted);
        }
    }

    @Override
    public int insertGeneral(final ItemInfoWrapper info, final int amountToInsert, final boolean simulate) {
        final ItemVariant variant = ItemVariant.of(ItemInfoWrapper.generateFromInfo(info));

        try (Transaction transaction = Transaction.openOuter()) {
            final int inserted = (int) this.storage.insert(variant, amountToInsert, transaction);
            if (!simulate && inserted > 0) {
                transaction.commit();
                this.fireCallback(false);
            }
            return inserted;
        }
    }

    @Override
    public ItemStack insertSlot(final ItemStack stack, final int slot, final boolean simulate) {
        final SingleSlotStorage<ItemVariant> slotStorage = this.slot(slot);
        if (slotStorage == null) {
            return stack;
        }

        try (Transaction transaction = Transaction.openOuter()) {
            final int inserted = (int) slotStorage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            if (!simulate && inserted > 0) {
                transaction.commit();
                this.fireCallback(false);
            }

            // Upstream returns what did not fit.
            final ItemStack remainder = stack.copy();
            remainder.shrink(inserted);
            return remainder;
        }
    }

    @Override
    public int extractGeneral(final ItemInfoWrapper info, final int amountToExtract, final boolean simulate) {
        final ItemVariant variant = ItemVariant.of(ItemInfoWrapper.generateFromInfo(info));

        try (Transaction transaction = Transaction.openOuter()) {
            final int extracted = (int) this.storage.extract(variant, amountToExtract, transaction);
            if (!simulate && extracted > 0) {
                transaction.commit();
                this.fireCallback(true);
            }
            return extracted;
        }
    }

    @Override
    public ItemStack extractSlot(final int slot, final int amount, final boolean simulate) {
        final SingleSlotStorage<ItemVariant> slotStorage = this.slot(slot);
        if (slotStorage == null || slotStorage.isResourceBlank()) {
            return ItemStack.EMPTY;
        }

        final ItemVariant variant = slotStorage.getResource();

        try (Transaction transaction = Transaction.openOuter()) {
            final int extracted = (int) slotStorage.extract(variant, amount, transaction);
            if (extracted <= 0) {
                return ItemStack.EMPTY;
            }
            if (!simulate) {
                transaction.commit();
                this.fireCallback(true);
            }
            return variant.toStack(extracted);
        }
    }

    @Override
    public int getContainerSize() {
        return this.slots().size();
    }

    @Override
    public boolean isEmpty() {
        for (final StorageView<ItemVariant> view : this.storage) {
            if (!view.isResourceBlank() && view.getAmount() > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(final int slot) {
        final SingleSlotStorage<ItemVariant> slotStorage = this.slot(slot);
        if (slotStorage == null || slotStorage.isResourceBlank()) {
            return ItemStack.EMPTY;
        }
        return slotStorage.getResource().toStack((int) slotStorage.getAmount());
    }

    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        return this.extractSlot(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return this.extractSlot(slot, Integer.MAX_VALUE, false);
    }

    @Override
    public boolean stillValid(final net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    private void fireCallback(final boolean extracted) {
        if (this.callback != null) {
            this.callback.accept(extracted);
        }
    }

    /**
     * The storage's slots, when it exposes them. A storage that is not
     * slot-addressable — a pipe, say — reports none, and the slot-indexed
     * operations above decline rather than guess.
     */
    @SuppressWarnings("unchecked")
    private List<SingleSlotStorage<ItemVariant>> slots() {
        if (this.storage instanceof final net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage<?> slotted) {
            return (List<SingleSlotStorage<ItemVariant>>) (List<?>) slotted.getSlots();
        }
        return List.of();
    }

    private SingleSlotStorage<ItemVariant> slot(final int slot) {
        final List<SingleSlotStorage<ItemVariant>> slots = this.slots();
        return slot >= 0 && slot < slots.size() ? slots.get(slot) : null;
    }
}
