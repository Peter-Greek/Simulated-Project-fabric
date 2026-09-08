package dev.simulated_team.simulated.backport.veil.api.client.color;

/**
 * Read-only colour. Veil's own is a float RGBA carrier; this keeps the same
 * accessors over a packed ARGB int, which is what everything in Simulated
 * ultimately hands to Minecraft.
 */
public interface Colorc {

    float red();

    float green();

    float blue();

    float alpha();

    int argb();

    default int rgb() {
        return this.argb() & 0x00FFFFFF;
    }

    default int redInt() {
        return (this.argb() >> 16) & 0xFF;
    }

    default int greenInt() {
        return (this.argb() >> 8) & 0xFF;
    }

    default int blueInt() {
        return this.argb() & 0xFF;
    }

    default int alphaInt() {
        return (this.argb() >>> 24) & 0xFF;
    }
}
