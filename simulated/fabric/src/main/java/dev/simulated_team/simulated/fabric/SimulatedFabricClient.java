package dev.simulated_team.simulated.fabric;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblyContraption;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimResourceManagers;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
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
    private static final int NO_INPUT_SENT = -1;

    private static boolean flightControlActive;
    private static int lastSentInputMask = NO_INPUT_SENT;

    @Override
    public void onInitializeClient() {
        // Model parts are client-only: PartialModel lives in Flywheel's baked
        // model package, which a dedicated server cannot load. Touched here, not
        // from the common init.
        SimPartialModels.init();

        // Creative-tab sections are assets, so the listener belongs on the
        // client resource reload rather than the server data reload.
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(SimResourceManagers.SIMULATED_SECTION);

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
                        // Check the contraption type, not just the entity class:
                        // a stale or reused entity id would otherwise teleport an
                        // unrelated Create contraption such as a bearing or a
                        // windmill.
                        if (client.level.getEntity(entityId) instanceof final ControlledContraptionEntity entity
                                && entity.getContraption() instanceof PhysicsAssemblyContraption) {
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
                        // Force the next tick to transmit, so the server always
                        // learns the current input when a helm is engaged.
                        lastSentInputMask = NO_INPUT_SENT;
                        if (client.player != null) {
                            client.player.displayClientMessage(Component.literal(message), true);
                        }
                    });
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                flightControlActive = false;
                lastSentInputMask = NO_INPUT_SENT;
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

            // The server applies the stored input every tick, so intent only has
            // to be transmitted when it actually changes.
            if (inputMask == lastSentInputMask) {
                return;
            }
            lastSentInputMask = inputMask;

            final FriendlyByteBuf input = PacketByteBufs.create();
            input.writeByte(inputMask);
            ClientPlayNetworking.send(SimulatedFabricNetworking.FLIGHT_INPUT, input);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            flightControlActive = false;
            lastSentInputMask = NO_INPUT_SENT;
        });
    }
}
