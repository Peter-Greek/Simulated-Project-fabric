package dev.simulated_team.simulated.compat.explorerscompass;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Points the Navigation Table at whatever an Explorer's Compass last found.
 *
 * <p>Same shape as
 * {@link dev.simulated_team.simulated.compat.naturescompass.NaturesCompassNavigationTarget}:
 * the components upstream reads are 1.20.5+, and the 1.20.1 build of the mod
 * keeps the coordinates in the stack's NBT instead.
 */
public class ExplorersCompassNavigationTarget implements NavigationTarget {

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
