package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/**
 * Draws the wheel rim, which upstream keeps out of the blockstate model because
 * it turns and because it overhangs its own block.
 *
 * <p>Cut down from upstream's renderer: no kinetic shaft stub, no plank-material
 * swapping, and no Flywheel visual. Because there is no visual, this does not
 * bail out when visualisation is supported — it is the only thing drawing the
 * rim on any backend. The transform is upstream's, unchanged.
 */
public class SteeringWheelRenderer implements BlockEntityRenderer<SteeringWheelBlockEntity> {

    public SteeringWheelRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(final SteeringWheelBlockEntity be,
                       final float partialTicks,
                       final PoseStack ms,
                       final MultiBufferSource buffer,
                       final int light,
                       final int overlay) {
        final boolean floor = be.getBlockState().getValue(SteeringWheelBlock.ON_FLOOR);
        final Direction facing = be.getBlockState().getValue(SteeringWheelBlock.FACING);

        final SuperByteBuffer model = CachedBuffers.partial(SimPartialModels.STEERING_WHEEL, be.getBlockState());

        model.rotateCentered(facing.getRotation());
        if (floor) {
            model.translate(0, 6.5 / 16f, -5 / 16f);
        } else {
            model.translate(0, 6.5 / 16f, 5 / 16f);
        }
        model.rotateCentered(be.getRenderAngle(partialTicks), Direction.UP);

        model.light(light);
        model.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    /** The rim overhangs its block, so it must survive being partly off screen. */
    @Override
    public boolean shouldRenderOffScreen(final SteeringWheelBlockEntity be) {
        return true;
    }
}
