package dev.simulated_team.simulated.backport.physics.physics.config.block_properties;

import dev.simulated_team.simulated.backport.physics.physics.floating_block.FloatingBlockMaterial;

import java.util.function.Supplier;

/**
 * The physics properties Sable defines for a block.
 *
 * <p>The defaults are the ones the tooltip shows while the engine is absent:
 * a mass of one block-unit, vanilla-ish friction, no bounce, not fragile, and
 * no buoyancy material.
 */
public final class PhysicsBlockPropertyTypes {

    public static final Supplier<PhysicsBlockPropertyType<Double>> MASS =
            () -> new PhysicsBlockPropertyType<>("mass", 1.0);

    public static final Supplier<PhysicsBlockPropertyType<Double>> FRICTION =
            () -> new PhysicsBlockPropertyType<>("friction", 0.6);

    public static final Supplier<PhysicsBlockPropertyType<Double>> RESTITUTION =
            () -> new PhysicsBlockPropertyType<>("restitution", 0.0);

    public static final Supplier<PhysicsBlockPropertyType<Boolean>> FRAGILE =
            () -> new PhysicsBlockPropertyType<>("fragile", false);

    public static final Supplier<PhysicsBlockPropertyType<Double>> FLOATING_SCALE =
            () -> new PhysicsBlockPropertyType<>("floating_scale", 0.0);

    /**
     * The material is named, not embedded: upstream looks the name up in the
     * loaded materials. Nothing is loaded here, so it is always absent.
     */
    public static final Supplier<PhysicsBlockPropertyType<net.minecraft.resources.ResourceLocation>> FLOATING_MATERIAL =
            () -> new PhysicsBlockPropertyType<>("floating_material", null);

    private PhysicsBlockPropertyTypes() {
    }
}
