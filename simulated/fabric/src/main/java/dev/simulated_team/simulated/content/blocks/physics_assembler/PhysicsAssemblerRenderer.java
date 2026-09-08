package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlock;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

/**
 * Draws the assembler's lever.
 *
 * <p>Upstream extends Create's {@code SmartBlockEntityRenderer}, which requires
 * the block entity to be a Create {@code SmartBlockEntity}. This port's
 * assembler is a plain block entity while it rides the Create contraption
 * stand-in, so the renderer is a plain one; the drawing below is upstream's,
 * unchanged.
 */
public class PhysicsAssemblerRenderer implements BlockEntityRenderer<PhysicsAssemblerBlockEntity> {

    public PhysicsAssemblerRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(final PhysicsAssemblerBlockEntity be, final float partialTicks, final PoseStack ms, final MultiBufferSource buffer, final int light, final int overlay) {
        final BlockState blockState = be.getBlockState();
        final VertexConsumer vb = buffer.getBuffer(RenderType.solid());

        // Render handle
        final SuperByteBuffer handle = CachedBuffers.partial(SimPartialModels.ASSEMBLER_LEVER, blockState);
        final float angle = getRenderAngle(be, partialTicks);
        this.transform(handle, blockState).translate(1 / 2f, 7 / 16f, 1 / 2f)
                .rotate(angle, Direction.EAST)
                .translate(-1 / 2f, -7 / 16f, -1 / 2f);
        handle.light(light)
                .renderInto(ms, vb);
    }

    public static float getRenderAngle(final PhysicsAssemblerBlockEntity be, final float partialTicks) {
        if (!be.isVirtual()) {
            be.initializeLeverPosition();
        }

        return (float) Math.toRadians(be.getClientAngle(partialTicks));
    }

    private SuperByteBuffer transform(final SuperByteBuffer buffer, final BlockState leverState) {
        final AttachFace face = leverState.getValue(AnalogLeverBlock.FACE);
        final float rX = face == AttachFace.FLOOR ? 0 : face == AttachFace.WALL ? 90 : 180;
        final float rY = AngleHelper.horizontalAngle(leverState.getValue(AnalogLeverBlock.FACING));
        buffer.rotateCentered((float) (rY / 180 * Math.PI), Direction.UP);
        buffer.rotateCentered((float) (rX / 180 * Math.PI), Direction.EAST);
        return buffer;
    }
}
