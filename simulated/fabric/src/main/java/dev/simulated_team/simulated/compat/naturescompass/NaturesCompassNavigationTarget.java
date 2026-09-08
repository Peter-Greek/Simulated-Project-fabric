package dev.simulated_team.simulated.compat.naturescompass;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Points the Navigation Table at whatever a Nature's Compass last found.
 *
 * <p>Upstream reads two data components off the stack. Nature's Compass only
 * grew those on 1.20.5+; its 1.20.1 build keeps the found coordinates in the
 * stack's NBT under {@code FoundX} and {@code FoundZ}, so that is what is read
 * here.
 *
 * <p>Reading the tag rather than the mod's own field also makes this compat
 * genuinely soft: nothing here links against Nature's Compass at all, so the
 * class loads whether or not the mod is installed, and simply finds nothing.
 */
public class NaturesCompassNavigationTarget implements NavigationTarget {

    @Override
    public @Nullable Vec3 getTarget(final NavTableBlockEntity navBE, final ItemStack self) {
        final CompoundTag tag = self.getTag();
        if (tag == null || !tag.contains("FoundX") || !tag.contains("FoundZ")) {
            return null;
        }

        final Vec3 pos = navBE.getProjectedSelfPos();
        return new Vec3(tag.getInt("FoundX"), pos.y(), tag.getInt("FoundZ"));
    }
}
