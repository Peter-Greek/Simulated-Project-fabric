package dev.simulated_team.simulated.fabric;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric bootstrap for the Minecraft 1.20.1 / Homestead port.
 *
 * Keep this entrypoint intentionally small while upstream systems are moved
 * behind loader-neutral or Fabric implementations one subsystem at a time.
 */
public final class SimulatedFabric implements ModInitializer {
    public static final String MOD_ID = "simulated";
    public static final Logger LOGGER = LoggerFactory.getLogger("Create Simulated");

    @Override
    public void onInitialize() {
        LOGGER.info("Starting Create Simulated Fabric port for the Homestead 1.20.1 stack");
    }
}
