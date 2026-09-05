package dev.simulated_team.simulated.fabric;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/** Client-only hooks for the Homestead Fabric port. */
public final class SimulatedFabricClient implements ClientModInitializer {
    private static boolean flightControlActive;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                SimulatedFabricNetworking.CONTRAPTION_POSITION,
                (client, handler, buffer, responseSender) -> {
                    final int entityId = buffer.readInt();
                    final double x = buffer.readDouble();
                    final double y = buffer.readDouble();
                    final double z = buffer.readDouble();
                    final float angle = buffer.readFloat();

                    client.execute(() -> {
                        if (client.level == null) {
                            return;
                        }
                        if (client.level.getEntity(entityId) instanceof final ControlledContraptionEntity entity) {
                            entity.setRotationAxis(Direction.Axis.Y);
                            entity.setAngle(angle);
                            entity.setPos(x, y, z);
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(
                SimulatedFabricNetworking.FLIGHT_CONTROL_STATE,
                (client, handler, buffer, responseSender) -> {
                    final boolean engaged = buffer.readBoolean();
                    final String message = buffer.readUtf(256);
                    client.execute(() -> {
                        flightControlActive = engaged;
                        if (client.player != null) {
                            client.player.displayClientMessage(Component.literal(message), true);
                        }
                    });
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                flightControlActive = false;
                return;
            }
            if (!flightControlActive) {
                return;
            }

            int inputMask = 0;
            if (client.screen == null) {
                if (client.options.keyUp.isDown()) {
                    inputMask |= SimulatedFabricNetworking.FLIGHT_FORWARD;
                }
                if (client.options.keyDown.isDown()) {
                    inputMask |= SimulatedFabricNetworking.FLIGHT_BACK;
                }
                if (client.options.keyLeft.isDown()) {
                    inputMask |= SimulatedFabricNetworking.FLIGHT_LEFT;
                }
                if (client.options.keyRight.isDown()) {
                    inputMask |= SimulatedFabricNetworking.FLIGHT_RIGHT;
                }
                if (client.options.keyJump.isDown()) {
                    inputMask |= SimulatedFabricNetworking.FLIGHT_UP;
                }
                if (client.options.keySprint.isDown()) {
                    inputMask |= SimulatedFabricNetworking.FLIGHT_DOWN;
                }
            }

            final FriendlyByteBuf input = PacketByteBufs.create();
            input.writeByte(inputMask);
            ClientPlayNetworking.send(SimulatedFabricNetworking.FLIGHT_INPUT, input);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> flightControlActive = false);
    }
}
