package dev.simulated_team.simulated.network.packets.contraption_diagram;

import dev.simulated_team.simulated.backport.physics.api.sublevel.SubLevelContainer;
import dev.simulated_team.simulated.backport.physics.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import net.minecraft.core.UUIDUtil;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.UUID;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public record RequestDiagramDataPacket(UUID subLevel) implements CustomPacketPayload {

    public static Type<RequestDiagramDataPacket> TYPE = new Type<>(Simulated.path("request_diagram_data"));

    public static StreamCodec<RegistryFriendlyByteBuf, RequestDiagramDataPacket> CODEC = StreamCodec.composite(
            SimCodecs.UUID_CODEC, RequestDiagramDataPacket::subLevel,
            RequestDiagramDataPacket::new);

    public void handle(final ServerPacketContext context) {
        final ServerPlayer player = context.player();
        final Level level = player.level();

        final SubLevelContainer container = SubLevelContainer.getContainer(level);
        assert container != null;

        final SubLevel subLevel = container.getSubLevel(this.subLevel);

        if (subLevel instanceof final ServerSubLevel serverSubLevel) {
            DiagramEntity.queueDiagramDataFor(serverSubLevel, player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
