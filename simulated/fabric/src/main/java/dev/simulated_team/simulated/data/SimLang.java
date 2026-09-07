package dev.simulated_team.simulated.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.RegistrateLangProvider;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Merges the hand-written keys in {@code datagen/lang/en_us.json} into the lang
 * file Registrate generates from the registered entries, the same way upstream
 * does. Names of blocks, items and block entities are not listed there — those
 * come from the registry entries themselves.
 */
public class SimLang {

    public static void registrateLang(final RegistrateLangProvider provider) {
        final BiConsumer<String, String> consumer = provider::add;
        getLangMap("en_us").forEach(consumer);
    }

    private static Map<String, String> getLangMap(final String lang) {
        final String filepath = "datagen/lang/%s.json".formatted(lang);
        final JsonObject langObject = FilesHelper.loadJsonResource(filepath).getAsJsonObject();

        final Map<String, String> langMap = new LinkedHashMap<>();
        flattenJson(langMap, langObject, null);
        return langMap;
    }

    /** Nested objects join with a dot, so {@code {"a":{"b":"c"}}} becomes {@code a.b = c}. */
    private static void flattenJson(final Map<String, String> target, final JsonObject object, final String prefix) {
        for (final Map.Entry<String, JsonElement> entry : object.entrySet()) {
            final String key = prefix == null ? entry.getKey() : prefix + "." + entry.getKey();
            final JsonElement value = entry.getValue();

            if (value.isJsonObject()) {
                flattenJson(target, value.getAsJsonObject(), key);
            } else {
                target.put(key, value.getAsString());
            }
        }
    }
}
