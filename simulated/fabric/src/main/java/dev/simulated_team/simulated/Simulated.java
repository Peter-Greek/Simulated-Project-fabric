package dev.simulated_team.simulated;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.simulated_team.simulated.backport.physics.platform.SableEventPlatform;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.data.advancements.SimAdvancementTriggers;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.events.SimulatedCommonEvents;
import dev.simulated_team.simulated.index.*;
import dev.simulated_team.simulated.network.SimPacketManager;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.service.SimModCompatibilityService;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;

public final class Simulated {
    public static final String MOD_ID = "simulated";
    public static final String MOD_NAME = "Create Simulated";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final NonNullSupplier<SimulatedRegistrate> REGISTRATE = NonNullSupplier.lazy(() ->
            (SimulatedRegistrate) new SimulatedRegistrate(path("simulated"), MOD_ID).defaultCreativeTab((ResourceKey)null));

    /**
     * Upstream also calls {@code SimAssemblyHelper.register()} here, which hooks
     * the sub-level assembler into Sable's own registry. There is no such
     * registry on this stack; the helper declines every assembly until V2.
     */
    public static void init() {
        setTooltips();
        SimEntityDataSerializers.register();
        getRegistrate().addDataGenerator(ProviderType.LANG, SimLang::registrateLang);

        SimRegistries.register();
        SimTags.register();
        SimBlocks.register();
        SimItems.register();
        SimBlockEntityTypes.register();
        SimParticleTypes.register();
        SimSoundEvents.init();
        SimSpriteShifts.init();
        SimPacketManager.init();
        SimEntityTypes.register();
        SimMenuTypes.register();
        SimNavigationTargets.register();
        SimDataComponents.register();
        SimItemAttributeTypes.init();

        SimArmInteractions.init();

        // Upstream does these two on NeoForge's trigger-registry event. The
        // first only loads the advancement class, which is what builds the
        // triggers; the second puts them in CriteriaTriggers. Order matters.
        SimAdvancements.register();
        SimAdvancementTriggers.register();

        SimulatedCommonEvents.register();
        SimBlockMovementChecks.register();
        SimModCompatibilityService.initLoaded();

        SableEventPlatform.INSTANCE.onPhysicsTick(SimulatedCommonEvents::onPhysicsTick);
        SableEventPlatform.INSTANCE.onPostPhysicsTick(SimulatedCommonEvents::onPostPhysicsTick);
    }

    public static void setTooltips() {
        getRegistrate().setTooltipModifierFactory(item -> {
            final Rarity rarity = item.getDefaultInstance().getRarity();
            FontHelper.Palette color = FontHelper.Palette.STANDARD_CREATE;
            if (rarity == Rarity.EPIC)
                color = new FontHelper.Palette(TooltipHelper.styleFromColor(SimColors.EPIC_OURPLE), TooltipHelper.styleFromColor(rarity.color));

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
