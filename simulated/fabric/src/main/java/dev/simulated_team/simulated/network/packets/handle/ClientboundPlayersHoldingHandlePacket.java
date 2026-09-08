package dev.simulated_team.simulated.network.packets.handle;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.handle.PlayerHoldingHandleRenderer;
import dev.simulated_team.simulated.backport.net.ClientPacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public record ClientboundPlayersHoldingHandlePacket(Collection<UUID> uuids) implements CustomPacketPayload {
	public static Type<ClientboundPlayersHoldingHandlePacket> TYPE = new Type<>(Simulated.path("players_holding_handles"));
	public static final StreamCodec<ByteBuf, ClientboundPlayersHoldingHandlePacket> CODEC = StreamCodec.composite(
		ByteBufCodecs.collection(HashSet::new, SimCodecs.UUID_CODEC), ClientboundPlayersHoldingHandlePacket::uuids,
	    ClientboundPlayersHoldingHandlePacket::new
	);

	public void handle(final ClientPacketContext context) {
		PlayerHoldingHandleRenderer.updatePlayerList(this.uuids);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
