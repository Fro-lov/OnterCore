package com.onter.ontercore.core.lang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServerLanguageManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> translations = new HashMap<>();
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ServerLanguageManager.class);

    public static void loadLanguageFile(File langFile) {
        if (!langFile.exists()) {
            LOGGER.warn("Language file not found: {}", langFile.getPath());
            return;
        }

        try (FileReader reader = new FileReader(langFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            json.entrySet().forEach(entry -> 
                translations.put(entry.getKey(), entry.getValue().getAsString())
            );
            LOGGER.info("Loaded {} translations from {}", translations.size(), langFile.getName());
        } catch (IOException e) {
            LOGGER.error("Failed to load language file: {}", langFile.getPath(), e);
        }
    }

    public static String translate(String key) {
        return translations.getOrDefault(key, key);
    }

    public static String translate(String key, String defaultValue) {
        return translations.getOrDefault(key, defaultValue);
    }

    public static void saveLanguageFile(File langFile) {
        try (FileWriter writer = new FileWriter(langFile)) {
            JsonObject json = new JsonObject();
            translations.forEach(json::addProperty);
            GSON.toJson(json, writer);
            LOGGER.info("Saved {} translations to {}", translations.size(), langFile.getName());
        } catch (IOException e) {
            LOGGER.error("Failed to save language file: {}", langFile.getPath(), e);
        }
    }

    public static void addTranslation(String key, String value) {
        translations.put(key, value);
    }
}
