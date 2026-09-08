package dev.simulated_team.simulated.network.packets.contraption_diagram;

import dev.simulated_team.simulated.backport.physics.Sable;
import dev.simulated_team.simulated.backport.physics.api.SubLevelHelper;
import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.entities.diagram.DiagramConfig;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen;
import dev.simulated_team.simulated.backport.net.ClientPacketContext;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public record DiagramOpenPacket(int entityID, DiagramConfig config) implements CustomPacketPayload {
    public static final Type<DiagramOpenPacket> TYPE = new Type<>(Simulated.path("open_diagram"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiagramOpenPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, DiagramOpenPacket::entityID,
            DiagramConfig.STREAM_CODEC, DiagramOpenPacket::config,
            DiagramOpenPacket::new
    );

    public void handle(final ClientPacketContext context) {
        final Level level = context.level();
        assert level != null;

        final Entity entity = level.getEntity(this.entityID());

        if (entity instanceof final DiagramEntity diagram) {
            final SubLevel subLevel = Sable.HELPER.getContaining(diagram);
            if (subLevel == null) return;

            DiagramScreen.open(diagram, this.config, subLevel);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
