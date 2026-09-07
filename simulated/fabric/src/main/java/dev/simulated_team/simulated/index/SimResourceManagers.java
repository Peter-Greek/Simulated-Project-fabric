package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.api.SimpleResourceManager;
import dev.simulated_team.simulated.client.SearchAlias;
import dev.simulated_team.simulated.client.sections.SimulatedSection;
import dev.simulated_team.simulated.content.entities.diagram.screen.Greeble;

public class SimResourceManagers {
	public static SimpleResourceManager<SimulatedSection> SIMULATED_SECTION = SimpleResourceManager.create(SimulatedSection.CODEC, Simulated.path("sections")).sorted();
	public static SimpleResourceManager<Greeble> GREEBLE = SimpleResourceManager.create(Greeble.CODEC, Simulated.path("greebles"));
	public static SimpleResourceManager<SearchAlias> SEARCH_ALIAS = SimpleResourceManager.create(SearchAlias.CODEC, Simulated.path("search_aliases"));

	/**
	 * Registers the three managers as reload listeners.
	 *
	 * <p>Upstream registers them through a loader service; on Fabric a listener
	 * is registered directly, and these are client-side resources, so they go on
	 * the client resource reloader. Called from the client initialiser.
	 */
	public static void init() {
		register(SIMULATED_SECTION);
		register(GREEBLE);
		register(SEARCH_ALIAS);
	}

	private static void register(final SimpleResourceManager<?> manager) {
		net.fabricmc.fabric.api.resource.ResourceManagerHelper
				.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
				.registerReloadListener(manager);
	}
}