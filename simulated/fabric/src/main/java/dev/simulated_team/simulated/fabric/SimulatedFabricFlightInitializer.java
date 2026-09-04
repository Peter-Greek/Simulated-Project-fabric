package dev.simulated_team.simulated.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/** Registers the temporary Create-backed flight controls for the Fabric port. */
public final class SimulatedFabricFlightInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        SimulatedFabricNetworking.registerServerReceivers();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SimulatedFabricNetworking.clearFlightControls());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SimulatedFabricNetworking.clearFlightControls());
    }
}
