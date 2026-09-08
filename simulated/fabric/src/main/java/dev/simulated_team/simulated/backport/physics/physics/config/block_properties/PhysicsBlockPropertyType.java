package dev.simulated_team.simulated.backport.physics.physics.config.block_properties;

/** One physics property a block can carry, and the value used when it has none. */
public record PhysicsBlockPropertyType<T>(String name, T defaultValue) {
}
