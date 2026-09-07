package dev.simulated_team.simulated.index;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.block.ItemUseOverrides;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerAnchorBlock;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlock;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlock;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelGenerator;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

public class SimBlocks {
    private static final SimulatedRegistrate REGISTRATE = Simulated.getRegistrate();

    public static final BlockEntry<PhysicsAssemblerBlock> PHYSICS_ASSEMBLER =
            REGISTRATE.block("physics_assembler", PhysicsAssemblerBlock::new)
                    .initialProperties(SharedProperties::wooden)
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(BlockTags.MINEABLE_WITH_AXE)
                    .blockstate((c, p) -> p.horizontalFaceBlock(c.get(), AssetLookup.partialBaseModel(c, p)))
                    .item()
                    .transform(customItemModel())
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("   ")
                            .pattern(" N ")
                            .pattern("ARA")
                            .define('A', AllItems.ANDESITE_ALLOY)
                            .define('N', Items.LEVER)
                            .define('R', AllBlocks.ANDESITE_CASING)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING))
                            .save(p))
                    .register();

    /**
     * No item form: this block only exists while the visible assembler is part
     * of a moving Create contraption. Indestructible so a player cannot break
     * the controller out from under a live assembly.
     */
    public static final BlockEntry<PhysicsAssemblerAnchorBlock> PHYSICS_ASSEMBLER_ANCHOR =
            REGISTRATE.block("physics_assembler_anchor", PhysicsAssemblerAnchorBlock::new)
                    .initialProperties(() -> Blocks.BARRIER)
                    .properties(p -> p.mapColor(MapColor.NONE)
                            .instrument(NoteBlockInstrument.BASS)
                            .strength(-1.0F, 3600000.0F)
                            .noCollission()
                            .noOcclusion()
                            .noLootTable()
                            .pushReaction(PushReaction.BLOCK))
                    .blockstate((c, p) -> p.simpleBlock(c.get(), p.models()
                            .getExistingFile(p.mcLoc("block/air"))))
                    .register();

    public static final BlockEntry<SteeringWheelBlock> STEERING_WHEEL =
            REGISTRATE.block("steering_wheel", SteeringWheelBlock::new)
                    .initialProperties(SharedProperties::wooden)
                    .properties(p -> p.mapColor(MapColor.PODZOL))
                    .addLayer(() -> net.minecraft.client.renderer.RenderType::cutoutMipped)
                    .transform(axeOrPickaxe())
                    .blockstate(new SteeringWheelGenerator()::generate)
                    .onRegister(ItemUseOverrides::addBlock)
                    .tag(AllTags.AllBlockTags.SAFE_NBT.tag)
                    .recipe((c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, c.get(), 1)
                            .pattern("C")
                            .pattern("A")
                            .pattern("S")
                            .define('C', AllBlocks.LARGE_COGWHEEL)
                            .define('A', AllBlocks.ANDESITE_CASING)
                            .define('S', AllBlocks.SHAFT)
                            .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING))
                            .save(p))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static void register() {
    }
}
