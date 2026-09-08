package dev.simulated_team.simulated.backport.physics.physics.config;

import dev.simulated_team.simulated.backport.physics.physics.floating_block.FloatingBlockMaterial;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;


/** The loaded floating-block materials. Empty until the data pack lands in V2. */
public final class FloatingBlockMaterialDataHandler {

    public static final Map<ResourceLocation, FloatingBlockMaterial> allMaterials = Collections.emptyMap();

    private FloatingBlockMaterialDataHandler() {
    }
}
