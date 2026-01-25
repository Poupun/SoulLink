package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event handler for the shared inventory system.
 */
@EventBusSubscriber(modid = SoulLink.MOD_ID)
public class SharedInventoryEventHandler {
    
    private static final Map<UUID, Integer> tickCounter = new ConcurrentHashMap<>();
    private static final int SYNC_CHECK_INTERVAL = 10;
    private static final int CONTAINER_CLOSE_DELAY = 5;
    private static final Map<UUID, Integer> containerCloseDelay = new ConcurrentHashMap<>();
    private static int saveTickCounter = 0;
    private static final int SAVE_INTERVAL = 200;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> SharedInventoryManager.onPlayerJoin(player));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SharedInventoryManager.onPlayerLeave(player);
            tickCounter.remove(player.getUUID());
            containerCloseDelay.remove(player.getUUID());
            if (SoulLinkConfig.LINK_INVENTORY.get()) {
                saveSharedInventory(player.serverLevel());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!(event.getContainer() instanceof InventoryMenu)) {
                SharedInventoryManager.setContainerOpen(player, true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (SharedInventoryManager.hasContainerOpen(player)) return;
            player.server.execute(() -> {
                if (!SharedInventoryManager.hasContainerOpen(player)) {
                    SharedInventoryManager.onPlayerInventoryChanged(player);
                }
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            SharedInventoryManager.setContainerOpen(player, false);
            containerCloseDelay.put(player.getUUID(), CONTAINER_CLOSE_DELAY);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        UUID playerId = player.getUUID();
        
        Integer closeDelay = containerCloseDelay.get(playerId);
        if (closeDelay != null) {
            if (closeDelay <= 0) {
                containerCloseDelay.remove(playerId);
                if (!SharedInventoryManager.hasContainerOpen(player) && 
                    !SharedInventoryManager.isHoldingCursorItem(player)) {
                    SharedInventoryManager.onPlayerInventoryChanged(player);
                }
            } else {
                containerCloseDelay.put(playerId, closeDelay - 1);
            }
            return;
        }
        
        if (SharedInventoryManager.hasContainerOpen(player)) return;
        if (SharedInventoryManager.isHoldingCursorItem(player)) return;
        
        int ticks = tickCounter.getOrDefault(playerId, 0) + 1;
        if (ticks >= SYNC_CHECK_INTERVAL) {
            ticks = 0;
            SharedInventoryManager.onPlayerInventoryChanged(player);
        }
        tickCounter.put(playerId, ticks);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        
        saveTickCounter++;
        if (saveTickCounter >= SAVE_INTERVAL) {
            saveTickCounter = 0;
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                if (overworld != null) saveSharedInventory(overworld);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        if (!SoulLinkConfig.KEEP_INVENTORY_ON_DEATH.get()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> SharedInventoryManager.applyToPlayer(player));
        }
    }

    private static void saveSharedInventory(ServerLevel level) {
        if (level == null) return;
        try {
            SharedInventorySavedData data = SharedInventorySavedData.get(level);
            data.setDirty();
        } catch (Exception e) {
            SoulLink.LOGGER.error("Failed to save shared inventory: {}", e.getMessage());
        }
    }
}
