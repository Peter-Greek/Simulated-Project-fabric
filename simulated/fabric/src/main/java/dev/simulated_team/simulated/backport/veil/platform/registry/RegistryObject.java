package dev.simulated_team.simulated.backport.veil.platform.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * A registered entry. Fabric registers eagerly, so unlike NeoForge's deferred
 * holder the value is already present by the time this exists.
 */
public record RegistryObject<T>(ResourceLocation id, T value) implements Supplier<T> {

    @Override
    public T get() {
        return this.value;
    }

    public ResourceLocation getId() {
        return this.id;
    }
}
