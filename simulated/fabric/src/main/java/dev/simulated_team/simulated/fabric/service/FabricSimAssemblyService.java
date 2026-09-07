package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.service.SimAssemblyService;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/**
 * Whether two blocks stay together when one is moved.
 *
 * <p>NeoForge asks the block itself through {@code canStickTo}. 1.20.1 Fabric has
 * no such hook, so this is vanilla's own rule, which is what that hook defaults
 * to: neither block may be one that refuses to be pushed or pulled.
 */
public class FabricSimAssemblyService implements SimAssemblyService {

    @Override
    public boolean canStickTo(final BlockState stateA, final BlockState stateB) {
        if (stateA.getPistonPushReaction() == PushReaction.BLOCK
                || stateB.getPistonPushReaction() == PushReaction.BLOCK) {
            return false;
        }
        return stateA.getPistonPushReaction() != PushReaction.DESTROY
                && stateB.getPistonPushReaction() != PushReaction.DESTROY;
    }
}
