package com.jellycreative.soullink.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jellycreative.soullink.SoulLink;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for Soul-Link mod using JSON file.
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
        public boolean showLinkMessages = false;
        public boolean keepInventoryOnDeath = true;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                config = GSON.fromJson(json, ConfigData.class);
                SoulLink.LOGGER.info("Loaded Soul-Link configuration");
            } catch (IOException e) {
                SoulLink.LOGGER.error("Failed to load config, using defaults", e);
                config = new ConfigData();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
            SoulLink.LOGGER.info("Saved Soul-Link configuration");
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

    public static boolean isKeepInventoryOnDeath() {
        return config.keepInventoryOnDeath;
    }
}
