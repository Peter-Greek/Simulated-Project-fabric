package dev.simulated_team.simulated.network.packets.helpers;

import dev.simulated_team.simulated.network.PacketValidation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import net.minecraft.core.BlockPos;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public abstract class SimBlockEntityConfigurationPacket<T extends SmartBlockEntity> implements CustomPacketPayload {
    private final BlockPos pos;
    private final Class<T> entityClass;

    public SimBlockEntityConfigurationPacket(final BlockPos pos, final Class<T> entityClass) {
        this.pos = pos;
        this.entityClass = entityClass;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public void handle(final ServerPacketContext context) {
        if (!PacketValidation.canInteract(context.player(), this.pos)) return;
        final ServerPlayer player = context.player();

        final Level world = player.level();
        if (world.isLoaded(this.pos)) {
            if (player.distanceToSqr(Vec3.atBottomCenterOf(this.pos)) <= this.maxRangeSqr()) {
                final BlockEntity blockEntity = world.getBlockEntity(this.pos);
                if (this.entityClass.isInstance(blockEntity)) {
                    this.applySettings(player, this.entityClass.cast(blockEntity));
                    if (!this.causeUpdate()) {
                        return;
                    }

                    ((SyncedBlockEntity)blockEntity).sendData();
                    blockEntity.setChanged();
                }
            }
        }
    }

    protected int maxRangeSqr() {
        return 20;
    }

    protected boolean causeUpdate() {
        return true;
    }

    protected abstract void applySettings(ServerPlayer player, T blockEntity);
}
