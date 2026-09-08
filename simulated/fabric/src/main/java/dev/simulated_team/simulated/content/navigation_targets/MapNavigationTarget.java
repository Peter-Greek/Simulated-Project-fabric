package dev.simulated_team.simulated.content.navigation_targets;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Points the Navigation Table at the nearest marker on a filled map.
 *
 * <p>Upstream reads the markers from the stack's {@code MapDecorations}
 * component, which is 1.20.5+. On 1.20.1 a map's markers live in the map's own
 * saved data on the server, so that is where they are read from here. The result
 * is the same set of markers — and the banner pass below already worked this way
 * upstream — with one consequence worth naming: the saved data is server-side,
 * so a map held on a client with no loaded map data finds nothing until the
 * server sends it, which is also when vanilla would draw the markers.
 */
public class MapNavigationTarget implements NavigationTarget {

    @Override
    public @Nullable Vec3 getTarget(final NavTableBlockEntity navBE, final ItemStack self) {
        final Level level = navBE.getLevel();
        final Vec3 pos = navBE.getProjectedSelfPos();
        return getNearestDecorationPos(level, pos, self);
    }

    private static @Nullable Vec3 getNearestDecorationPos(final Level level, final Vec3 pos, final ItemStack stack) {
        final MapItemSavedData mapData = MapItem.getSavedData(stack, level);
        if (mapData == null) {
            return null;
        }

        double closestDist = Double.POSITIVE_INFINITY;
        Vec3 closestPos = null;

        for (final MapDecoration decoration : mapData.getDecorations()) {
            if (!isFindable(decoration)) {
                continue;
            }

            // Decoration coordinates are map-space bytes; the map's own centre
            // and scale turn them back into world coordinates.
            final double worldX = mapData.centerX + decoration.getX() / 2.0D * (1 << mapData.scale);
            final double worldZ = mapData.centerZ + decoration.getY() / 2.0D * (1 << mapData.scale);

            final double dist = pos.distanceToSqr(worldX, pos.y(), worldZ);
            if (dist < closestDist) {
                closestPos = new Vec3(worldX, pos.y(), worldZ);
                closestDist = dist;
            }
        }

        for (final MapBanner banner : mapData.getBanners()) {
            final Vec3 bannerPos = banner.getPos().getCenter();
            final double dist = pos.distanceToSqr(bannerPos.x(), pos.y(), bannerPos.z());
            if (dist < closestDist) {
                closestPos = bannerPos;
                closestDist = dist;
            }
        }

        return closestPos;
    }

    /**
     * 1.20.1's decoration type is an enum, not a registry entry, so the tag
     * upstream filters on cannot be applied to it. The tag's contents are the
     * marker types worth navigating to, which on this version is every marker
     * that is not the player themselves.
     */
    private static boolean isFindable(final MapDecoration decoration) {
        final MapDecoration.Type type = decoration.getType();
        return type != MapDecoration.Type.PLAYER
                && type != MapDecoration.Type.PLAYER_OFF_MAP
                && type != MapDecoration.Type.PLAYER_OFF_LIMITS
                && type != MapDecoration.Type.FRAME;
    }
}
