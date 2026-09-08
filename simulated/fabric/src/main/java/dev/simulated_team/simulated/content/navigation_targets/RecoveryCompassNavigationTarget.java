package dev.simulated_team.simulated.content.navigation_targets;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimDataComponents;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.UUID;
import dev.simulated_team.simulated.backport.core.component.SimComponents;

public class RecoveryCompassNavigationTarget implements NavigationTarget {
    @Override
    public @Nullable Vec3 getTarget(final NavTableBlockEntity navBE, final ItemStack self) {
        final UUID lastPlayer = SimComponents.get(self, SimDataComponents.COMPASS_PLACER_UUID);
        if (lastPlayer != null) {
            GlobalPos lastDeathLocation;

            final Player player = navBE.getLevel().getPlayerByUUID(lastPlayer);
            if (player != null) {
                Optional<GlobalPos> lastDeathLocationOptional = player.getLastDeathLocation();
                if (lastDeathLocationOptional.isEmpty()) {
                    SimComponents.remove(self, SimDataComponents.LAST_PLAYER_DEATH_LOCATION);
                    return null;
                }

                lastDeathLocation = lastDeathLocationOptional.get();
                SimComponents.set(self, SimDataComponents.LAST_PLAYER_DEATH_LOCATION,  lastDeathLocation);
            } else {
                lastDeathLocation = SimComponents.get(self, SimDataComponents.LAST_PLAYER_DEATH_LOCATION);
            }

            if (lastDeathLocation == null) {
                return null;
            }

            final ResourceKey<Level> dimension = navBE.getLevel().dimension();
            if (!lastDeathLocation.dimension().equals(dimension)) {
                return null;
            }

            return lastDeathLocation.pos().getCenter();
        }

        return null;
    }

    @Override
    public void onInsert(final ItemStack itemStack, final NavTableBlockEntity be, @Nullable final Player player) {
        if (player != null) {
            // 1.20.5 builds a component map and applies it at once; here the
            // two values are written straight onto the stack.
            SimComponents.set(itemStack, SimDataComponents.COMPASS_PLACER_UUID, player.getUUID());
            player.getLastDeathLocation().ifPresent(globalPos ->
                    SimComponents.set(itemStack, SimDataComponents.LAST_PLAYER_DEATH_LOCATION, globalPos));
        }
    }

    @Override
    public void onExtract(final ItemStack itemStack, final NavTableBlockEntity be, @Nullable final Player player) {
        SimComponents.remove(itemStack, SimDataComponents.COMPASS_PLACER_UUID);
        SimComponents.remove(itemStack, SimDataComponents.LAST_PLAYER_DEATH_LOCATION);
    }
}
