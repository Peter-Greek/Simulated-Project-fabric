package dev.simulated_team.simulated.backport.veil.platform.registry;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Veil's loader-neutral registration handle, on Fabric's registries.
 *
 * <p>Two shapes are used: registering entries into a registry that already
 * exists ({@code get(BuiltInRegistries.SOUND_EVENT, modId)}), and creating a new
 * registry and handing it back ({@code get(key, modId).asVanillaRegistry()}).
 * Both are real here — {@link FabricRegistryBuilder} is exactly what Veil's
 * {@code RegistrationProvider} does on NeoForge through its own deferred
 * register.
 */
public class RegistrationProvider<T> {

    private final Registry<T> registry;
    private final String modId;
    private final Set<RegistryObject<T>> entries = new LinkedHashSet<>();

    private RegistrationProvider(final Registry<T> registry, final String modId) {
        this.registry = registry;
        this.modId = modId;
    }

    /** Register into an existing registry. */
    public static <T> RegistrationProvider<T> get(final Registry<T> registry, final String modId) {
        return new RegistrationProvider<>(registry, modId);
    }

    /**
     * Create a registry for a key that has none yet, and register into it. Fabric
     * has no equivalent of NeoForge's "create on first use", so the registry is
     * built here and the same instance is handed back on a repeat call.
     */
    @SuppressWarnings("unchecked")
    public static <T> RegistrationProvider<T> get(final ResourceKey<Registry<T>> key, final String modId) {
        Registry<T> registry = (Registry<T>) net.minecraft.core.registries.BuiltInRegistries.REGISTRY
                .get(key.location());
        if (registry == null) {
            registry = FabricRegistryBuilder.createSimple(key).buildAndRegister();
        }
        return new RegistrationProvider<>(registry, modId);
    }

    public RegistryObject<T> register(final String name, final Supplier<? extends T> factory) {
        final ResourceLocation id = new ResourceLocation(this.modId, name);
        final T value = Registry.register(this.registry, id, factory.get());
        final RegistryObject<T> entry = new RegistryObject<>(id, value);
        this.entries.add(entry);
        return entry;
    }

    public Registry<T> asVanillaRegistry() {
        return this.registry;
    }

    public String getModId() {
        return this.modId;
    }

    public Set<RegistryObject<T>> getEntries() {
        return Collections.unmodifiableSet(this.entries);
    }
}
