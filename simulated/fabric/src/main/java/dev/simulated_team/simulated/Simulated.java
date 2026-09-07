package dev.simulated_team.simulated;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;

/**
 * Loader-neutral entry point, mirroring upstream's class of the same name.
 *
 * <p>{@code SimulatedFabric} is the Fabric {@code ModInitializer} that calls
 * into this. Registration order here follows upstream so that later subsystems
 * can be dropped in without reshuffling it.
 */
public final class Simulated {
    public static final String MOD_ID = "simulated";
    public static final String MOD_NAME = "Create Simulated";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final NonNullSupplier<SimulatedRegistrate> REGISTRATE = NonNullSupplier.lazy(() ->
            (SimulatedRegistrate) new SimulatedRegistrate(path("simulated"), MOD_ID)
                    .defaultCreativeTab((ResourceKey<CreativeModeTab>) null));

    private Simulated() {
    }

    public static void init() {
        setTooltips();
        getRegistrate().addDataGenerator(ProviderType.LANG, SimLang::registrateLang);

        SimResourceManagers.init();
        SimBlocks.register();
        SimItems.register();
        SimBlockEntityTypes.register();

        getRegistrate().register();
    }

    public static void setTooltips() {
        getRegistrate().setTooltipModifierFactory(item -> {
            final Rarity rarity = item.getDefaultInstance().getRarity();
            FontHelper.Palette color = FontHelper.Palette.STANDARD_CREATE;
            if (rarity == Rarity.EPIC)
                color = new FontHelper.Palette(TooltipHelper.styleFromColor(SimColors.EPIC_OURPLE),
                        TooltipHelper.styleFromColor(rarity.color));

            return new ItemDescription
                    .Modifier(item, color)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item)));
        });
    }

    public static SimulatedRegistrate getRegistrate() {
        return REGISTRATE.get();
    }

    public static ResourceLocation path(final String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
