package dev.simulated_team.simulated.backport.physics.api.physics.force;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * The named groups a force can belong to, which is how the Contraption Diagram
 * separates gravity from lift from propulsion.
 *
 * <p>The groups themselves are real — they are just names with a default
 * visibility, and the diagram reads them to build its legend. What is missing
 * until V2 is any force actually being filed under one.
 */
public final class ForceGroups {

    public static final ResourceKey<Registry<ForceGroup>> KEY =
            ResourceKey.createRegistryKey(new ResourceLocation("sable", "force_group"));

    public static final Registry<ForceGroup> REGISTRY = new MappedRegistry<>(KEY, com.mojang.serialization.Lifecycle.stable());

    public static final ForceGroup GRAVITY = add("gravity", true);
    public static final ForceGroup DRAG = add("drag", true);
    public static final ForceGroup LIFT = add("lift", true);
    public static final ForceGroup PROPULSION = add("propulsion", true);
    public static final ForceGroup MAGNETIC_FORCE = add("magnetic_force", false);
    public static final ForceGroup LEVITATION = add("levitation", false);

    private ForceGroups() {
    }

    /** Upstream reads some of these through a supplier; both spellings work. */
    public static java.util.function.Supplier<ForceGroup> supplier(final ForceGroup group) {
        return () -> group;
    }

    public static ForceGroup add(final String name, final boolean defaultDisplayed) {
        final ResourceLocation id = new ResourceLocation("sable", name);
        return Registry.register(REGISTRY, id, new ForceGroup(id, defaultDisplayed));
    }
}
