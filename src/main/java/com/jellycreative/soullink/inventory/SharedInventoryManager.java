package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.network.SoulLinkNetwork;
import com.jellycreative.soullink.network.SyncInventoryPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the shared inventory system for Soul-Link.
 * 
 * This class maintains a single canonical inventory that all linked players share.
 * Any changes made by one player are synchronized to all other players.
 * 
 * Inventory slots:
 * - 0-8: Hotbar
 * - 9-35: Main inventory
 * - 36-39: Armor (boots, leggings, chestplate, helmet)
 * - 40: Offhand
 * 
 * IMPORTANT: This version includes fixes for duplication and item loss bugs:
 * - Players with open containers (crafting tables, furnaces, etc.) are excluded from sync
 * - Debouncing prevents rapid sync conflicts
 * - Cursor items are handled properly
 * - Version tracking prevents stale data overwrites
 */
@Mod.EventBusSubscriber(modid = SoulLink.MOD_ID)
public class SharedInventoryManager {
    
    // The canonical shared inventory (41 slots: 36 main + 4 armor + 1 offhand)
    private static final List<ItemStack> sharedInventory = new CopyOnWriteArrayList<>();
    
    // Track which players are currently being synced to prevent loops
    private static final ConcurrentHashMap<UUID, Boolean> syncingPlayers = new ConcurrentHashMap<>();
    
    // Track the last known inventory state for each player to detect changes
    private static final ConcurrentHashMap<UUID, List<ItemStack>> lastKnownInventory = new ConcurrentHashMap<>();
    
    // Track which players have a container open (crafting table, furnace, chest, etc.)
    private static final ConcurrentHashMap<UUID, Boolean> playersWithContainerOpen = new ConcurrentHashMap<>();
    
    // Debounce: Track last sync time to prevent rapid-fire syncs causing race conditions
    private static final ConcurrentHashMap<UUID, Long> lastSyncTime = new ConcurrentHashMap<>();
    private static final long SYNC_DEBOUNCE_MS = 150; // Minimum 150ms between syncs from same player
    
    // Global sync version to prevent stale data overwrites
    private static final AtomicLong globalSyncVersion = new AtomicLong(0);
    private static final ConcurrentHashMap<UUID, Long> playerSyncVersion = new ConcurrentHashMap<>();
    
    // Lock object for thread-safe operations
    private static final Object inventoryLock = new Object();
    
    // Whether the shared inventory has been initialized
    private static boolean initialized = false;
    
    // Total inventory size (36 main + 4 armor + 1 offhand)
    public static final int INVENTORY_SIZE = 41;
    
    // Slot indices
    public static final int HOTBAR_START = 0;
    public static final int HOTBAR_END = 8;
    public static final int MAIN_START = 9;
    public static final int MAIN_END = 35;
    public static final int ARMOR_START = 36;
    public static final int ARMOR_END = 39;
    public static final int OFFHAND_SLOT = 40;

    static {
        // Initialize with empty stacks
        initializeEmptyInventory();
    }

