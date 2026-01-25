package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.network.SyncInventoryPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the shared inventory between all linked players.
 * Uses version tracking to prevent stale updates.
 */
public class SharedInventoryManager {
    
    public static final int MAIN_INVENTORY_SIZE = 36;
    public static final int ARMOR_START = 36;
    public static final int ARMOR_SIZE = 4;
    public static final int OFFHAND_SLOT = 40;
    public static final int TOTAL_SIZE = 41;
    
    private static SharedInventorySavedData savedData;
    
    // Track players with open containers to prevent sync during container use
    private static final Set<UUID> playersWithContainerOpen = ConcurrentHashMap.newKeySet();
    
    // Track last sync time per player to debounce
    private static final Map<UUID, Long> lastSyncTime = new ConcurrentHashMap<>();
    private static final long SYNC_DEBOUNCE_MS = 150;
    
    // Global sync version to prevent stale updates
    private static final AtomicLong globalSyncVersion = new AtomicLong(0);
    private static final Map<UUID, Long> playerSyncVersions = new ConcurrentHashMap<>();
    
    public static void init(ServerLevel level) {
        savedData = level.getDataStorage().computeIfAbsent(
                SharedInventorySavedData.factory(level.registryAccess()),
                SoulLink.MOD_ID + "_inventory"
        );
    }
    
    public static List<ItemStack> getSharedInventory() {
        if (savedData == null) return createEmptyInventory();
        return savedData.getInventory();
    }
    
    public static void setSharedInventory(List<ItemStack> inventory, HolderLookup.Provider registries) {
        if (savedData != null) {
            savedData.setInventory(inventory);
            savedData.setDirty();
        }
    }
    
    /**
     * Mark a player as having a container open (crafting table, chest, etc.)
     */
    public static void markContainerOpen(UUID playerId) {
        playersWithContainerOpen.add(playerId);
    }
    
    /**
     * Mark a player as having closed their container
     */
    public static void markContainerClosed(UUID playerId) {
        playersWithContainerOpen.remove(playerId);
    }
    
    /**
     * Check if a player has a container open
     */
    public static boolean hasContainerOpen(UUID playerId) {
        return playersWithContainerOpen.contains(playerId);
    }
    
    /**
     * Check if player is holding an item on cursor
     */
    public static boolean isHoldingCursorItem(ServerPlayer player) {
        return !player.containerMenu.getCarried().isEmpty();
    }
    
    /**
     * Get next sync version
     */
    public static long getNextSyncVersion() {
        return globalSyncVersion.incrementAndGet();
    }
    
    /**
     * Check if sync version is current
     */
    public static boolean isSyncVersionCurrent(UUID playerId, long version) {
        Long playerVersion = playerSyncVersions.get(playerId);
        return playerVersion == null || version > playerVersion;
    }
    
    /**
     * Update player's sync version
     */
    public static void updatePlayerSyncVersion(UUID playerId, long version) {
        playerSyncVersions.put(playerId, version);
    }
    
    /**
     * Check if enough time has passed since last sync for this player
     */
    public static boolean canSync(UUID playerId) {
        long now = System.currentTimeMillis();
        Long lastSync = lastSyncTime.get(playerId);
        if (lastSync != null && (now - lastSync) < SYNC_DEBOUNCE_MS) {
            return false;
        }
        lastSyncTime.put(playerId, now);
        return true;
    }
    
