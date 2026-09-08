package dev.simulated_team.simulated.compat.explorerscompass;

import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers the Explorer's Compass navigation target, looking the compass item
 * up by id rather than through the mod's own class, so nothing here links
 * against Explorer's Compass. Only reached when the mod is loaded.
 */
public class ExplorersCompassRegistry {

    private static final ResourceLocation COMPASS = new ResourceLocation("explorerscompass", "explorerscompass");

    public static void init() {
        Simulated.getRegistrate().navTarget("explorers_compass", ExplorersCompassNavigationTarget::new,
                () -> BuiltInRegistries.ITEM.get(COMPASS));
    }
}
