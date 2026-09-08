package dev.simulated_team.simulated.mixin.torsion_spring;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.api.IDirectionalAnalogOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets a block answer a comparator differently depending on which side is asking.
 *
 * <p>The direction is addressed by ordinal, not by name. A name-addressed
 * {@code @Local} into a vanilla class resolves in development, where Loom supplies
 * a jar carrying mapped local names, and fails at runtime, where the local table
 * survives but its names are obfuscated. {@code getInputSignal} has exactly one
 * {@code Direction} in scope at the call this wraps, so ordinal 0 is that local in
 * both environments.
 */
@Mixin(ComparatorBlock.class)
public class ComparatorBlockMixin {
    @WrapOperation(method = "getInputSignal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getAnalogOutputSignal(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"))
    private int simulated$potentiallyDirectionalAnalogueSignal(final BlockState instance, final Level level, final BlockPos pos, final Operation<Integer> original, @Local(ordinal = 0) final Direction direction) {
        if (instance.getBlock() instanceof final IDirectionalAnalogOutput directionalAnalogOutput) {
            return directionalAnalogOutput.getAnalogOutputSignalFrom(instance, level, pos, direction);
        }
        return original.call(instance, level, pos);
    }
}
