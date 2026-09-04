package dev.simulated_team.simulated.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Client-only hooks for the Homestead Fabric port. */
public final class SimulatedFabricClient implements ClientModInitializer {
    private static KeyMapping flightToggle;
    private static KeyMapping flightForward;
    private static KeyMapping flightBack;
    private static KeyMapping flightLeft;
    private static KeyMapping flightRight;
    private static KeyMapping flightUp;
    private static KeyMapping flightDown;
    private static boolean flightControlActive;

    @Override
    public void onInitializeClient() {
        registerFlightKeys();

        ClientPlayNetworking.registerGlobalReceiver(
                SimulatedFabricNetworking.CONTRAPTION_POSITION,
                (client, handler, buffer, responseSender) -> {
                    final int entityId = buffer.readInt();
                    final double x = buffer.readDouble();
                    final double y = buffer.readDouble();
                    final double z = buffer.readDouble();

                    client.execute(() -> {
                        if (client.level == null) {
                            return;
                        }
                        if (client.level.getEntity(entityId) instanceof final ControlledContraptionEntity entity) {
                            // ControlledContraptionEntity deliberately ignores vanilla lerp/teleport
                            // updates. Apply the authoritative server position directly on the client.
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

            while (flightToggle.consumeClick()) {
                final FriendlyByteBuf toggle = PacketByteBufs.create();
                ClientPlayNetworking.send(SimulatedFabricNetworking.FLIGHT_CONTROL_TOGGLE, toggle);
            }

            if (!flightControlActive) {
                return;
            }

            int inputMask = 0;
            if (flightForward.isDown()) {
                inputMask |= SimulatedFabricNetworking.FLIGHT_FORWARD;
            }
            if (flightBack.isDown()) {
                inputMask |= SimulatedFabricNetworking.FLIGHT_BACK;
            }
            if (flightLeft.isDown()) {
                inputMask |= SimulatedFabricNetworking.FLIGHT_LEFT;
            }
            if (flightRight.isDown()) {
                inputMask |= SimulatedFabricNetworking.FLIGHT_RIGHT;
            }
            if (flightUp.isDown()) {
                inputMask |= SimulatedFabricNetworking.FLIGHT_UP;
            }
            if (flightDown.isDown()) {
                inputMask |= SimulatedFabricNetworking.FLIGHT_DOWN;
            }

            final FriendlyByteBuf input = PacketByteBufs.create();
            input.writeByte(inputMask);
            input.writeFloat(client.player.getYRot());
            ClientPlayNetworking.send(SimulatedFabricNetworking.FLIGHT_INPUT, input);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> flightControlActive = false);
    }

    private static void registerFlightKeys() {
        flightToggle = registerKey("key.simulated.flight_toggle", GLFW.GLFW_KEY_G);
        flightForward = registerKey("key.simulated.flight_forward", GLFW.GLFW_KEY_UP);
        flightBack = registerKey("key.simulated.flight_back", GLFW.GLFW_KEY_DOWN);
        flightLeft = registerKey("key.simulated.flight_left", GLFW.GLFW_KEY_LEFT);
        flightRight = registerKey("key.simulated.flight_right", GLFW.GLFW_KEY_RIGHT);
        flightUp = registerKey("key.simulated.flight_up", GLFW.GLFW_KEY_PAGE_UP);
        flightDown = registerKey("key.simulated.flight_down", GLFW.GLFW_KEY_PAGE_DOWN);
    }

    private static KeyMapping registerKey(final String translationKey, final int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(
                translationKey,
                InputConstants.Type.KEYSYM,
                defaultKey,
                "key.categories.simulated"));
    }
}
