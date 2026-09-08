package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.service.SimEntityDataSerialization;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;

/**
 * 1.20.1 keeps entity data serialisers in a plain id map rather than a registry,
 * so registering one is a direct call.
 */
public class FabricSimEntityDataSerialization implements SimEntityDataSerialization {

    @Override
    public <A, T extends EntityDataSerializer<A>> void registerDataSerializer(final String name, final T serializer) {
        EntityDataSerializers.registerSerializer(serializer);
        Simulated.LOGGER.debug("Registered entity data serializer {}", name);
    }
}
