package dev.simulated_team.simulated.data.fabric;

import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.fabric.SimFabricRecipeTypes;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * The hand-written recipes that are not processing recipes — on this stack, the
 * Portable Engine's dyeing recipe alone.
 *
 * <p>1.20.1's {@code SpecialRecipeBuilder} takes the registered serializer
 * rather than 1.20.5's recipe constructor. The written JSON is the same.
 */
public class SimStandardRecipeGen extends BaseRecipeProvider {

    GeneratedRecipe PORTABLE_ENGINE_DYEING =
            this.createSpecial(SimFabricRecipeTypes.PORTABLE_ENGINE_DYEING.getSerializer(),
                    "crafting", "portable_engine_dyeing");

    public SimStandardRecipeGen(final PackOutput output) {
        super(output, Simulated.MOD_ID);
    }

    @Override
    public String getName() {
        return "Simulated's Surprisingly Standard Recipes";
    }

    private GeneratedRecipe createSpecial(final RecipeSerializer<? extends CraftingRecipe> serializer,
                                          final String recipeType, final String path) {
        final ResourceLocation location = Simulated.path(recipeType + "/" + path);

        return this.register(consumer -> SpecialRecipeBuilder.special(serializer)
                .save(consumer, location.toString()));
    }
}
