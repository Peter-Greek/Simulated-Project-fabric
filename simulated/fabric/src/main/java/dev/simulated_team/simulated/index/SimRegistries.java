package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.backport.veil.platform.registry.RegistrationProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class SimRegistries {
	public static class Keys {
		public static final ResourceKey<Registry<NavigationTarget>> NAVIGATION_TARGET = key("navigation_target");
		public static final ResourceKey<Registry<BlockPropertiesTooltip.Entry>> PROPERTY_TOOLTIP = key("property_tooltip");

		private static <T> ResourceKey<Registry<T>> key(final String name) {
			return ResourceKey.createRegistryKey(Simulated.path(name));
		}
	}

	/** Navigation targets are read on both sides: the nav-table data component encodes them. */
	public static final Registry<NavigationTarget> NAVIGATION_TARGET = registry(Keys.NAVIGATION_TARGET);

	/**
	 * The property-tooltip registry, created on first use.
	 *
	 * <p>Its entries are tooltip functions filled in by {@code BlockPropertiesTooltip.init}
	 * from client setup, and read only when a tooltip is drawn, so a dedicated server
	 * fills it with nothing. Creating it eagerly there made vanilla log
	 * {@code Registry 'simulated:property_tooltip' was empty after loading} on every
	 * boot. Holding it behind a class that only client code touches means the server
	 * never creates it at all. Nothing is lost by that: the registry is unsynced, and
	 * nothing outside the tooltip path reads it.
	 */
	public static Registry<BlockPropertiesTooltip.Entry> propertyTooltip() {
		return PropertyTooltipHolder.REGISTRY;
	}

	private static final class PropertyTooltipHolder {
		private static final Registry<BlockPropertiesTooltip.Entry> REGISTRY = registry(Keys.PROPERTY_TOOLTIP);
	}

	private static <T> Registry<T> registry(final ResourceKey<Registry<T>> registryKey) {
		final RegistrationProvider<T> provider = RegistrationProvider.get(registryKey, Simulated.MOD_ID);
		return provider.asVanillaRegistry();
	}

	public static void register() {}
}
