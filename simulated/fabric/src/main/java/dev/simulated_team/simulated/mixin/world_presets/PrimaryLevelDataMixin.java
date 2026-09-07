package dev.simulated_team.simulated.mixin.world_presets;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelVersion;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Remembers which world preset a save was created from, so the End Sea preset
 * survives a reload.
 *
 * <p><b>Ported by hand, not by {@code tools/port_upstream.py}.</b> 1.20.1's
 * {@code parse} carries the data fixer, the save version, the raw player tag
 * and a {@link LevelVersion} that 1.21 no longer passes; the callback has to
 * list them to match. Nothing else changes — the preset is still read off the
 * same key at the same point.
 */
@Mixin(PrimaryLevelData.class)
public class PrimaryLevelDataMixin implements PrimaryLevelDataExtension {

	@Unique
	private static final String simulated$WORLD_PRESET_KEY = "simulated:world_preset";

	@Shadow private EndDragonFight.Data endDragonFightData;
	private ResourceLocation simulated$worldPresetKey = WorldPresets.NORMAL.location();

	@Inject(method = "parse", at = @At("RETURN"))
	private static <T> void simulated$parse(final Dynamic<T> dynamic, final DataFixer dataFixer, final int version, final CompoundTag playerTag, final LevelSettings levelSettings, final LevelVersion levelVersion, final PrimaryLevelData.SpecialWorldProperty specialWorldProperty, final WorldOptions worldOptions, final Lifecycle lifecycle, final CallbackInfoReturnable<PrimaryLevelData> cir) {
		final DataResult<String> string = dynamic.get(simulated$WORLD_PRESET_KEY).asString();
		if(string.result().isPresent()) {
			((PrimaryLevelDataExtension) cir.getReturnValue()).setPreset(new ResourceLocation(string.result().orElseThrow()));
		}
	}

	@Inject(method = "setTagData", at = @At("TAIL"))
	private void simulated$setTagData(final RegistryAccess registryAccess, final CompoundTag compoundTag, final CompoundTag compoundTag2, final CallbackInfo ci) {
		compoundTag.putString(simulated$WORLD_PRESET_KEY, this.getPreset().toString());
	}

	@Override
	public ResourceLocation getPreset() {
		return this.simulated$worldPresetKey;
	}

	@Override
	public void setPreset(final ResourceLocation resourceLocation) {
		this.simulated$worldPresetKey = resourceLocation;
	}

	@Override
	public void setEndDragonFight(final EndDragonFight.Data endDragonFight) {
		this.endDragonFightData = endDragonFight;
	}
}
