package dev.simulated_team.simulated.backport.core.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;

import javax.annotation.Nullable;

/**
 * The vanilla components Simulated reads, mapped onto where 1.20.1 keeps that
 * same data.
 *
 * <p>Only the ones upstream touches <em>and</em> that have a 1.20.1 home on the
 * stack are here; each is backed by the pre-component storage, so a stack from
 * anywhere else still reads correctly. Three upstream reads have no such home
 * and were ported at the call site instead — map decorations come from the
 * map's saved data, the lodestone target from three top-level stack tags, and
 * the Extendo Grip attribute modifiers wait on Sable's attributes in V2. All
 * three are recorded in FABRIC_PORT_PLAN.md.
 */
public final class DataComponents {

    /** 1.20.1 keeps the custom name in {@code display.Name}. */
    public static final DataComponentType<Component> CUSTOM_NAME =
            DataComponentType.<Component>builder().backedBy(new DataComponentType.Accessor<>() {
                @Nullable
                @Override
                public Component get(final ItemStack stack) {
                    return stack.hasCustomHoverName() ? stack.getHoverName() : null;
                }

                @Override
                public void set(final ItemStack stack, final Component value) {
                    stack.setHoverName(value);
                }

                @Override
                public boolean has(final ItemStack stack) {
                    return stack.hasCustomHoverName();
                }

                @Override
                public void remove(final ItemStack stack) {
                    stack.resetHoverName();
                }
            }).build().named("custom_name");

    /**
     * 1.20.5 wraps block entity data in {@code CustomData}; 1.20.1 keeps the raw
     * compound under {@code BlockEntityTag}. Ported call sites unwrap through
     * {@link CustomData}, which here is that compound.
     */
    public static final DataComponentType<CustomData> BLOCK_ENTITY_DATA =
            DataComponentType.<CustomData>builder().backedBy(new DataComponentType.Accessor<>() {
                @Nullable
                @Override
                public CustomData get(final ItemStack stack) {
                    final CompoundTag tag = stack.getTagElement("BlockEntityTag");
                    return tag == null ? null : new CustomData(tag);
                }

                @Override
                public void set(final ItemStack stack, final CustomData value) {
                    stack.getOrCreateTag().put("BlockEntityTag", value.copyTag());
                }

                @Override
                public boolean has(final ItemStack stack) {
                    return stack.getTagElement("BlockEntityTag") != null;
                }

                @Override
                public void remove(final ItemStack stack) {
                    stack.removeTagKey("BlockEntityTag");
                }
            }).build().named("block_entity_data");

    /** Durability is an item property on 1.20.1, not stack data. */
    public static final DataComponentType<Integer> MAX_DAMAGE =
            DataComponentType.<Integer>builder().backedBy(new DataComponentType.Accessor<>() {
                @Nullable
                @Override
                public Integer get(final ItemStack stack) {
                    return stack.isDamageableItem() ? stack.getMaxDamage() : null;
                }

                @Override
                public void set(final ItemStack stack, final Integer value) {
                    // Not settable per stack before 1.20.5; upstream only reads it.
                }

                @Override
                public boolean has(final ItemStack stack) {
                    return stack.isDamageableItem();
                }

                @Override
                public void remove(final ItemStack stack) {
                }
            }).build().named("max_damage");

    /** The map's id, which 1.20.1 keeps in the stack tag as {@code map}. */
    public static final DataComponentType<Integer> MAP_ID =
            DataComponentType.<Integer>builder().backedBy(new DataComponentType.Accessor<>() {
                @Nullable
                @Override
                public Integer get(final ItemStack stack) {
                    return MapItem.getMapId(stack);
                }

                @Override
                public void set(final ItemStack stack, final Integer value) {
                    stack.getOrCreateTag().putInt("map", value);
                }

                @Override
                public boolean has(final ItemStack stack) {
                    return MapItem.getMapId(stack) != null;
                }

                @Override
                public void remove(final ItemStack stack) {
                    stack.removeTagKey("map");
                }
            }).build().named("map_id");

    private DataComponents() {
    }
}
