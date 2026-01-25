package com.jellycreative.soullink.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jellycreative.soullink.SoulLink;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for Soul-Link mod using JSON.
 */
public class SoulLinkConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("soullink.json");

    private static ConfigData config = new ConfigData();

    public static class ConfigData {
        public boolean syncHealth = true;
        public boolean syncHunger = true;
        public boolean syncKnockback = true;
        public boolean syncInventory = true;
        public boolean showLinkMessages = true;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                config = GSON.fromJson(reader, ConfigData.class);
                if (config == null) {
                    config = new ConfigData();
                }
                SoulLink.LOGGER.info("Config loaded successfully");
            } catch (IOException e) {
                SoulLink.LOGGER.error("Failed to load config", e);
                config = new ConfigData();
            }
        } else {
            save();
            SoulLink.LOGGER.info("Created default config");
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            SoulLink.LOGGER.error("Failed to save config", e);
        }
    }

    public static boolean isSyncHealth() {
        return config.syncHealth;
    }

    public static boolean isSyncHunger() {
        return config.syncHunger;
    }

    public static boolean isSyncKnockback() {
        return config.syncKnockback;
    }

    public static boolean isSyncInventory() {
        return config.syncInventory;
    }

    public static boolean isShowLinkMessages() {
        return config.showLinkMessages;
    }
}
