package dev.simulated_team.simulated.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Registers the temporary Create-backed flight controls for the Fabric port. */
public final class SimulatedFabricFlightInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        SimulatedFabricNetworking.registerServerReceivers();

        // Helms advance on the server tick rather than on packet arrival, so a
        // craft behaves identically regardless of the pilot's packet rate.
        ServerTickEvents.END_SERVER_TICK.register(SimulatedFabricNetworking::tickFlightSessions);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                SimulatedFabricNetworking.handlePlayerDisconnect(handler.getPlayer()));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> SimulatedFabricNetworking.clearFlightControls());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SimulatedFabricNetworking.clearFlightControls());
    }
}
