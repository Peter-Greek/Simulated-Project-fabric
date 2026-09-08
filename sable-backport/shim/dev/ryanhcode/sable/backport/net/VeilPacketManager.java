package dev.ryanhcode.sable.backport.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Stand-in for {@code foundry.veil.api.network.VeilPacketManager}, which does not
 * exist on Veil's 1.20.1 line.
 *
 * <p>Sable uses six members of it: {@code create}, {@code registerClientbound},
 * {@code registerServerbound}, {@code PacketSink}, {@code player} and
 * {@code server}. Each maps onto Fabric's own play-networking API, with the
 * payload id used directly as the plugin-message channel.
 *
 * <p>Like Veil, handlers are dispatched on the game thread rather than the netty
 * thread, so a handler may touch level state without its own {@code execute}.
 */
public final class VeilPacketManager {

    @FunctionalInterface
    public interface Handler<T> {
        void handle(T packet, PacketContext context);
    }

    /** A destination for outbound packets. */
    @FunctionalInterface
    public interface PacketSink {
        void sendPacket(CustomPacketPayload packet);
    }

    private static final Map<ResourceLocation, StreamCodec<? super RegistryFriendlyByteBuf, ?>> CLIENTBOUND_CODECS =
            new HashMap<>();
    private static final Map<ResourceLocation, StreamCodec<? super RegistryFriendlyByteBuf, ?>> SERVERBOUND_CODECS =
            new HashMap<>();

    private VeilPacketManager() {
    }

    /**
     * The mod id and protocol version are Veil registration bookkeeping. Fabric
     * keys channels by the payload id alone, so nothing here needs them; they are
     * accepted so the call sites stay unchanged.
     */
    public static VeilPacketManager create(final String modId, final String protocolVersion) {
        return new VeilPacketManager();
    }

    public <T extends CustomPacketPayload> void registerClientbound(
            final CustomPacketPayload.Type<T> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            final Handler<T> handler) {
        CLIENTBOUND_CODECS.put(type.id(), codec);
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientTransport.receive(type, codec, handler);
        }
    }

    public <T extends CustomPacketPayload> void registerServerbound(
            final CustomPacketPayload.Type<T> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            final Handler<T> handler) {
        SERVERBOUND_CODECS.put(type.id(), codec);
        ServerPlayNetworking.registerGlobalReceiver(type.id(), (server, player, listener, buf, responseSender) -> {
            final T packet = codec.decode(wrap(buf, server.registryAccess()));
            server.execute(() -> handler.handle(packet, context(player.level(), player)));
        });
    }

    /** Server to client: everything this player should see. */
    public static PacketSink player(final Player player) {
        if (!(player instanceof final ServerPlayer serverPlayer)) {
            return packet -> {
            };
        }
        return packet -> ServerPlayNetworking.send(serverPlayer, packet.type().id(),
                encode(packet, serverPlayer.server.registryAccess()));
    }

    /** Client to server. */
    public static PacketSink server() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return packet -> {
            };
        }
        return ClientTransport.sink();
    }

    static RegistryFriendlyByteBuf wrap(final FriendlyByteBuf buf, final RegistryAccess registryAccess) {
        return new RegistryFriendlyByteBuf(buf, registryAccess);
    }

    @SuppressWarnings("unchecked")
    static FriendlyByteBuf encode(final CustomPacketPayload packet, final RegistryAccess registryAccess) {
        final ResourceLocation id = packet.type().id();
        StreamCodec<? super RegistryFriendlyByteBuf, ?> codec = CLIENTBOUND_CODECS.get(id);
        if (codec == null) {
            codec = SERVERBOUND_CODECS.get(id);
        }
        if (codec == null) {
            throw new IllegalStateException("No Sable stream codec registered for " + id);
        }
        final FriendlyByteBuf buf = PacketByteBufs.create();
        ((StreamCodec<RegistryFriendlyByteBuf, CustomPacketPayload>) codec)
                .encode(new RegistryFriendlyByteBuf(buf, registryAccess), packet);
        return buf;
    }

    static PacketContext context(final Level level, final Player player) {
        return new PacketContext() {
            @Override
            public Level level() {
                return level;
            }

            @Override
            public Player player() {
                return player;
            }
        };
    }

    /**
     * Client-only transport, kept in a nested class so a dedicated server never
     * loads {@code net.minecraft.client} while registering clientbound packets.
     */
    private static final class ClientTransport {

        static <T extends CustomPacketPayload> void receive(
                final CustomPacketPayload.Type<T> type,
                final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                final Handler<T> handler) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                    type.id(), (client, listener, buf, responseSender) -> {
                        final T packet = codec.decode(wrap(buf, listener.registryAccess()));
                        client.execute(() -> handler.handle(packet, context(client.level, client.player)));
                    });
        }

        static PacketSink sink() {
            return packet -> {
                final net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                final RegistryAccess registryAccess = client.level == null
                        ? RegistryAccess.EMPTY
                        : client.level.registryAccess();
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        packet.type().id(), encode(packet, registryAccess));
            };
        }
    }
}
