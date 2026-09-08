package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.service.SimEntityDataSerialization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.phys.Vec3;

/**
 * Entity data serialisers Simulated adds.
 *
 * <p>1.20.5 rebuilt {@code EntityDataSerializer} on stream codecs; 1.20.1 reads
 * and writes the buffer directly, which is what this does. The wire shape — three
 * doubles — is the same either way.
 */
public class SimEntityDataSerializers {

    public static final EntityDataSerializer<Vec3> VEC3 = new EntityDataSerializer<>() {
        @Override
        public void write(final FriendlyByteBuf buffer, final Vec3 value) {
            buffer.writeDouble(value.x);
            buffer.writeDouble(value.y);
            buffer.writeDouble(value.z);
        }

        @Override
        public Vec3 read(final FriendlyByteBuf buffer) {
            return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }

        @Override
        public Vec3 copy(final Vec3 value) {
            return new Vec3(value.x, value.y, value.z);
        }
    };

    public static void register() {
        SimEntityDataSerialization.INSTANCE.registerDataSerializer("vec3", VEC3);
    }
}
