package dev.simulated_team.simulated.data.fabric;

import com.simibubi.create.AllTags;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlock;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.fabric.SimFabricRecipeTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Recolours a Portable Engine by crafting it with a dye.
 *
 * <p>Three changes from upstream, all 1.20.5 API that does not exist here: a
 * recipe carries its own id, the crafting grid is a {@code CraftingContainer}
 * rather than a {@code CraftingInput}, and the engine's data is carried over as
 * NBT rather than as a component patch. The behaviour is the same — one engine
 * plus one dye, and everything else on the stack survives the recolour.
 */
public class PortableEngineDyeingRecipe extends CustomRecipe {

    public PortableEngineDyeingRecipe(final ResourceLocation id, final CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(final CraftingContainer input, final Level level) {
        int engines = 0;
        int dyes = 0;

        for (int i = 0; i < input.getContainerSize(); ++i) {
            final ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (Block.byItem(stack.getItem()) instanceof PortableEngineBlock) {
                    ++engines;
                } else {
                    if (!stack.is(AllTags.forgeItemTag("dyes")))
                        return false;
                    ++dyes;
                }

                if (dyes > 1 || engines > 1) {
                    return false;
                }
            }
        }

        return engines == 1 && dyes == 1;
    }

    @Override
    public ItemStack assemble(final CraftingContainer input, final RegistryAccess registries) {
        ItemStack engine = ItemStack.EMPTY;
        DyeColor color = DyeColor.RED;

        for (int i = 0; i < input.getContainerSize(); ++i) {
            final ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (Block.byItem(stack.getItem()) instanceof PortableEngineBlock) {
                    engine = stack;
                } else if (stack.getItem() instanceof final DyeItem dye) {
                    // 1.20.1 has no DyeColor.getColor(ItemStack); every vanilla
                    // member of the dye tag is a DyeItem, and anything else in
                    // it leaves the colour at the default, as upstream does.
                    color = dye.getDyeColor();
                }
            }
        }

        final ItemStack dyedEngine = SimBlocks.PORTABLE_ENGINES.get(color)
                .asStack();
        final CompoundTag tag = engine.getTag();
        if (tag != null && !tag.isEmpty()) {
            dyedEngine.setTag(tag.copy());
        }

        return dyedEngine;
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SimFabricRecipeTypes.PORTABLE_ENGINE_DYEING.getSerializer();
    }
}
