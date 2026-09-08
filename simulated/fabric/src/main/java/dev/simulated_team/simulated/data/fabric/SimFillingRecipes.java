package dev.simulated_team.simulated.data.fabric;

import com.simibubi.create.AllTags;
import com.simibubi.create.api.data.recipe.FillingRecipeGen;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimItems;
import net.minecraft.data.PackOutput;

/**
 * Spout filling recipes.
 *
 * <p>Two changes from upstream, both forced by the older Create: the provider
 * takes no registry lookup, and the honey fluid tag comes from Create's own
 * {@code AllFluidTags} rather than NeoForge's {@code Tags.Fluids}. Fluid amounts
 * on this stack are droplets rather than millibuckets, so upstream's 500 mB is
 * half of {@code BUCKET} — the same half-bucket of honey.
 */
public class SimFillingRecipes extends FillingRecipeGen {
    private final GeneratedRecipe HONEY_GLUE = this.create("honey_glue",
            b -> b.require(AllTags.AllFluidTags.HONEY.tag, BUCKET / 2)
                  .require(CommonMetal.IRON.plates)
                  .output(SimItems.HONEY_GLUE));

    public SimFillingRecipes(final PackOutput output) {
        super(output, Simulated.MOD_ID);
    }

    @Override
    public String getName() {
        return "Simulated's Fantastic Filling Recipes";
    }
}
