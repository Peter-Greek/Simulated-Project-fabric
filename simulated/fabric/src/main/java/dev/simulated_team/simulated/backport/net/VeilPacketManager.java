package dev.simulated_team.simulated.backport.net;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Stand-in for {@code foundry.veil.api.network.VeilPacketManager}, which does
 * not exist on Veil's 1.20.1 line.
 *
 * <p>Simulated uses {@code create}, {@code registerClientbound},
 * {@code registerServerbound}, {@code PacketSink}, {@code player},
 * {@code server}, {@code all} and {@code tracking}. Each maps onto Fabric's own
 * play-networking API, with the payload id used directly as the plugin-message
 * channel.
 *
 * <p>Like Veil, handlers are dispatched on the game thread rather than the netty
 * thread, so a handler may touch level state without its own {@code execute}.
 */
public final class VeilPacketManager {

    @FunctionalInterface
    public interface ServerHandler<T> {
        void handle(T packet, ServerPacketContext context);
    }

    @FunctionalInterface
    public interface ClientHandler<T> {
        void handle(T packet, ClientPacketContext context);
    }

    /** A destination for outbound packets. */
    @FunctionalInterface
    public interface PacketSink {
        void sendPacket(CustomPacketPayload packet);
    }

    /** Drops everything; returned wherever the requested destination is not reachable. */
    private static final PacketSink NOOP = packet -> {
    };

    private static final Map<ResourceLocation, StreamCodec<? super RegistryFriendlyByteBuf, ?>> CODECS =
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
            final ClientHandler<T> handler) {
        CODECS.put(type.id(), codec);
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientTransport.receive(type, codec, handler);
        }
    }

    public <T extends CustomPacketPayload> void registerServerbound(
            final CustomPacketPayload.Type<T> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            final ServerHandler<T> handler) {
        CODECS.put(type.id(), codec);
        ServerPlayNetworking.registerGlobalReceiver(type.id(), (server, player, listener, buf, responseSender) -> {
            if (buf.readableBytes() > 65536) return;
            final T packet;
            try {
                packet = codec.decode(wrap(buf, server.registryAccess()));
                if (buf.isReadable()) return;
            } catch (RuntimeException malformed) {
                return;
            }
            server.execute(() -> {
                if (!player.isRemoved() && player.connection == listener) {
                    handler.handle(packet, serverContext(player));
                }
            });
        });
    }

    /** Server to client: one player. */
    public static PacketSink player(final Player player) {
        if (!(player instanceof final ServerPlayer serverPlayer)) {
            return NOOP;
        }
        return packet -> ServerPlayNetworking.send(serverPlayer, packet.type().id(),
                encode(packet, serverPlayer.server.registryAccess()));
    }

    /** Server to client: every player on the server. */
    public static PacketSink all(final MinecraftServer server) {
        if (server == null) {
            return NOOP;
        }
        return packet -> {
            final FriendlyByteBuf buf = encode(packet, server.registryAccess());
            for (final ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(serverPlayer, packet.type().id(), PacketByteBufs.copy(buf));
            }
        };
    }

    /** Server to client: every player whose client is tracking this block entity. */
    public static PacketSink tracking(final BlockEntity blockEntity) {
        final Level level = blockEntity.getLevel();
        if (!(level instanceof final ServerLevel serverLevel)) {
            return NOOP;
        }
        return packet -> {
            final FriendlyByteBuf buf = encode(packet, serverLevel.registryAccess());
            for (final ServerPlayer serverPlayer : PlayerLookup.tracking(serverLevel, blockEntity.getBlockPos())) {
                ServerPlayNetworking.send(serverPlayer, packet.type().id(), PacketByteBufs.copy(buf));
            }
        };
    }

    /**
     * Server to client: every player tracking this entity. Veil includes the
     * entity itself when it is a player, which {@link PlayerLookup#tracking}
     * does not, so it is added back.
     */
    public static PacketSink tracking(final Entity entity) {
        if (!(entity.level() instanceof final ServerLevel serverLevel)) {
            return NOOP;
        }
        return packet -> {
            final FriendlyByteBuf buf = encode(packet, serverLevel.registryAccess());
            for (final ServerPlayer serverPlayer : PlayerLookup.tracking(entity)) {
                ServerPlayNetworking.send(serverPlayer, packet.type().id(), PacketByteBufs.copy(buf));
            }
            if (entity instanceof final ServerPlayer self) {
                ServerPlayNetworking.send(self, packet.type().id(), PacketByteBufs.copy(buf));
            }
        };
    }

    /** Client to server. */
    public static PacketSink server() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return NOOP;
        }
        return ClientTransport.sink();
    }

    static RegistryFriendlyByteBuf wrap(final FriendlyByteBuf buf, final RegistryAccess registryAccess) {
        return new RegistryFriendlyByteBuf(buf, registryAccess);
    }

    @SuppressWarnings("unchecked")
    static FriendlyByteBuf encode(final CustomPacketPayload packet, final RegistryAccess registryAccess) {
        final ResourceLocation id = packet.type().id();
        final StreamCodec<? super RegistryFriendlyByteBuf, ?> codec = CODECS.get(id);
        if (codec == null) {
            throw new IllegalStateException("No stream codec registered for " + id);
        }
        final FriendlyByteBuf buf = PacketByteBufs.create();
        ((StreamCodec<RegistryFriendlyByteBuf, CustomPacketPayload>) codec)
                .encode(new RegistryFriendlyByteBuf(buf, registryAccess), packet);
        return buf;
    }

    static ServerPacketContext serverContext(final ServerPlayer player) {
        return new ServerPacketContext() {
            @Override
            public Level level() {
                return player.level();
            }

            @Override
            public ServerPlayer player() {
                return player;
            }

            @Override
            public void disconnect(final Component reason) {
                player.connection.disconnect(reason);
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
                final ClientHandler<T> handler) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                    type.id(), (client, listener, buf, responseSender) -> {
                        final T packet = codec.decode(wrap(buf, listener.registryAccess()));
                        client.execute(() -> {
                            if (client.level != null && client.player != null && client.getConnection() == listener)
                                handler.handle(packet, clientContext());
                        });
                    });
        }

        static ClientPacketContext clientContext() {
            final net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            return new ClientPacketContext() {
                @Override
                public Level level() {
                    return client.level;
                }

                @Override
                public net.minecraft.client.player.LocalPlayer player() {
                    return client.player;
                }

                @Override
                public void disconnect(final Component reason) {
                    if (client.getConnection() != null) {
                        client.getConnection().getConnection().disconnect(reason);
                    }
                }
            };
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
