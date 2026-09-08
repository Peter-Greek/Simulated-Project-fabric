package dev.simulated_team.simulated.compat.naturescompass;

import dev.simulated_team.simulated.Simulated;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers the Nature's Compass navigation target, looking the compass item up
 * by id rather than through the mod's own class, so nothing here links against
 * Nature's Compass. Only reached when the mod is loaded.
 */
public class NaturesCompassRegistry {

    private static final ResourceLocation COMPASS = new ResourceLocation("naturescompass", "naturescompass");

    public static void init() {
        Simulated.getRegistrate().navTarget("natures_compass", NaturesCompassNavigationTarget::new,
                () -> BuiltInRegistries.ITEM.get(COMPASS));
    }
}
