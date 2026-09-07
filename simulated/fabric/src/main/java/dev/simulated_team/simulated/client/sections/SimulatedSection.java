package dev.simulated_team.simulated.client.sections;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.simulated_team.simulated.Simulated;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A creative-tab section, loaded from {@code assets/<ns>/simulated/sections}.
 *
 * <p>Upstream carries colours as Veil {@code Colorc} and the title as a
 * {@code ComponentSerialization.CODEC}. Neither exists here — Veil is not on the
 * 1.20.1 stack and component codecs arrived in 1.20.5 — so colours are plain
 * ARGB ints and the title round-trips through {@link Component.Serializer}. The
 * JSON shape upstream writes is unchanged.
 */
public record SimulatedSection(int priority, Title title, ResourceLocation sprite, boolean animateOnHover)
        implements Comparable<SimulatedSection> {
    private static final ResourceLocation DEFAULT_BANNER = Simulated.path("default_banner");

    public static final Codec<Component> COMPONENT_CODEC = Codec.PASSTHROUGH.comapFlatMap(
            dynamic -> {
                final Component component = Component.Serializer.fromJson(
                        dynamic.convert(com.mojang.serialization.JsonOps.INSTANCE).getValue());
                return component == null
                        ? DataResult.error(() -> "Not a valid text component")
                        : DataResult.success(component);
            },
            component -> new com.mojang.serialization.Dynamic<>(
                    com.mojang.serialization.JsonOps.INSTANCE,
                    Component.Serializer.toJsonTree(component)));

    /** Accepts either {@code "#aarrggbb"} / {@code "#rrggbb"} or a raw integer. */
    public static final Codec<Integer> ARGB_CODEC = Codec.either(Codec.STRING, Codec.INT).comapFlatMap(
            either -> either.map(
                    string -> {
                        final String hex = string.startsWith("#") ? string.substring(1) : string;
                        try {
                            return DataResult.success((int) Long.parseLong(hex, 16));
                        } catch (final NumberFormatException e) {
                            return DataResult.error(() -> "Not a hexadecimal colour: " + string);
                        }
                    },
                    DataResult::success),
            argb -> com.mojang.datafixers.util.Either.left(String.format("#%08x", argb)));

    public static final Codec<SimulatedSection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("priority", 0).forGetter(SimulatedSection::priority),
            Title.CODEC.fieldOf("title").forGetter(SimulatedSection::title),
            ResourceLocation.CODEC.optionalFieldOf("sprite", DEFAULT_BANNER).forGetter(SimulatedSection::sprite),
            Codec.BOOL.optionalFieldOf("only_animate_on_hover", false).forGetter(SimulatedSection::animateOnHover)
    ).apply(instance, SimulatedSection::new));

    @Override
    public int compareTo(@NotNull final SimulatedSection other) {
        return Integer.compare(this.priority(), other.priority());
    }

    public record Title(Component text, int color, Optional<Integer> secondaryColor, int background) {
        public static final Codec<Title> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                COMPONENT_CODEC.fieldOf("text").forGetter(Title::text),
                ARGB_CODEC.optionalFieldOf("color", 0xffffffff).forGetter(Title::color),
                ARGB_CODEC.optionalFieldOf("secondary_color").forGetter(Title::secondaryColor),
                ARGB_CODEC.optionalFieldOf("background", 0xaa000000).forGetter(Title::background)
        ).apply(instance, Title::new));
    }
}
