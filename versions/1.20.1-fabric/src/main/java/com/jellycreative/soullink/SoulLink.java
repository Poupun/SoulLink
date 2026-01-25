package com.jellycreative.soullink;

import com.jellycreative.soullink.command.SoulLinkCommands;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.event.SoulLinkEventHandler;
import com.jellycreative.soullink.inventory.SharedInventoryEventHandler;
import com.jellycreative.soullink.network.SoulLinkNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoulLink implements ModInitializer {
    public static final String MOD_ID = "soullink";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Soul-Link mod initializing...");

        // Load config
        SoulLinkConfig.load();

        // Register networking
        SoulLinkNetwork.register();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SoulLinkCommands.register(dispatcher);
        });

        // Damage events handled via mixin (AFTER_DAMAGE not available in Fabric API for 1.20.1)

        // Register tick event for hunger sync
        ServerTickEvents.END_SERVER_TICK.register(SoulLinkEventHandler::onServerTick);

        // Register player connection events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SoulLinkEventHandler.onPlayerJoin(handler.getPlayer());
            SharedInventoryEventHandler.onPlayerJoin(handler.getPlayer());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SoulLinkEventHandler.onPlayerLeave(handler.getPlayer());
        });

        // Register inventory events
        SharedInventoryEventHandler.register();

        LOGGER.info("Soul-Link mod initialized successfully!");
    }
}
