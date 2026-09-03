package dev.simulated_team.simulated.fabric;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-only hooks for the Homestead Fabric port. */
public final class SimulatedFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
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
    }
}
