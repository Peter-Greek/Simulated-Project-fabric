package dev.simulated_team.simulated.mixin.lodestone_compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.backport.core.component.SimComponents;
import dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability.LodestoneTrackingMap;
import dev.simulated_team.simulated.index.SimDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Lets a lodestone compass point at a lodestone that has moved with a
 * contraption, by tagging the compass with a tracker id the mod keeps positions
 * for.
 *
 * <p><b>Ported by hand, not by {@code tools/port_upstream.py}.</b> Upstream
 * hooks the 1.20.5 component calls — {@code ItemStack.get} inside
 * {@code inventoryTick} and {@code ItemStack.set} inside {@code useOn} — and
 * neither exists in 1.20.1, where a compass is plain NBT. The two hooks below
 * reach the same points through 1.20.1's own shapes: {@code inventoryTick} at
 * its head, since the body's own component check is the real guard either way,
 * and vanilla's private {@code addLodestoneTags}, which both branches of
 * {@code useOn} route through and which receives exactly the tag upstream's
 * {@code set} would have written to.
 */
@Mixin(CompassItem.class)
public abstract class CompassItemMixin extends Item {
	public CompassItemMixin(final Properties properties) {
		super(properties);
	}

	@Inject(method = "inventoryTick", at = @At("HEAD"))
	private void simulated$checkID(final ItemStack stack, final Level level, final Entity entity, final int itemSlot, final boolean isSelected, final CallbackInfo ci) {
		if (!level.isClientSide) {
			if (SimComponents.has(stack, SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER)) {
				final UUID trackerID = SimComponents.get(stack, SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER);
				final LodestoneTrackingMap map = LodestoneTrackingMap.getOrLoad(level);
				if (map != null && entity instanceof final ServerPlayer sp) {
					map.sendUpdateForPlayer(trackerID, sp);
				}
			}
		}
	}

	@WrapOperation(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CompassItem;addLodestoneTags(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;Lnet/minecraft/nbt/CompoundTag;)V"))
	public void simulated$setLodestoneData(final CompassItem instance, final ResourceKey<Level> dimension, final BlockPos lodestonePos, final CompoundTag itemTag, final Operation<Void> original, @Local(argsOnly = true) final UseOnContext context) {
		final BlockPos pos = context.getClickedPos();
		final LodestoneTrackingMap map = LodestoneTrackingMap.getOrLoad(context.getLevel());
		if (map != null) {
			final UUID uuid = map.addOrGetLodestoneTrackingPoint(pos);
			if (uuid != null) {
				SimComponents.setOnTag(itemTag, SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER, uuid);
			}
		}

		original.call(instance, dimension, lodestonePos, itemTag);
	}
}
