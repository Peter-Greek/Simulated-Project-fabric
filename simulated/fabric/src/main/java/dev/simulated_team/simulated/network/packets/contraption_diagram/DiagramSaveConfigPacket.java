package dev.simulated_team.simulated.network.packets.contraption_diagram;

import dev.simulated_team.simulated.backport.physics.Sable;
import dev.simulated_team.simulated.backport.physics.api.SubLevelHelper;
import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.DiagramConfig;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public record DiagramSaveConfigPacket(int entityID, DiagramConfig config) implements CustomPacketPayload {
    public static final Type<DiagramSaveConfigPacket> TYPE = new Type<>(Simulated.path("save_diagram"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiagramSaveConfigPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DiagramSaveConfigPacket::entityID,
            DiagramConfig.STREAM_CODEC, DiagramSaveConfigPacket::config,
            DiagramSaveConfigPacket::new
    );

    public void handle(final ServerPacketContext context) {
        final Level level = context.level();

        final Entity entity = level.getEntity(this.entityID());

        if (entity instanceof final DiagramEntity diagram && dev.simulated_team.simulated.network.PacketValidation.canInteract(context.player(), entity)) {

            diagram.setConfig(this.config);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
