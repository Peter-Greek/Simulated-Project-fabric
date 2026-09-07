package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.api.SimpleResourceManager;
import dev.simulated_team.simulated.client.sections.SimulatedSection;

public class SimResourceManagers {
    public static final SimpleResourceManager<SimulatedSection> SIMULATED_SECTION =
            SimpleResourceManager.create(SimulatedSection.CODEC, Simulated.path("sections")).sorted();

    public static void init() {
    }
}
