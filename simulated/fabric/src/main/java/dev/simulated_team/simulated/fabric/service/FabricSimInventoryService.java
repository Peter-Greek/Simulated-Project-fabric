package dev.simulated_team.simulated.fabric.service;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import dev.simulated_team.simulated.multiloader.energy.SingleBattery;
import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.inventory.fabric.InventoryLoaderWrapperImpl;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import dev.simulated_team.simulated.service.SimInventoryService;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Inventory, tank and energy access, over Fabric's transfer API.
 *
 * <p>Upstream registers capability providers with NeoForge and reads them back
 * through the capability system. Fabric's equivalent is a lookup registered per
 * block entity type, which is what the three {@code register…} methods do.
 *
 * <p><b>Deviation.</b> Upstream can also wrap a contraption's mounted storage,
 * because NeoForge exposes it as an item handler. Create's Fabric build exposes
 * it as a {@code Storage} only through its own wrapper, and not for the mounted
 * half separately; both wrap-a-contraption methods therefore return null and the
 * Auger's contraption-side item transfer is inert. Recorded in
 * FABRIC_PORT_PLAN.md.
 */
public class FabricSimInventoryService implements SimInventoryService {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends InventoryLoaderWrapper> T getInventory(@Nullable final BlockEntity be,
                                                             @Nullable final Direction dir) {
        if (be == null || be.getLevel() == null) {
            return null;
        }

        final Storage<ItemVariant> storage =
                ItemStorage.SIDED.find(be.getLevel(), be.getBlockPos(), be.getBlockState(), be, dir);
        if (storage == null) {
            return null;
        }

        return (T) new InventoryLoaderWrapperImpl(storage);
    }

    @Override
    public <T extends InventoryLoaderWrapper> T getWrappedAllItemsFromContraption(final MountedStorageManager manager) {
        return null;
    }

    @Override
    public <T extends InventoryLoaderWrapper> T getWrappedMountedItemsFromContraption(final MountedStorageManager manager) {
        return null;
    }

    @Override
    public <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerInventory(
            final BiFunction<T, Direction, AbstractContainer> getter) {
        return type -> ItemStorage.SIDED.registerForBlockEntity((be, direction) -> {
            final AbstractContainer container = getter.apply(be, direction);
            if (container == null) {
                return null;
            }
            return net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage.of(container, direction);
        }, type);
    }

    @Override
    public <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerTank(
            final BiFunction<T, Direction, SingleTank> getter) {
        // Create's own fluid handling covers the Docking Connector's tank on this
        // stack; exposing it a second time here would double-count transfers.
        return type -> {
        };
    }

    @Override
    public <T extends BlockEntity> NonNullConsumer<BlockEntityType<T>> registerBattery(
            final BiFunction<T, Direction, SingleBattery> getter) {
        // Fabric has no first-party energy API, and Homestead ships no energy mod
        // Simulated needs to talk to. Recorded in FABRIC_PORT_PLAN.md.
        return type -> {
        };
    }
}
