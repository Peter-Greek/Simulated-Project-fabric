package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.network.PacketValidation;
import dev.simulated_team.simulated.backport.physics.Sable;
import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlockEntity;
import dev.simulated_team.simulated.content.items.spring.SpringItem;
import dev.simulated_team.simulated.content.items.spring.SpringItemHandler;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import dev.simulated_team.simulated.backport.net.SimCodecs;
import net.minecraft.util.Mth;

public record PlaceSpringPacket(BlockPos parentPos, BlockPos childPos, Direction parentFacing, Direction childFacing,
                                InteractionHand hand) implements CustomPacketPayload {

    public static Type<PlaceSpringPacket> TYPE = new Type<>(Simulated.path("place_spring"));

    public static StreamCodec<RegistryFriendlyByteBuf, PlaceSpringPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, (packet) -> packet.hand().ordinal(),
            SimCodecs.BLOCK_POS, PlaceSpringPacket::parentPos,
            SimCodecs.BLOCK_POS, PlaceSpringPacket::childPos,
            SimCodecs.DIRECTION, PlaceSpringPacket::parentFacing,
            SimCodecs.DIRECTION, PlaceSpringPacket::childFacing,
            (hand, parentPos, childPos, parentFacing, childFacing) -> new PlaceSpringPacket(parentPos, childPos, parentFacing, childFacing, hand == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(final ServerPacketContext ctx) {
        if (!PacketValidation.canInteract(ctx.player(), this.parentPos, 40) || !PacketValidation.canInteract(ctx.player(), this.childPos)) return;
        final ServerPlayer player = ctx.player();
        final Level level = ctx.level();

        final BlockPos parentRelative = this.parentPos().relative(this.parentFacing);
        final BlockPos childRelative = this.childPos().relative(this.childFacing);
        if (parentRelative.equals(childRelative)
                || !PacketValidation.canInteract(player, parentRelative, 40)
                || !PacketValidation.canInteract(player, childRelative)
                || !player.mayUseItemAt(parentRelative, this.parentFacing, player.getItemInHand(this.hand))
                || !player.mayUseItemAt(childRelative, this.childFacing, player.getItemInHand(this.hand))
                || !level.getBlockState(parentRelative).canBeReplaced()
                || !level.getBlockState(childRelative).canBeReplaced()
                || level.getBlockState(this.parentPos).isAir()
                || level.getBlockState(this.childPos).isAir()) return;
        final BlockState previousParent = level.getBlockState(parentRelative);
        final BlockState previousChild = level.getBlockState(childRelative);

        final ItemStack spring = player.getItemInHand(this.hand);
        final double distanceSquared = Sable.HELPER.distanceSquaredWithSubLevels(level, parentRelative.getCenter(), childRelative.getCenter());
        if (!(spring.getItem() instanceof SpringItem) || distanceSquared > (SpringItemHandler.MAX_LENGTH + 1) * (SpringItemHandler.MAX_LENGTH + 1)) {
            return;
        }

        final SpringBlockEntity controllerSpring = this.addSpring(level, parentRelative, childRelative, this.parentFacing(), true, (float) distanceSquared);
        final SpringBlockEntity partnerSpring = this.addSpring(level, childRelative, parentRelative, this.childFacing(), false, (float) distanceSquared);

        if (controllerSpring == null || partnerSpring == null) {
            level.setBlockAndUpdate(parentRelative, previousParent);
            level.setBlockAndUpdate(childRelative, previousChild);
            return;
        }

        final double distance = Mth.clamp(Math.sqrt(distanceSquared) + 1, 1, SpringItemHandler.MAX_LENGTH);
        controllerSpring.setDesiredLength(distance);
        partnerSpring.setDesiredLength(distance);

        player.awardStat(Stats.ITEM_USED.get(spring.getItem()));
        if (!player.getAbilities().instabuild) {
            spring.shrink(1);
        }
    }

    private SpringBlockEntity addSpring(final Level level, final BlockPos placedPos, final BlockPos childPos, final Direction facing, final boolean controller, final float distance) {
        final BlockState newState = SimBlocks.SPRING.getDefaultState();

        if (level.setBlockAndUpdate(placedPos, newState.setValue(SpringBlock.FACING, facing))) {
            final SpringBlockEntity parentSpring = (SpringBlockEntity) level.getBlockEntity(placedPos);

            if (parentSpring == null) return null;
            parentSpring.setController(controller);

            final SubLevel subLevel = Sable.HELPER.getContaining(level, childPos);
            parentSpring.setPartnerPos(childPos, subLevel != null ? subLevel.getUniqueId() : null);

            parentSpring.notifyUpdate();
            return parentSpring;
        }
        return null;
    }
}
