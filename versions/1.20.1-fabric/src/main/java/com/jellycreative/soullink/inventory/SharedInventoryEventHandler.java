package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.event.SoulLinkEventHandler;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

/**
 * Event handler for shared inventory system.
 */
public class SharedInventoryEventHandler {

    private static final Set<UUID> playersWithOpenContainer = new HashSet<>();
    private static final Map<UUID, Long> lastSyncTime = new HashMap<>();
    private static final long SYNC_DEBOUNCE_MS = 150;

    public static void register() {
        // Register tick event for inventory sync
        ServerTickEvents.END_SERVER_TICK.register(SharedInventoryEventHandler::onServerTick);

        // Register respawn event
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (SoulLinkConfig.isSyncInventory() && SoulLinkEventHandler.isLinkEnabled()) {
                // Restore shared inventory after respawn
                newPlayer.getServer().execute(() -> {
                    SharedInventoryManager.syncToPlayer(newPlayer);
                });
            }
        });
    }

    private static void onServerTick(MinecraftServer server) {
        if (!SoulLinkConfig.isSyncInventory() || !SoulLinkEventHandler.isLinkEnabled()) return;

        // Sync inventory every second
        if (server.getTicks() % 20 != 0) return;

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                // Skip if player has container open
                if (playersWithOpenContainer.contains(player.getUuid())) continue;

                // Skip if player is holding cursor item
                if (!player.currentScreenHandler.getCursorStack().isEmpty()) continue;

                // Debounce
                long now = System.currentTimeMillis();
                Long lastSync = lastSyncTime.get(player.getUuid());
                if (lastSync != null && now - lastSync < SYNC_DEBOUNCE_MS) continue;

                lastSyncTime.put(player.getUuid(), now);

                // Sync from this player to shared inventory
                SharedInventoryManager.syncFromPlayer(player);
            }
        }
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        if (!SoulLinkConfig.isSyncInventory() || !SoulLinkEventHandler.isLinkEnabled()) return;

        // Give player the shared inventory on join
        player.getServer().execute(() -> {
            SharedInventoryManager.syncToPlayer(player);
        });
    }

    public static void markContainerOpen(ServerPlayerEntity player) {
        playersWithOpenContainer.add(player.getUuid());
    }

    public static void markContainerClosed(ServerPlayerEntity player) {
        playersWithOpenContainer.remove(player.getUuid());
        // Schedule sync after container closes
        if (player.getServer() != null) {
            player.getServer().execute(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
                SharedInventoryManager.syncFromPlayer(player);
            });
        }
    }
}
