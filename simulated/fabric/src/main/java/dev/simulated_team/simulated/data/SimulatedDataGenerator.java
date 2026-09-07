package dev.simulated_team.simulated.data;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.data.fabric.SimFillingRecipes;
import dev.simulated_team.simulated.data.fabric.SimMechanicalCraftingRecipes;
import dev.simulated_team.simulated.data.fabric.SimSequencedAssemblyRecipes;
import dev.simulated_team.simulated.data.fabric.SimStandardRecipeGen;
import dev.simulated_team.simulated.api.sound.SoundsProvider;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.index.SimTags;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

import java.util.List;
import java.util.Set;

/**
 * Runs Registrate's providers over everything registered by
 * {@link Simulated#init()}, writing into {@code src/main/generated}, and then
 * the four providers that sit outside Registrate.
 *
 * <p>The helper is built with validation disabled rather than through
 * {@code ExistingFileHelper.withResourcesFromArg()}: that path reaches for
 * {@code Minecraft.getInstance()} for the asset index, and datagen runs on the
 * dedicated-server side where that class cannot load. Nothing here validates
 * that a referenced parent model exists — the game's own missing-model warnings
 * are what catch that, and V1 ships on those being clean.
 *
 * <p>Upstream splits this across two NeoForge events: a high-priority one that
 * adds {@link SimTags}' generators before Registrate's providers are built, and
 * a second that adds the advancements and the recipe providers. Fabric has one
 * entrypoint, so the ordering is expressed directly — tags first, because
 * {@code setupDatagen} builds the tag providers from what is registered at that
 * moment.
 */
public class SimulatedDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(final FabricDataGenerator generator) {
        SimTags.addGenerators();

        final FabricDataGenerator.Pack pack = generator.createPack();
        final ExistingFileHelper existingFileHelper =
                new ExistingFileHelper(List.of(), Set.of(), false, null, null);
        Simulated.getRegistrate().setupDatagen(pack, existingFileHelper);

        // Both addProvider overloads accept a one-argument lambda, so each
        // factory is named explicitly to say which one it is.
        pack.addProvider((FabricDataGenerator.Pack.RegistryDependentFactory<SimAdvancements>)
                SimAdvancements::new);
        pack.addProvider((FabricDataGenerator.Pack.Factory<SimFillingRecipes>)
                SimFillingRecipes::new);
        pack.addProvider((FabricDataGenerator.Pack.Factory<SimMechanicalCraftingRecipes>)
                SimMechanicalCraftingRecipes::new);
        pack.addProvider((FabricDataGenerator.Pack.Factory<SimSequencedAssemblyRecipes>)
                SimSequencedAssemblyRecipes::new);
        pack.addProvider((FabricDataGenerator.Pack.Factory<SimStandardRecipeGen>)
                SimStandardRecipeGen::new);

        // sounds.json is not something Registrate describes. Without it every
        // sound event this mod fires resolves to nothing and the log fills with
        // missing-sound warnings.
        pack.addProvider((FabricDataGenerator.Pack.Factory<SoundsProvider>)
                output -> SimSoundEvents.REGISTRY.getProvider(output));
    }
}