    private static void initializeEmptyInventory() {
        sharedInventory.clear();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            sharedInventory.add(ItemStack.EMPTY);
        }
        initialized = true;
    }

    /**
     * Check if shared inventory feature is enabled
     */
    public static boolean isEnabled() {
        return SoulLinkConfig.LINK_INVENTORY.get();
    }

    /**
     * Get the shared inventory
     */
    public static List<ItemStack> getSharedInventory() {
        return new ArrayList<>(sharedInventory);
    }

    /**
     * Set a specific slot in the shared inventory
     */
    public static void setSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < INVENTORY_SIZE) {
            synchronized (inventoryLock) {
                sharedInventory.set(slot, stack.copy());
            }
        }
    }

    /**
     * Get an item from a specific slot
     */
    public static ItemStack getSlot(int slot) {
        if (slot >= 0 && slot < INVENTORY_SIZE) {
            return sharedInventory.get(slot).copy();
        }
        return ItemStack.EMPTY;
    }

    /**
     * Mark a player as having a container open (crafting table, furnace, chest, etc.)
     * This prevents sync from overwriting their inventory mid-transaction.
     */
    public static void setContainerOpen(ServerPlayer player, boolean open) {
        if (open) {
            playersWithContainerOpen.put(player.getUUID(), true);
            SoulLink.LOGGER.debug("Player {} opened a container - sync paused for them", player.getName().getString());
        } else {
            playersWithContainerOpen.remove(player.getUUID());
            SoulLink.LOGGER.debug("Player {} closed container - sync resumed", player.getName().getString());
        }
    }

    /**
     * Check if a player has a container open (other than their basic inventory)
     */
    public static boolean hasContainerOpen(ServerPlayer player) {
        // Check our explicit tracking first
        if (playersWithContainerOpen.getOrDefault(player.getUUID(), false)) {
            return true;
        }
        
        // Also check the player's current container - if it's not the basic inventory menu, they have something open
        AbstractContainerMenu container = player.containerMenu;
        if (container != null && !(container instanceof InventoryMenu)) {
            return true;
        }
        
        return false;
    }

    /**
     * Check if a player is holding an item on their cursor (while dragging items)
     * Items on cursor should not trigger sync as they may be in transit.
     */
    public static boolean isHoldingCursorItem(ServerPlayer player) {
        return !player.containerMenu.getCarried().isEmpty();
    }

    /**
     * Copy a player's inventory to the shared inventory
     */
    public static void copyFromPlayer(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // SAFETY: Don't copy if player has container open - they might be mid-transaction
        if (hasContainerOpen(player)) {
            SoulLink.LOGGER.debug("Skipping copy from {} - container is open", player.getName().getString());
            return;
        }
        
        // SAFETY: Don't copy if player is dragging an item with their cursor
        if (isHoldingCursorItem(player)) {
            SoulLink.LOGGER.debug("Skipping copy from {} - cursor item in transit", player.getName().getString());
            return;
        }
        
        synchronized (inventoryLock) {
            Inventory inv = player.getInventory();
            
            // Copy main inventory (slots 0-35)
            for (int i = 0; i < 36; i++) {
                sharedInventory.set(i, inv.getItem(i).copy());
            }
            
            // Copy armor (slots 36-39)
            for (int i = 0; i < 4; i++) {
                sharedInventory.set(ARMOR_START + i, inv.armor.get(i).copy());
            }
            
            // Copy offhand (slot 40)
            sharedInventory.set(OFFHAND_SLOT, inv.offhand.get(0).copy());
            
            // Increment global version to mark this as the newest state
            globalSyncVersion.incrementAndGet();
        }
        
        // Update last known inventory for this player
        updateLastKnownInventory(player);
        
        // Update this player's sync version since they now have the latest
        playerSyncVersion.put(player.getUUID(), globalSyncVersion.get());
        
        SoulLink.LOGGER.debug("Copied inventory from player {} to shared inventory (version {})", 
                player.getName().getString(), globalSyncVersion.get());
    }

    /**
     * Apply the shared inventory to a player
     */
    public static void applyToPlayer(ServerPlayer player) {
        if (!isEnabled()) return;
        
        UUID playerId = player.getUUID();
        
        // Check if we're already syncing this player (prevent loops)
        if (syncingPlayers.getOrDefault(playerId, false)) {
            return;
        }
        
        // SAFETY: Don't apply if player has a container open - could corrupt their transaction
        if (hasContainerOpen(player)) {
            SoulLink.LOGGER.debug("Skipping apply to {} - container is open", player.getName().getString());
            return;
        }
        
        // SAFETY: Don't apply if player is dragging an item with cursor
        if (isHoldingCursorItem(player)) {
            SoulLink.LOGGER.debug("Skipping apply to {} - cursor item in transit", player.getName().getString());
            return;
        }
        
        // Check if this player is already up to date (version check)
        long currentVersion = globalSyncVersion.get();
        Long playerVersion = playerSyncVersion.get(playerId);
        if (playerVersion != null && playerVersion >= currentVersion) {
            return; // Player already has the latest version
        }
        
        syncingPlayers.put(playerId, true);
        
        try {
            synchronized (inventoryLock) {
                Inventory inv = player.getInventory();
                
                // Apply main inventory (slots 0-35)
                for (int i = 0; i < 36; i++) {
                    inv.setItem(i, sharedInventory.get(i).copy());
                }
                
                // Apply armor (slots 36-39)
                for (int i = 0; i < 4; i++) {
                    inv.armor.set(i, sharedInventory.get(ARMOR_START + i).copy());
                }
                
                // Apply offhand (slot 40)
                inv.offhand.set(0, sharedInventory.get(OFFHAND_SLOT).copy());
            }
            
            // Update player's sync version
            playerSyncVersion.put(playerId, globalSyncVersion.get());
            
            // Update last known inventory
            updateLastKnownInventory(player);
            
            // Send sync packet to client
            SoulLinkNetwork.sendToPlayer(new SyncInventoryPacket(sharedInventory), player);
            
            // Mark inventory as changed
            player.inventoryMenu.broadcastChanges();
            
        } finally {
            syncingPlayers.put(playerId, false);
        }
    }

    /**
     * Synchronize all players to the shared inventory
     */
    public static void syncAllPlayers() {
        if (!isEnabled()) return;
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyToPlayer(player);
        }
    }

    /**
     * Synchronize all players EXCEPT the source player
     */
    public static void syncAllPlayersExcept(ServerPlayer source) {
        if (!isEnabled()) return;
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.getUUID().equals(source.getUUID())) {
                applyToPlayer(player);
            }
        }
    }

    /**
     * Called when a player's inventory changes.
     * Updates the shared inventory and syncs to all other players.
     */
    public static void onPlayerInventoryChanged(ServerPlayer player) {
        if (!isEnabled()) return;
        
        UUID playerId = player.getUUID();
        
        // Don't process if we're currently syncing this player (loop prevention)
        if (syncingPlayers.getOrDefault(playerId, false)) {
            return;
        }
        
        // SAFETY: Don't process if player has a container open
        if (hasContainerOpen(player)) {
            return;
        }
        
        // SAFETY: Don't process if player is dragging cursor item
        if (isHoldingCursorItem(player)) {
            return;
        }
        
        // DEBOUNCE: Prevent rapid sync spam that causes race conditions
        long now = System.currentTimeMillis();
        Long lastSync = lastSyncTime.get(playerId);
        if (lastSync != null && (now - lastSync) < SYNC_DEBOUNCE_MS) {
            return; // Too soon since last sync from this player
        }
        
        // Check if inventory actually changed
        if (!hasInventoryChanged(player)) {
            return;
        }
        
        // Update debounce timestamp
        lastSyncTime.put(playerId, now);
        
        // Copy player's inventory to shared
        copyFromPlayer(player);
        
        // Sync to all other players
        syncAllPlayersExcept(player);
    }

    /**
     * Check if a player's inventory has changed from the last known state
     */
    private static boolean hasInventoryChanged(ServerPlayer player) {
        UUID playerId = player.getUUID();
        List<ItemStack> lastKnown = lastKnownInventory.get(playerId);
        
        if (lastKnown == null) {
            return true;
        }
        
        Inventory inv = player.getInventory();
        
        // Check main inventory
        for (int i = 0; i < 36; i++) {
            if (!ItemStack.matches(inv.getItem(i), lastKnown.get(i))) {
                return true;
            }
        }
        
        // Check armor
        for (int i = 0; i < 4; i++) {
            if (!ItemStack.matches(inv.armor.get(i), lastKnown.get(ARMOR_START + i))) {
                return true;
            }
        }
        
        // Check offhand
        if (!ItemStack.matches(inv.offhand.get(0), lastKnown.get(OFFHAND_SLOT))) {
            return true;
        }
        
        return false;
    }

    /**
     * Update the last known inventory state for a player
     */
    private static void updateLastKnownInventory(ServerPlayer player) {
        UUID playerId = player.getUUID();
        List<ItemStack> snapshot = new ArrayList<>(INVENTORY_SIZE);
        Inventory inv = player.getInventory();
        
        // Main inventory
        for (int i = 0; i < 36; i++) {
            snapshot.add(inv.getItem(i).copy());
        }
        
        // Armor
        for (int i = 0; i < 4; i++) {
            snapshot.add(inv.armor.get(i).copy());
        }
        
        // Offhand
        snapshot.add(inv.offhand.get(0).copy());
        
        lastKnownInventory.put(playerId, snapshot);
    }

    /**
     * Called when a player joins - sync from shared inventory
     */
    public static void onPlayerJoin(ServerPlayer player) {
        if (!isEnabled()) return;
        
        MinecraftServer server = player.server;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        
        // If this is the first player, use their inventory as the shared one
        if (players.size() == 1 || isSharedInventoryEmpty()) {
            copyFromPlayer(player);
            SoulLink.LOGGER.info("Player {} is first - using their inventory as shared", player.getName().getString());
        } else {
            // Apply shared inventory to the joining player
            applyToPlayer(player);
            SoulLink.LOGGER.info("Synced shared inventory to joining player {}", player.getName().getString());
        }
    }

    /**
     * Called when a player leaves - clean up their tracking data
     */
    public static void onPlayerLeave(ServerPlayer player) {
        UUID playerId = player.getUUID();
        syncingPlayers.remove(playerId);
        lastKnownInventory.remove(playerId);
        playersWithContainerOpen.remove(playerId);
        lastSyncTime.remove(playerId);
        playerSyncVersion.remove(playerId);
    }

    /**
     * Check if the shared inventory is empty (all slots empty)
     */
    private static boolean isSharedInventoryEmpty() {
        for (ItemStack stack : sharedInventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Save the shared inventory to NBT
     */
    public static CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag itemList = new ListTag();
        
        synchronized (inventoryLock) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                sharedInventory.get(i).save(itemTag);
                itemList.add(itemTag);
            }
        }
        
        tag.put("SharedInventory", itemList);
        tag.putBoolean("Initialized", initialized);
        tag.putLong("SyncVersion", globalSyncVersion.get());
        
        return tag;
    }

    /**
     * Load the shared inventory from NBT
     */
    public static void loadFromNBT(CompoundTag tag) {
        if (tag == null || !tag.contains("SharedInventory")) {
            initializeEmptyInventory();
            return;
        }
        
        ListTag itemList = tag.getList("SharedInventory", 10); // 10 = CompoundTag
        
        synchronized (inventoryLock) {
            initializeEmptyInventory();
            
            for (int i = 0; i < itemList.size(); i++) {
                CompoundTag itemTag = itemList.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < INVENTORY_SIZE) {
                    sharedInventory.set(slot, ItemStack.of(itemTag));
                }
            }
        }
        
        initialized = tag.getBoolean("Initialized");
        if (tag.contains("SyncVersion")) {
            globalSyncVersion.set(tag.getLong("SyncVersion"));
        }
        SoulLink.LOGGER.info("Loaded shared inventory from world data (version {})", globalSyncVersion.get());
    }

    /**
     * Reset the shared inventory (for new worlds or manual reset)
     */
    public static void reset() {
        synchronized (inventoryLock) {
            initializeEmptyInventory();
        }
        lastKnownInventory.clear();
        syncingPlayers.clear();
        playersWithContainerOpen.clear();
        lastSyncTime.clear();
        playerSyncVersion.clear();
        globalSyncVersion.set(0);
        SoulLink.LOGGER.info("Shared inventory reset");
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Load will happen via world data
        SoulLink.LOGGER.info("SharedInventoryManager ready");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Save happens via world data
        reset();
    }
}
