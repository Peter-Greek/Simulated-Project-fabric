package dev.simulated_team.simulated.backport.physics.physics.floating_block;

import net.minecraft.resources.ResourceLocation;


/** How a block behaves in a fluid. Read only by the buoyancy pass, which is V2. */
public record FloatingBlockMaterial(ResourceLocation id, double density, double liftStrength) {

    public FloatingBlockMaterial(final ResourceLocation id, final double density) {
        this(id, density, 0.0);
    }
}
