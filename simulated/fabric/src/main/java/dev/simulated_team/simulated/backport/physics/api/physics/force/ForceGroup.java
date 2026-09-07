package dev.simulated_team.simulated.backport.physics.api.physics.force;

import net.minecraft.resources.ResourceLocation;

/** One named group of forces. See {@link ForceGroups}. */
public record ForceGroup(ResourceLocation id, boolean defaultDisplayed, int color) {

    public ForceGroup(final ResourceLocation id) {
        this(id, true, 0xFFFFFFFF);
    }

    public ForceGroup(final ResourceLocation id, final boolean defaultDisplayed) {
        this(id, defaultDisplayed, 0xFFFFFFFF);
    }

    /** The translation key the diagram legend shows. */
    public String name() {
        return "force_group." + this.id.getNamespace() + "." + this.id.getPath();
    }
}
