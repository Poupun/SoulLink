package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.network.SoulLinkNetwork;
import com.jellycreative.soullink.network.SyncInventoryPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the shared inventory between all linked players.
 */
public class SharedInventoryManager {
    
    public static final int MAIN_INVENTORY_SIZE = 36;
    public static final int ARMOR_START = 36;
    public static final int ARMOR_SIZE = 4;
    public static final int OFFHAND_SLOT = 40;
    public static final int TOTAL_SIZE = 41;
    
    private static List<ItemStack> sharedInventory = createEmptyInventory();
    
    private static final Set<UUID> playersWithContainerOpen = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> lastSyncTime = new ConcurrentHashMap<>();
    private static final long SYNC_DEBOUNCE_MS = 150;
    private static final AtomicLong globalSyncVersion = new AtomicLong(0);
    private static final Map<UUID, Long> playerSyncVersions = new ConcurrentHashMap<>();
    
    public static void markContainerOpen(UUID playerId) {
        playersWithContainerOpen.add(playerId);
    }
    
    public static void markContainerClosed(UUID playerId) {
        playersWithContainerOpen.remove(playerId);
    }
    
    public static boolean hasContainerOpen(UUID playerId) {
        return playersWithContainerOpen.contains(playerId);
    }
    
    public static boolean isHoldingCursorItem(ServerPlayer player) {
        return !player.containerMenu.getCarried().isEmpty();
    }
    
    public static long getNextSyncVersion() {
        return globalSyncVersion.incrementAndGet();
    }
    
    public static void updatePlayerSyncVersion(UUID playerId, long version) {
        playerSyncVersions.put(playerId, version);
    }
    
    public static boolean canSync(UUID playerId) {
        long now = System.currentTimeMillis();
        Long lastSync = lastSyncTime.get(playerId);
        if (lastSync != null && (now - lastSync) < SYNC_DEBOUNCE_MS) {
            return false;
        }
        lastSyncTime.put(playerId, now);
        return true;
    }
    
    public static void syncFromPlayer(ServerPlayer sourcePlayer) {
        if (!SoulLinkConfig.SYNC_INVENTORY.get()) return;
        
        if (hasContainerOpen(sourcePlayer.getUUID())) return;
        if (isHoldingCursorItem(sourcePlayer)) return;
        if (!canSync(sourcePlayer.getUUID())) return;
        
        long syncVersion = getNextSyncVersion();
        updatePlayerSyncVersion(sourcePlayer.getUUID(), syncVersion);
        
        List<ItemStack> inventory = new ArrayList<>();
        
        for (int i = 0; i < MAIN_INVENTORY_SIZE; i++) {
            inventory.add(sourcePlayer.getInventory().getItem(i).copy());
        }
        
        for (int i = 0; i < ARMOR_SIZE; i++) {
            inventory.add(sourcePlayer.getInventory().armor.get(i).copy());
        }
        
        inventory.add(sourcePlayer.getInventory().offhand.get(0).copy());
        
        sharedInventory = inventory;
        
        List<ServerPlayer> allPlayers = sourcePlayer.server.getPlayerList().getPlayers();
        for (ServerPlayer otherPlayer : allPlayers) {
            if (!otherPlayer.equals(sourcePlayer)) {
                if (hasContainerOpen(otherPlayer.getUUID())) continue;
                if (isHoldingCursorItem(otherPlayer)) continue;
                
                updatePlayerSyncVersion(otherPlayer.getUUID(), syncVersion);
                applyInventoryToPlayer(otherPlayer, inventory);
                SoulLinkNetwork.sendToPlayer(new SyncInventoryPayload(inventory), otherPlayer);
            }
        }
    }
    
    public static void applyInventoryToPlayer(ServerPlayer player, List<ItemStack> inventory) {
        if (inventory == null || inventory.isEmpty()) return;
        if (hasContainerOpen(player.getUUID())) return;
        if (isHoldingCursorItem(player)) return;
        
        for (int i = 0; i < Math.min(MAIN_INVENTORY_SIZE, inventory.size()); i++) {
            player.getInventory().setItem(i, inventory.get(i).copy());
        }
        
        for (int i = 0; i < ARMOR_SIZE && (ARMOR_START + i) < inventory.size(); i++) {
            player.getInventory().armor.set(i, inventory.get(ARMOR_START + i).copy());
        }
        
        if (OFFHAND_SLOT < inventory.size()) {
            player.getInventory().offhand.set(0, inventory.get(OFFHAND_SLOT).copy());
        }
    }
    
    public static void loadForPlayer(ServerPlayer player) {
        if (!SoulLinkConfig.SYNC_INVENTORY.get()) return;
        
        if (!sharedInventory.isEmpty()) {
            applyInventoryToPlayer(player, sharedInventory);
            SoulLinkNetwork.sendToPlayer(new SyncInventoryPayload(sharedInventory), player);
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
    
    public static CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();
        for (int i = 0; i < sharedInventory.size(); i++) {
            ItemStack stack = sharedInventory.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stack.save(itemTag);
                listTag.add(itemTag);
            }
        }
        tag.put("SharedInventory", listTag);
        return tag;
    }
    
    public static void loadFromNBT(CompoundTag tag) {
        sharedInventory = createEmptyInventory();
        if (tag.contains("SharedInventory", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("SharedInventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < TOTAL_SIZE) {
                    sharedInventory.set(slot, ItemStack.of(itemTag));
                }
            }
        }
    }
}
