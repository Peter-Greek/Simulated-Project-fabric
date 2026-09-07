package dev.simulated_team.simulated.backport.veil.api.client.color;

import com.mojang.serialization.Codec;

/** A packed ARGB colour. See {@link Colorc}. */
public class Color implements Colorc {

    /**
     * Veil accepts a colour in JSON either as a number or as a {@code #rrggbb}
     * string; both shapes are kept so existing resource files still parse.
     */
    public static final Codec<Integer> ARGB_INT_CODEC = Codec.either(Codec.INT, Codec.STRING).xmap(
            either -> either.map(value -> value, Color::parse),
            value -> com.mojang.datafixers.util.Either.left(value));

    public static final Color WHITE = new Color(0xFFFFFFFF, true);

    public static final Color BLACK = new Color(0xFF000000, true);

    private final int argb;

    /**
     * @param value      the colour, packed
     * @param hasAlpha   whether the top byte carries alpha; when it does not,
     *                   the colour is treated as fully opaque, as Veil does
     */
    public Color(final int value, final boolean hasAlpha) {
        this.argb = hasAlpha ? value : (value | 0xFF000000);
    }

    public Color(final int red, final int green, final int blue, final int alpha) {
        this.argb = ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public Color(final float red, final float green, final float blue, final float alpha) {
        this(Math.round(red * 255.0F), Math.round(green * 255.0F),
                Math.round(blue * 255.0F), Math.round(alpha * 255.0F));
    }

    private static int parse(final String text) {
        final String trimmed = text.startsWith("#") ? text.substring(1) : text;
        return (int) Long.parseLong(trimmed, 16);
    }

    @Override
    public float red() {
        return this.redInt() / 255.0F;
    }

    @Override
    public float green() {
        return this.greenInt() / 255.0F;
    }

    @Override
    public float blue() {
        return this.blueInt() / 255.0F;
    }

    @Override
    public float alpha() {
        return this.alphaInt() / 255.0F;
    }

    @Override
    public int argb() {
        return this.argb;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof final Colorc colour && colour.argb() == this.argb;
    }

    @Override
    public int hashCode() {
        return this.argb;
    }

    @Override
    public String toString() {
        return String.format("Color[#%08X]", this.argb);
    }
}
