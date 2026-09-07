package dev.simulated_team.simulated.data;

import dev.simulated_team.simulated.Simulated;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

import java.util.List;
import java.util.Set;

/**
 * Runs Registrate's providers over everything registered by
 * {@link Simulated#init()}, writing into {@code src/main/generated}.
 *
 * <p>The helper is built with validation disabled rather than through
 * {@code ExistingFileHelper.withResourcesFromArg()}: that path reaches for
 * {@code Minecraft.getInstance()} for the asset index, and datagen runs on the
 * dedicated-server side where that class cannot load. Nothing here validates
 * that a referenced parent model exists — the game's own missing-model warnings
 * are what catch that, and V1 ships on those being clean.
 */
public class SimulatedDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(final FabricDataGenerator generator) {
        final FabricDataGenerator.Pack pack = generator.createPack();
        final ExistingFileHelper existingFileHelper =
                new ExistingFileHelper(List.of(), Set.of(), false, null, null);
        Simulated.getRegistrate().setupDatagen(pack, existingFileHelper);
    }
}
