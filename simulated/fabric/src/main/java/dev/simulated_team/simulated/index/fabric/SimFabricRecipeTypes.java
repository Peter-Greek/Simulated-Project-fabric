package dev.simulated_team.simulated.index.fabric;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.data.fabric.PortableEngineDyeingRecipe;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Simulated's own recipe serializers.
 *
 * <p>Upstream registers these through a NeoForge {@code DeferredRegister} in an
 * enum of the same shape. 1.20.1 Fabric registries are open, so each entry goes
 * in when {@link #register()} runs; the enum, its ids and its
 * {@link IRecipeTypeInfo} contract are otherwise unchanged, which is what
 * Create's recipe machinery reads.
 *
 * <p>Only the Portable Engine's dyeing recipe lives here. It reuses vanilla's
 * crafting recipe type, so nothing is registered into the recipe-type registry.
 */
public enum SimFabricRecipeTypes implements IRecipeTypeInfo, StringRepresentable {

    PORTABLE_ENGINE_DYEING(() -> new SimpleCraftingRecipeSerializer<>(PortableEngineDyeingRecipe::new),
            () -> RecipeType.CRAFTING);

    public final ResourceLocation id;
    public final Supplier<RecipeSerializer<?>> serializerSupplier;
    private final Supplier<RecipeType<?>> type;
    private RecipeSerializer<?> serializer;

    SimFabricRecipeTypes(final Supplier<RecipeSerializer<?>> serializerSupplier,
                         final Supplier<RecipeType<?>> typeSupplier) {
        this.id = Simulated.path(Lang.asId(this.name()));
        this.serializerSupplier = serializerSupplier;
        this.type = typeSupplier;
    }

    public static void register() {
        for (final SimFabricRecipeTypes entry : values()) {
            if (entry.serializer == null) {
                entry.serializer = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, entry.id,
                        entry.serializerSupplier.get());
            }
        }
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RecipeSerializer<?>> T getSerializer() {
        if (this.serializer == null) {
            throw new IllegalStateException(this.id + " has not been registered yet");
        }
        return (T) this.serializer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RecipeType<?>> T getType() {
        return (T) this.type.get();
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.id.toString();
    }
}
