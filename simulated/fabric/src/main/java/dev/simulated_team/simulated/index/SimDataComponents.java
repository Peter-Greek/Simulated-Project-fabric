package dev.simulated_team.simulated.index;

import com.mojang.serialization.Codec;
import dev.simulated_team.simulated.backport.core.component.DataComponentType;
import dev.simulated_team.simulated.backport.net.ByteBufCodecs;
import dev.simulated_team.simulated.backport.net.SimCodecs;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * The item data Simulated attaches to stacks.
 *
 * <p>Upstream registers these into {@code Registries.DATA_COMPONENT_TYPE},
 * which arrived in 1.20.5. There is no such registry here, so a type is simply
 * a name and its codecs — see the backport's {@code DataComponentType} — and the
 * value lives in the stack's own NBT under one compound. Upstream's codecs are
 * unchanged, so the encoded shape matches what 1.21.1 writes.
 */
public class SimDataComponents {

    public static final DataComponentType<BlockPos> ROPE_FIRST_CONNECTION = register(
            "rope_first_connection",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(SimCodecs.BLOCK_POS)
    );

    public static final DataComponentType<UUID> LODESTONE_COMPASS_SUBLEVEL_TRACKER = register(
            "lodestone_compass_tracker",
            builder -> builder.persistent(UUIDUtil.CODEC));

    public static final DataComponentType<UUID> COMPASS_PLACER_UUID = register("compass_placer",
            builder -> builder.persistent(UUIDUtil.STRING_CODEC));

    public static final DataComponentType<GlobalPos> LAST_PLAYER_DEATH_LOCATION = register(
            "last_player_death_location",
            builder -> builder.persistent(GlobalPos.CODEC));

    public static final DataComponentType<NavigationTarget> TARGET = register("target", builder -> builder
            .persistent(SimRegistries.NAVIGATION_TARGET.byNameCodec())
            .networkSynchronized(SimCodecs.RESOURCE_LOCATION
                    .map(SimRegistries.NAVIGATION_TARGET::get, SimRegistries.NAVIGATION_TARGET::getKey))
    );

    public static final DataComponentType<Float> BOUNCINESS = register("bounciness", builder -> builder
            .persistent(Codec.FLOAT)
            .networkSynchronized(ByteBufCodecs.FLOAT)
    );

    private static <T> DataComponentType<T> register(
            final String name, final UnaryOperator<DataComponentType.Builder<T>> builder) {
        return builder.apply(DataComponentType.builder()).build().named(name);
    }

    public static void register() {
    }
}
