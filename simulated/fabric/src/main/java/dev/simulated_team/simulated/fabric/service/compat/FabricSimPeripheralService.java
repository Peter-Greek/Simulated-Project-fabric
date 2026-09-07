package dev.simulated_team.simulated.fabric.service.compat;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import dev.simulated_team.simulated.service.compat.SimPeripheralService;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * ComputerCraft peripherals, on its Fabric API.
 *
 * <p>Upstream registers these as NeoForge capabilities during a registration
 * event. ComputerCraft's Fabric build exposes the same thing as a block lookup,
 * so each is registered directly when the compat initialises — which only
 * happens if ComputerCraft is loaded at all, so nothing here is touched
 * otherwise.
 *
 * <p><b>Deviation.</b> Wired-modem elements are not registered: ComputerCraft's
 * 1.20.1 Fabric API has no public lookup for {@code WiredElement}, only the
 * NeoForge capability upstream uses. The Docking Connector still works as a
 * peripheral; it just cannot join a wired network. Recorded in
 * FABRIC_PORT_PLAN.md.
 */
public class FabricSimPeripheralService implements SimPeripheralService {

    @Override
    public <T extends BlockEntity> void addPeripheral(final Supplier<BlockEntityType<T>> typeSupplier,
                                                      final CapabilityGetter<T, IPeripheral> getter) {
        PeripheralLookup.get().registerForBlockEntity(getter::get, typeSupplier.get());
    }

    @Override
    public <T extends BlockEntity> void addWired(final Supplier<BlockEntityType<T>> typeSupplier,
                                                 final CapabilityGetter<T, WiredElement> getter) {
    }
}
