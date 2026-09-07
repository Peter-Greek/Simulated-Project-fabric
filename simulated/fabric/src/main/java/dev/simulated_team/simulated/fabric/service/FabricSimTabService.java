package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.fabric.SimulatedFabricContent;
import dev.simulated_team.simulated.service.SimTabService;
import net.minecraft.world.item.CreativeModeTab;

public class FabricSimTabService implements SimTabService {

    @Override
    public CreativeModeTab getCreativeTab() {
        return SimulatedFabricContent.creativeTab();
    }
}
