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
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import net.minecraft.world.level.ItemLike;
import dev.simulated_team.simulated.index.SimRegistries;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.client.BlockPropertiesTooltip;

/**
 * Upstream's Registrate subclass, on the 1.20.1 Registrate API.
 *
 * <p>Differences from upstream, all forced by the older Registrate:
 * {@code accept} hands back {@link RegistryEntry} with one type parameter and
 * takes a {@link RegistryObject} rather than NeoForge's {@code DeferredHolder}.
 *
 * <p>The navigation-target and property-tooltip helpers register into custom
 * registries. Upstream builds those through Veil's {@code RegistrationProvider};
 * here they are Fabric registries created by the replacement provider, and the
 * entries go in directly rather than through Registrate's deferred pipeline,
 * because on 1.20.1 Registrate's {@code simple} does not take an arbitrary
 * registry key.
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

    /** Which item each navigation target reads from, so it can be attached to it. */
    public static final Map<ResourceLocation, Supplier<ItemLike>> NAVIGATION_TARGET_ITEMS =
            new ConcurrentHashMap<>();

    public <T extends NavigationTarget> T navTarget(final String name, final NonNullSupplier<T> navTarget,
                                                    final Supplier<ItemLike> itemSupplier) {
        final ResourceLocation id = new ResourceLocation(this.getModid(), name);
        final T entry = Registry.register(SimRegistries.NAVIGATION_TARGET, id, navTarget.get());
        NAVIGATION_TARGET_ITEMS.put(id, itemSupplier);
        return entry;
    }

    public <T extends NavigationTarget> T navTarget(final String name, final NonNullSupplier<T> navTarget,
                                                    final ItemLike item) {
        return this.navTarget(name, navTarget, () -> item);
    }

    public <T extends BlockPropertiesTooltip.Entry> T propertyTooltip(final String name,
                                                                     final NonNullSupplier<T> tooltipFunction) {
        return Registry.register(SimRegistries.propertyTooltip(),
                new ResourceLocation(this.getModid(), name), tooltipFunction.get());
    }

    /**
     * Upstream attaches each navigation target to its item as a default data
     * component. 1.20.1 has no default components, so the Navigation Table asks
     * this map which target an item carries instead; the hook is kept so the
     * call site reads as upstream's.
     */
    public static void onAddDefaultComponents(final BiConsumer<ItemLike, Consumer<Object>> modify) {
    }

    /** The navigation target registered for this item, or null if it has none. */
    @javax.annotation.Nullable
    public static NavigationTarget navigationTargetFor(final ItemLike item) {
        for (final Map.Entry<ResourceLocation, Supplier<ItemLike>> entry : NAVIGATION_TARGET_ITEMS.entrySet()) {
            if (entry.getValue().get() == item) {
                return SimRegistries.NAVIGATION_TARGET.get(entry.getKey());
            }
        }
        return null;
    }

}
