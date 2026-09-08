package dev.simulated_team.simulated.backport.physics.index;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import javax.annotation.Nullable;

/**
 * Sable's entity attributes. Nothing registers them here, so the holders are
 * null and upstream's guarded reads fall through.
 */
public final class SableAttributes {

    @Nullable
    public static final Holder<Attribute> SUB_LEVEL_GRAVITY = null;

    @Nullable
    public static final Holder<Attribute> PUNCH_STRENGTH = null;

    @Nullable
    public static final Holder<Attribute> PUNCH_COOLDOWN = null;

    private SableAttributes() {
    }
}
