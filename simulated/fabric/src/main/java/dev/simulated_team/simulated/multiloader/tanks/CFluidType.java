package dev.simulated_team.simulated.multiloader.tanks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;

/**
 * A loader-independent representation of a fluid.
 *
 * <p>Upstream carries the fluid's extra data as a {@code DataComponentPatch},
 * which arrived in 1.20.5. On this stack a fluid's data is still an NBT tag —
 * that is what Fabric's own {@code FluidVariant} holds — so the component patch
 * is a {@link CompoundTag} here and the serialised shape is the tag itself
 * rather than a codec-encoded patch.
 */
public record CFluidType(Fluid fluid, @Nullable CompoundTag data) {

    public static final CFluidType BLANK = new CFluidType(Fluids.EMPTY, null);

    public boolean isBlank() {
        return this.equals(BLANK);
    }

    public CompoundTag write() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.fluid).toString());

        if (this.data != null && !this.data.isEmpty()) {
            tag.put("data", this.data);
        }

        return tag;
    }

    public static CFluidType read(final CompoundTag tag) {
        final Fluid fluid = BuiltInRegistries.FLUID.get(new ResourceLocation(tag.getString("Fluid")));
        final CompoundTag data = tag.contains("data") ? tag.getCompound("data") : null;

        return new CFluidType(fluid, data);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final CFluidType other) {
            // both haves tag, or both no haves tag
            return this.fluid.isSame(other.fluid) && this.dataEquals(other.data);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.fluid.hashCode() * 31 + (this.data == null ? 0 : this.data.hashCode());
    }

    /** An absent tag and an empty tag describe the same fluid. */
    private boolean dataEquals(@Nullable final CompoundTag other) {
        final boolean mineEmpty = this.data == null || this.data.isEmpty();
        final boolean theirsEmpty = other == null || other.isEmpty();
        if (mineEmpty || theirsEmpty) {
            return mineEmpty == theirsEmpty;
        }
        return this.data.equals(other);
    }
}
