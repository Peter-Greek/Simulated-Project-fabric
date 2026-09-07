package dev.simulated_team.simulated.index;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.simulated_team.simulated.Simulated;

/**
 * Model parts drawn by renderers rather than by a blockstate. Ported entry by
 * entry alongside the blocks that use them.
 */
public class SimPartialModels {

    public static final PartialModel STEERING_WHEEL = block("steering_wheel/wheel");

    private static PartialModel block(final String path) {
        return PartialModel.of(Simulated.path("block/" + path));
    }

    public static void init() {
    }
}
