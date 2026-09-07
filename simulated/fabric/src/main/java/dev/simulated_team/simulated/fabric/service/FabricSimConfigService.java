package dev.simulated_team.simulated.fabric.service;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.infrastructure.config.CStress;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.config.client.SimClient;
import dev.simulated_team.simulated.config.server.SimServer;
import dev.simulated_team.simulated.service.SimConfigService;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import net.createmod.catnip.config.ConfigBase;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Simulated's server and client config.
 *
 * <p>Upstream registers the specs through NeoForge's mod container. On Fabric,
 * Forge Config API Port — which Create already ships and Create's own config
 * uses — takes the same {@code ForgeConfigSpec} and registers it against a mod
 * id. The spec, the file layout, and every value are upstream's.
 */
public class FabricSimConfigService implements SimConfigService {

    public static final Map<ModConfig.Type, ConfigBase> CONFIGS = new EnumMap<>(ModConfig.Type.class);

    private static SimServer server;
    private static SimClient client;

    @Override
    public boolean serverLoaded() {
        return server != null && server.specification != null && server.specification.isLoaded();
    }

    @Override
    public boolean clientLoaded() {
        return client != null && client.specification != null && client.specification.isLoaded();
    }

    @Override
    public SimServer server() {
        return server;
    }

    @Override
    public SimClient client() {
        return client;
    }

    public static ConfigBase byType(final ModConfig.Type type) {
        return CONFIGS.get(type);
    }

    private static void onLoading(final ModConfig config) {
        forSpecOf(config, ConfigBase::onLoad);
    }

    private static void onReloading(final ModConfig config) {
        forSpecOf(config, ConfigBase::onReload);
    }

    private static void forSpecOf(final ModConfig config, final java.util.function.Consumer<ConfigBase> action) {
        for (final ConfigBase base : CONFIGS.values()) {
            if (base.specification == config.getSpec()) {
                action.accept(base);
            }
        }
    }

    private static <T extends ConfigBase> T build(final Supplier<T> factory, final ModConfig.Type side) {
        final Pair<T, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(builder -> {
            final T config = factory.get();
            config.registerAll(builder);
            return config;
        });
        final T config = specPair.getLeft();
        config.specification = specPair.getRight();
        CONFIGS.put(side, config);
        return config;
    }

    /** Called once from the mod initialiser, before anything reads a value. */
    public static void register() {
        server = build(SimServer::new, ModConfig.Type.SERVER);
        client = build(SimClient::new, ModConfig.Type.CLIENT);

        for (final Map.Entry<ModConfig.Type, ConfigBase> entry : CONFIGS.entrySet()) {
            ForgeConfigRegistry.INSTANCE.register(Simulated.MOD_ID, entry.getKey(), entry.getValue().specification);
        }

        // Upstream runs these off NeoForge's ModConfigEvent. Without them a
        // config value read through a cached field keeps its startup value, so
        // editing the file mid-session appears to do nothing.
        ModConfigEvents.loading(Simulated.MOD_ID).register(FabricSimConfigService::onLoading);
        ModConfigEvents.reloading(Simulated.MOD_ID).register(FabricSimConfigService::onReloading);

        final CStress stress = SimConfigService.INSTANCE.server().kinetics.stressValues;
        BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
        BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);
    }
}
