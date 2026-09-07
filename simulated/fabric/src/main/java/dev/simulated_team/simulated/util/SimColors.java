package dev.simulated_team.simulated.util;

import java.awt.Color;

public class SimColors {
    public static int SUCCESS_LIME = new Color(158, 222, 115).getRGB();
    public static int NUH_UH_RED = new Color(255, 113, 113).getRGB();

    public static int REDSTONE_OFF = new Color(86, 1, 1).getRGB();
    public static int REDSTONE_ON = new Color(205, 0, 0).getRGB();

    public static int redstone(final float frac) {
        return net.createmod.catnip.theme.Color.mixColors(REDSTONE_OFF, REDSTONE_ON, frac);
    }

    public static int ADVANCABLE_GOLD = new Color(219, 162, 19).getRGB();
    public static int EPIC_OURPLE = new Color(165, 0, 170).getRGB();
}
