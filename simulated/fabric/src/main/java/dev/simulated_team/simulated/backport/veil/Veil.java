package dev.simulated_team.simulated.backport.veil;

import net.fabricmc.loader.api.FabricLoader;

/** Veil's entry point. Simulated asks it one thing: whether Iris is loaded. */
public final class Veil {

    public static final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");

    private Veil() {
    }
}
