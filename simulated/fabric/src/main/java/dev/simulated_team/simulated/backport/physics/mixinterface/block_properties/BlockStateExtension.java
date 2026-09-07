package dev.simulated_team.simulated.backport.physics.mixinterface.block_properties;

/**
 * Sable mixes this onto every {@code BlockState} so a block can carry physics
 * properties — mass, friction, restitution, buoyancy.
 *
 * <p><b>Inert.</b> There is no mixin adding it here, so upstream's cast to it
 * would fail at runtime. Every ported cast is guarded by
 * {@link #of(Object)}, which returns the default carrier instead: each property
 * reports its own default, which is what the block-properties tooltip then
 * shows. Real values arrive with the engine in V2.
 */
public interface BlockStateExtension {

    /** The default carrier: every property reads back its declared default. */
    BlockStateExtension DEFAULTS = new BlockStateExtension() {
    };

    /**
     * Upstream casts a {@code BlockState} to this interface directly. Nothing
     * implements it on this stack, so ported code goes through here instead and
     * gets the defaults.
     */
    static BlockStateExtension of(final Object blockState) {
        return blockState instanceof final BlockStateExtension extension ? extension : DEFAULTS;
    }

    default <T> T sable$getProperty(
            final dev.simulated_team.simulated.backport.physics.physics.config.block_properties
                    .PhysicsBlockPropertyType<T> type) {
        return type.defaultValue();
    }
}
