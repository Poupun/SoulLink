package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event handler for the shared inventory system.
 * Handles all inventory-related events and triggers synchronization.
 */
@Mod.EventBusSubscriber(modid = SoulLink.MOD_ID)
public class SharedInventoryEventHandler {
    
    // Track tick counter per player for periodic sync checks
    private static final Map<UUID, Integer> tickCounter = new ConcurrentHashMap<>();
    
    // How often to check for inventory changes (in ticks)
    private static final int SYNC_CHECK_INTERVAL = 5; // Every 5 ticks (0.25 seconds)
    
    // Track if we need to save
    private static int saveTickCounter = 0;
    private static final int SAVE_INTERVAL = 200; // Every 10 seconds (200 ticks)

    /**
     * Handle player login - sync from shared inventory
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            // Small delay to ensure player is fully loaded
            player.server.execute(() -> {
                SharedInventoryManager.onPlayerJoin(player);
            });
        }
    }

    /**
     * Handle player logout - clean up tracking
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SharedInventoryManager.onPlayerLeave(player);
            tickCounter.remove(player.getUUID());
            
            // Save the shared inventory when a player leaves
            if (SoulLinkConfig.LINK_INVENTORY.get()) {
                saveSharedInventory(player.serverLevel());
            }
        }
    }

    /**
     * Handle item pickup - sync immediately
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            // Sync after the item is picked up
            player.server.execute(() -> {
                SharedInventoryManager.onPlayerInventoryChanged(player);
            });
        }
    }

    /**
     * Handle container close - sync after interacting with chests, crafting, etc.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            // Sync after container closes
            player.server.execute(() -> {
                SharedInventoryManager.onPlayerInventoryChanged(player);
            });
        }
    }

    /**
     * Periodic tick handler to catch any inventory changes not captured by events
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        
        if (!(event.player instanceof ServerPlayer player)) return;
        
        UUID playerId = player.getUUID();
        int ticks = tickCounter.getOrDefault(playerId, 0) + 1;
        
        if (ticks >= SYNC_CHECK_INTERVAL) {
            ticks = 0;
            // Check for inventory changes
            SharedInventoryManager.onPlayerInventoryChanged(player);
        }
        
        tickCounter.put(playerId, ticks);
    }

    /**
     * Server tick handler for periodic saving
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        
        saveTickCounter++;
        
        if (saveTickCounter >= SAVE_INTERVAL) {
            saveTickCounter = 0;
            
            // Save shared inventory periodically
            if (event.getServer() != null) {
                ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    saveSharedInventory(overworld);
                }
            }
        }
    }

    /**
     * Handle player respawn - restore shared inventory
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        if (!SoulLinkConfig.KEEP_INVENTORY_ON_DEATH.get()) return;
        
        if (event.getEntity() instanceof ServerPlayer player) {
            // Restore shared inventory after respawn
            player.server.execute(() -> {
                SharedInventoryManager.applyToPlayer(player);
            });
        }
    }

    /**
     * Handle player clone (for respawn) - copy inventory if needed
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) return;
        if (!SoulLinkConfig.KEEP_INVENTORY_ON_DEATH.get()) return;
        
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer newPlayer) {
            // Will be handled by respawn event
        }
    }

    /**
     * Save the shared inventory to world data
     */
    private static void saveSharedInventory(ServerLevel level) {
        if (level == null) return;
        
        try {
            SharedInventorySavedData data = SharedInventorySavedData.get(level);
            data.markDirty();
        } catch (Exception e) {
            SoulLink.LOGGER.error("Failed to save shared inventory: {}", e.getMessage());
        }
    }

    /**
     * Load the shared inventory from world data
     */
    public static void loadSharedInventory(ServerLevel level) {
        if (level == null) return;
        
        try {
            // This will trigger the load
            SharedInventorySavedData.get(level);
            SoulLink.LOGGER.info("Shared inventory loaded from world data");
        } catch (Exception e) {
            SoulLink.LOGGER.error("Failed to load shared inventory: {}", e.getMessage());
        }
    }
}
