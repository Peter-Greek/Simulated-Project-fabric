package dev.simulated_team.simulated.index.fabric;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimStats;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulated's seven custom statistics, on Fabric.
 *
 * <p>Upstream defers each id into NeoForge's {@code CUSTOM_STAT} register and
 * then, once loading is complete, asks {@code Stats.CUSTOM} for each one so the
 * statistic shows up in the menu before it is first awarded. 1.20.1 Fabric
 * registries are open, so both halves happen in {@link #register()} — there is
 * nothing to wait for.
 */
public class FabricSimStats extends SimStats {

    private static final List<SimStats.Stat> STATS_TO_LOAD = new ArrayList<>();

    public static void register() {
        new FabricSimStats().init();

        for (final SimStats.Stat stat : STATS_TO_LOAD) {
            Stats.CUSTOM.get(stat.identifier().get(), stat.formatter());
        }
        STATS_TO_LOAD.clear();
    }

    @Override
    protected SimStats.Stat makeCustomStat(final String key, final StatFormatter formatter) {
        final ResourceLocation id = Simulated.path(key);
        Registry.register(BuiltInRegistries.CUSTOM_STAT, id, id);

        final SimStats.Stat stat = new SimStats.Stat(() -> id, formatter);
        STATS_TO_LOAD.add(stat);
        return stat;
    }
}
