package dev.simulated_team.simulated.backport.veil.api;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;

import java.util.Map;

/**
 * A reload listener that parses each JSON file through a codec.
 *
 * <p>Real: this is what Veil's own does, on vanilla's
 * {@link SimpleJsonResourceReloadListener}. Subclasses receive the parsed values
 * rather than raw JSON.
 */
public abstract class CodecReloadListener<T> extends SimpleJsonResourceReloadListener {

    private final Codec<T> codec;

    protected CodecReloadListener(final Codec<T> codec, final com.google.gson.Gson gson, final String directory) {
        super(gson, directory);
        this.codec = codec;
    }

    protected Codec<T> codec() {
        return this.codec;
    }

    protected java.util.Optional<T> parse(final ResourceLocation id, final JsonElement json) {
        return this.codec.parse(com.mojang.serialization.JsonOps.INSTANCE, json)
                .resultOrPartial(error -> org.slf4j.LoggerFactory.getLogger(this.getClass())
                        .error("Failed to parse {}: {}", id, error))
                .map(value -> value);
    }

    protected abstract void apply(Map<ResourceLocation, JsonElement> files,
                                  net.minecraft.server.packs.resources.ResourceManager manager,
                                  net.minecraft.util.profiling.ProfilerFiller profiler);
}