    /**
     * Synchronize a player's inventory to the shared inventory and broadcast to others.
     */
    public static void syncFromPlayer(ServerPlayer sourcePlayer) {
        if (!SoulLinkConfig.SYNC_INVENTORY.get()) return;
        if (savedData == null) return;
        
        // Don't sync if player has a container open or is holding cursor item
        if (hasContainerOpen(sourcePlayer.getUUID())) return;
        if (isHoldingCursorItem(sourcePlayer)) return;
        
        // Debounce check
        if (!canSync(sourcePlayer.getUUID())) return;
        
        // Get new version for this sync
        long syncVersion = getNextSyncVersion();
        updatePlayerSyncVersion(sourcePlayer.getUUID(), syncVersion);
        
        // Copy player's inventory to shared storage
        List<ItemStack> inventory = new ArrayList<>();
        
        // Main inventory (slots 0-35)
        for (int i = 0; i < MAIN_INVENTORY_SIZE; i++) {
            inventory.add(sourcePlayer.getInventory().getItem(i).copy());
        }
        
        // Armor (slots 36-39)
        for (int i = 0; i < ARMOR_SIZE; i++) {
            inventory.add(sourcePlayer.getInventory().armor.get(i).copy());
        }
        
        // Offhand (slot 40)
        inventory.add(sourcePlayer.getInventory().offhand.get(0).copy());
        
        savedData.setInventory(inventory);
        savedData.setDirty();
        
        // Sync to all other online players
        List<ServerPlayer> allPlayers = sourcePlayer.server.getPlayerList().getPlayers();
        for (ServerPlayer otherPlayer : allPlayers) {
            if (!otherPlayer.equals(sourcePlayer)) {
                // Skip players with containers open or holding cursor items
                if (hasContainerOpen(otherPlayer.getUUID())) continue;
                if (isHoldingCursorItem(otherPlayer)) continue;
                
                // Update version for other player
                updatePlayerSyncVersion(otherPlayer.getUUID(), syncVersion);
                
                applyInventoryToPlayer(otherPlayer, inventory);
                PacketDistributor.sendToPlayer(otherPlayer, new SyncInventoryPayload(inventory));
            }
        }
    }
    
    /**
     * Apply the shared inventory to a player.
     */
    public static void applyInventoryToPlayer(ServerPlayer player, List<ItemStack> inventory) {
        if (inventory == null || inventory.isEmpty()) return;
        
        // Don't apply if player has container open or is holding cursor item
        if (hasContainerOpen(player.getUUID())) return;
        if (isHoldingCursorItem(player)) return;
        
        // Main inventory
        for (int i = 0; i < Math.min(MAIN_INVENTORY_SIZE, inventory.size()); i++) {
            player.getInventory().setItem(i, inventory.get(i).copy());
        }
        
        // Armor
        for (int i = 0; i < ARMOR_SIZE && (ARMOR_START + i) < inventory.size(); i++) {
            player.getInventory().armor.set(i, inventory.get(ARMOR_START + i).copy());
        }
        
        // Offhand
        if (OFFHAND_SLOT < inventory.size()) {
            player.getInventory().offhand.set(0, inventory.get(OFFHAND_SLOT).copy());
        }
    }
    
    /**
     * Load shared inventory for a player joining.
     */
    public static void loadForPlayer(ServerPlayer player) {
        if (!SoulLinkConfig.SYNC_INVENTORY.get()) return;
        if (savedData == null) return;
        
        List<ItemStack> inventory = savedData.getInventory();
        if (!inventory.isEmpty()) {
            applyInventoryToPlayer(player, inventory);
            PacketDistributor.sendToPlayer(player, new SyncInventoryPayload(inventory));
        }
    }
    
    public static void clearPlayerTracking(UUID playerId) {
        playersWithContainerOpen.remove(playerId);
        lastSyncTime.remove(playerId);
        playerSyncVersions.remove(playerId);
    }
    
    private static List<ItemStack> createEmptyInventory() {
        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < TOTAL_SIZE; i++) {
            inventory.add(ItemStack.EMPTY);
        }
        return inventory;
    }
    
    public static void saveInventory(CompoundTag tag, HolderLookup.Provider registries) {
        if (savedData != null) {
            List<ItemStack> inventory = savedData.getInventory();
            ListTag listTag = new ListTag();
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.get(i);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putInt("Slot", i);
                    listTag.add(stack.save(registries, itemTag));
                }
            }
            tag.put("SharedInventory", listTag);
        }
    }
    
    public static void loadInventory(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("SharedInventory", Tag.TAG_LIST)) {
            List<ItemStack> inventory = createEmptyInventory();
            ListTag listTag = tag.getList("SharedInventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < TOTAL_SIZE) {
                    inventory.set(slot, ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY));
                }
            }
            if (savedData != null) {
                savedData.setInventory(inventory);
            }
        }
    }
}
