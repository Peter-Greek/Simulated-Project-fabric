package dev.simulated_team.simulated.content.item_attributes;

import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import dev.simulated_team.simulated.backport.physics.mixinterface.block_properties.BlockStateExtension;
import dev.simulated_team.simulated.backport.physics.physics.config.block_properties.PhysicsBlockPropertyTypes;
import dev.simulated_team.simulated.index.SimItemAttributeTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BlockMassItemAttribute implements ItemAttribute {

    private double mass;

    public BlockMassItemAttribute(final double mass) {
        this.mass = mass;
    }

    public double mass() {
        return this.mass;
    }



	@Override
	public boolean appliesTo(final ItemStack stack, final Level world) {
		if(stack.getItem() instanceof final BlockItem item) {
			final BlockStateExtension extension = BlockStateExtension.of(item.getBlock().defaultBlockState());
			return extension.sable$getProperty(PhysicsBlockPropertyTypes.MASS.get()) == this.mass();
		}
		return false;
	}

	@Override
	public void save(final net.minecraft.nbt.CompoundTag tag) {
		tag.putDouble("value", this.mass);
	}

	@Override
	public void load(final net.minecraft.nbt.CompoundTag tag) {
		this.mass = tag.getDouble("value");
	}

	@Override
	public ItemAttributeType getType() {
		return SimItemAttributeTypes.BLOCK_MASS.get();
	}

	@Override
	public String getTranslationKey() {
		return "block_mass";
	}

	@Override
	public Object[] getTranslationParameters() {
		return new Object[]{ this.mass() };
	}

	public static class Type implements ItemAttributeType {

		@Override
		public @NotNull ItemAttribute createAttribute() {
			return new BlockMassItemAttribute(1.0);
		}

		@Override
		public List<ItemAttribute> getAllAttributes(final ItemStack stack, final Level level) {
			if(stack.getItem() instanceof final BlockItem item) {
				final BlockStateExtension extension = BlockStateExtension.of(item.getBlock().defaultBlockState());
				final double mass = extension.sable$getProperty(PhysicsBlockPropertyTypes.MASS.get());
				return List.of(new BlockMassItemAttribute(mass));
			}
			return List.of();
		}


	}
}
