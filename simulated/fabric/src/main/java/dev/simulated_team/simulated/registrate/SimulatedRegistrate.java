package dev.simulated_team.simulated.registrate;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.fabric.RegistryObject;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Upstream's Registrate subclass, on the 1.20.1 Registrate API.
 *
 * <p>Differences from upstream, all forced by the older Registrate:
 * {@code accept} hands back {@link RegistryEntry} with one type parameter and
 * takes a {@link RegistryObject} rather than NeoForge's {@code DeferredHolder}.
 *
 * <p>The navigation-target and property-tooltip helpers are not here yet — they
 * register into custom registries whose element types have not been ported. They
 * land with the Navigation Table and the block-properties tooltip.
 */
public class SimulatedRegistrate extends CreateRegistrate {

    public static final Set<String> MODS = new HashSet<>();
    public static final List<Supplier<Item>> TAB_ITEMS = Collections.synchronizedList(new ArrayList<>());
    public static final Map<ResourceLocation, ResourceLocation> ITEM_TO_SECTION = new ConcurrentHashMap<>();

    private ResourceLocation currentSection;

    public SimulatedRegistrate(final ResourceLocation initialSection, final String modId) {
        super(modId);
        this.currentSection = initialSection;
        MODS.add(modId);
    }

    public SimulatedRegistrate inSection(final ResourceLocation section) {
        this.currentSection = section;
        return this;
    }

    public static ResourceLocation sectionOf(final Item item) {
        return ITEM_TO_SECTION.get(BuiltInRegistries.ITEM.getKey(item));
    }

    @Override
    protected <R, T extends R> RegistryEntry<T> accept(
            final String name,
            final ResourceKey<? extends Registry<R>> type,
            final Builder<R, T, ?, ?> builder,
            final NonNullSupplier<? extends T> creator,
            final NonNullFunction<RegistryObject<T>, ? extends RegistryEntry<T>> entryFactory) {
        final RegistryEntry<T> entry = super.accept(name, type, builder, creator, entryFactory);

        if (type.equals(Registries.ITEM)) {
            @SuppressWarnings("unchecked")
            final RegistryEntry<? extends Item> itemEntry = (RegistryEntry<? extends Item>) entry;
            TAB_ITEMS.add(itemEntry::get);
            ITEM_TO_SECTION.put(entry.getId(), this.currentSection);
        }

        return entry;
    }

    public void addExtraItem(final ResourceLocation item) {
        TAB_ITEMS.add(() -> BuiltInRegistries.ITEM.get(item));
        ITEM_TO_SECTION.put(item, this.currentSection);
    }
}
