package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.service.SimItemService;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.world.item.ItemStack;

/**
 * Fuel values.
 *
 * <p>The ordinary burn time comes from Fabric's fuel registry, which is the same
 * number the furnace uses. The superheated one is a Create data map added in
 * 1.20.5; this Create build has no such map, so nothing is superheated — the
 * Portable Engine falls back to its normal fuel handling. Recorded in
 * FABRIC_PORT_PLAN.md.
 */
public class FabricSimItemService implements SimItemService {

    @Override
    public int getBurnTime(final ItemStack stack) {
        final Integer burnTime = FuelRegistry.INSTANCE.get(stack.getItem());
        return burnTime == null ? 0 : burnTime;
    }

    @Override
    public int getSuperheatedBurnTime(final ItemStack stack) {
        return 0;
    }
}
