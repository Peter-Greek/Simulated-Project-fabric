package dev.simulated_team.simulated.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.simulated_team.simulated.Simulated;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Upstream's codec-driven resource manager, rewritten without Veil's
 * {@code CodecReloadListener} — Veil is not available on this stack, and the
 * listener it supplies is a thin wrapper around exactly this.
 */
public class SimpleResourceManager<T> extends SimplePreparableReloadListener<Map<ResourceLocation, T>>
        implements IdentifiableResourceReloadListener {

    private final Codec<T> codec;
    private final FileToIdConverter converter;
    private final ResourceLocation id;

    private final Map<ResourceLocation, T> entries = new LinkedHashMap<>();
    private final Map<T, ResourceLocation> toId = new LinkedHashMap<>();
    private final List<T> sortedValues = new java.util.ArrayList<>();
    private boolean canSort = false;

    public static <T> SimpleResourceManager<T> create(final Codec<T> codec, final ResourceLocation path) {
        return new SimpleResourceManager<>(codec, path);
    }

    private SimpleResourceManager(final Codec<T> codec, final ResourceLocation path) {
        this.codec = codec;
        this.converter = FileToIdConverter.json(path.getNamespace() + "/" + path.getPath());
        this.id = path;
    }

    public SimpleResourceManager<T> sorted() {
        this.canSort = true;
        return this;
    }

    public T get(final ResourceLocation id) {
        return this.entries.get(id);
    }

    public ResourceLocation getId(final T t) {
        return this.toId.get(t);
    }

    public Set<Map.Entry<ResourceLocation, T>> entrySet() {
        return this.entries.entrySet();
    }

    public Collection<T> entries() {
        return this.entries.values();
    }

    public List<T> sortedEntries() {
        return this.sortedValues;
    }

    @Override
    public ResourceLocation getFabricId() {
        return this.id;
    }

    @Override
    protected Map<ResourceLocation, T> prepare(final ResourceManager manager, final ProfilerFiller profiler) {
        final Map<ResourceLocation, T> parsed = new LinkedHashMap<>();

        for (final Map.Entry<ResourceLocation, Resource> entry : this.converter.listMatchingResources(manager).entrySet()) {
            final ResourceLocation file = entry.getKey();
            final ResourceLocation entryId = this.converter.fileToId(file);

            try (BufferedReader reader = entry.getValue().openAsReader()) {
                final JsonElement json = JsonParser.parseReader(reader);
                this.codec.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> Simulated.LOGGER.error("Failed to parse {}: {}", file, error))
                        .ifPresent(value -> parsed.put(entryId, value));
            } catch (final IOException | RuntimeException e) {
                Simulated.LOGGER.error("Failed to read {}", file, e);
            }
        }

        return parsed;
    }

    @Override
    protected void apply(final Map<ResourceLocation, T> map, final ResourceManager manager, final ProfilerFiller profiler) {
        this.entries.clear();
        this.entries.putAll(map);
        this.toId.clear();
        map.forEach((key, value) -> this.toId.put(value, key));

        if (this.canSort) {
            this.sortedValues.clear();
            this.sortedValues.addAll(map.values().stream()
                    .sorted((a, b) -> ((Comparable<T>) a).compareTo(b))
                    .toList());
        }
    }
}
