package dev.simulated_team.simulated.index;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelRenderer;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;

public class SimBlockEntityTypes {
    private static final SimulatedRegistrate REGISTRATE = Simulated.getRegistrate();

    public static final BlockEntityEntry<PhysicsAssemblerBlockEntity> PHYSICS_ASSEMBLER =
            REGISTRATE.blockEntity("physics_assembler", PhysicsAssemblerBlockEntity::new)
                    .validBlocks(SimBlocks.PHYSICS_ASSEMBLER, SimBlocks.PHYSICS_ASSEMBLER_ANCHOR)
                    .register();

    public static final BlockEntityEntry<SteeringWheelBlockEntity> STEERING_WHEEL =
            REGISTRATE.blockEntity("steering_wheel", SteeringWheelBlockEntity::new)
                    .validBlocks(SimBlocks.STEERING_WHEEL)
                    .renderer(() -> SteeringWheelRenderer::new)
                    .register();

    public static void register() {
    }
}
