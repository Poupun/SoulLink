package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.network.SoulLinkNetwork;
import com.jellycreative.soullink.network.SyncInventoryPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages the shared inventory system for Soul-Link.
 */
@EventBusSubscriber(modid = SoulLink.MOD_ID)
public class SharedInventoryManager {
    
    private static final List<ItemStack> sharedInventory = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<UUID, Boolean> syncingPlayers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, List<ItemStack>> lastKnownInventory = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> playersWithContainerOpen = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastSyncTime = new ConcurrentHashMap<>();
    private static final long SYNC_DEBOUNCE_MS = 150;
    private static final AtomicLong globalSyncVersion = new AtomicLong(0);
    private static final ConcurrentHashMap<UUID, Long> playerSyncVersion = new ConcurrentHashMap<>();
    private static final Object inventoryLock = new Object();
    private static boolean initialized = false;
    
    public static final int INVENTORY_SIZE = 41;
    public static final int HOTBAR_START = 0;
    public static final int HOTBAR_END = 8;
    public static final int MAIN_START = 9;
    public static final int MAIN_END = 35;
    public static final int ARMOR_START = 36;
    public static final int ARMOR_END = 39;
    public static final int OFFHAND_SLOT = 40;

    static {
        initializeEmptyInventory();
    }

    private static void initializeEmptyInventory() {
        sharedInventory.clear();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            sharedInventory.add(ItemStack.EMPTY);
        }
        initialized = true;
    }

    public static boolean isEnabled() {
        return SoulLinkConfig.LINK_INVENTORY.get();
    }

    public static List<ItemStack> getSharedInventory() {
        return new ArrayList<>(sharedInventory);
    }

    public static void setSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < INVENTORY_SIZE) {
            synchronized (inventoryLock) {
                sharedInventory.set(slot, stack.copy());
            }
        }
    }

    public static ItemStack getSlot(int slot) {
        if (slot >= 0 && slot < INVENTORY_SIZE) {
            return sharedInventory.get(slot).copy();
        }
        return ItemStack.EMPTY;
    }

    public static void setContainerOpen(ServerPlayer player, boolean open) {
        if (open) {
            playersWithContainerOpen.put(player.getUUID(), true);
        } else {
            playersWithContainerOpen.remove(player.getUUID());
        }
    }

    public static boolean hasContainerOpen(ServerPlayer player) {
        if (playersWithContainerOpen.getOrDefault(player.getUUID(), false)) {
            return true;
        }
        AbstractContainerMenu container = player.containerMenu;
        return container != null && !(container instanceof InventoryMenu);
    }

    public static boolean isHoldingCursorItem(ServerPlayer player) {
        return !player.containerMenu.getCarried().isEmpty();
    }

    public static void copyFromPlayer(ServerPlayer player) {
        if (!isEnabled()) return;
        if (hasContainerOpen(player) || isHoldingCursorItem(player)) return;
        
        synchronized (inventoryLock) {
            Inventory inv = player.getInventory();
            for (int i = 0; i < 36; i++) {
                sharedInventory.set(i, inv.getItem(i).copy());
            }
            for (int i = 0; i < 4; i++) {
                sharedInventory.set(ARMOR_START + i, inv.armor.get(i).copy());
            }
            sharedInventory.set(OFFHAND_SLOT, inv.offhand.getFirst().copy());
            globalSyncVersion.incrementAndGet();
        }
        updateLastKnownInventory(player);
        playerSyncVersion.put(player.getUUID(), globalSyncVersion.get());
    }

    public static void applyToPlayer(ServerPlayer player) {
        if (!isEnabled()) return;
        
        UUID playerId = player.getUUID();
        if (syncingPlayers.getOrDefault(playerId, false)) return;
        if (hasContainerOpen(player) || isHoldingCursorItem(player)) return;
        
        long currentVersion = globalSyncVersion.get();
        Long playerVersion = playerSyncVersion.get(playerId);
        if (playerVersion != null && playerVersion >= currentVersion) return;
        
        syncingPlayers.put(playerId, true);
        try {
            synchronized (inventoryLock) {
                Inventory inv = player.getInventory();
                for (int i = 0; i < 36; i++) {
                    inv.setItem(i, sharedInventory.get(i).copy());
                }
                for (int i = 0; i < 4; i++) {
                    inv.armor.set(i, sharedInventory.get(ARMOR_START + i).copy());
                }
                inv.offhand.set(0, sharedInventory.get(OFFHAND_SLOT).copy());
            }
            playerSyncVersion.put(playerId, globalSyncVersion.get());
            updateLastKnownInventory(player);
            SoulLinkNetwork.sendToPlayer(new SyncInventoryPayload(sharedInventory), player);
            player.inventoryMenu.broadcastChanges();
        } finally {
            syncingPlayers.put(playerId, false);
        }
    }

    public static void syncAllPlayers() {
        if (!isEnabled()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyToPlayer(player);
        }
    }

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

    public static void onPlayerInventoryChanged(ServerPlayer player) {
        if (!isEnabled()) return;
        UUID playerId = player.getUUID();
        if (syncingPlayers.getOrDefault(playerId, false)) return;
        if (hasContainerOpen(player) || isHoldingCursorItem(player)) return;
        
        long now = System.currentTimeMillis();
        Long lastSync = lastSyncTime.get(playerId);
        if (lastSync != null && (now - lastSync) < SYNC_DEBOUNCE_MS) return;
        if (!hasInventoryChanged(player)) return;
        
        lastSyncTime.put(playerId, now);
        copyFromPlayer(player);
        syncAllPlayersExcept(player);
    }

    private static boolean hasInventoryChanged(ServerPlayer player) {
        UUID playerId = player.getUUID();
        List<ItemStack> lastKnown = lastKnownInventory.get(playerId);
        if (lastKnown == null) return true;
        
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (!ItemStack.matches(inv.getItem(i), lastKnown.get(i))) return true;
        }
        for (int i = 0; i < 4; i++) {
            if (!ItemStack.matches(inv.armor.get(i), lastKnown.get(ARMOR_START + i))) return true;
        }
        if (!ItemStack.matches(inv.offhand.getFirst(), lastKnown.get(OFFHAND_SLOT))) return true;
        return false;
    }

    private static void updateLastKnownInventory(ServerPlayer player) {
        UUID playerId = player.getUUID();
        List<ItemStack> snapshot = new ArrayList<>(INVENTORY_SIZE);
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) snapshot.add(inv.getItem(i).copy());
        for (int i = 0; i < 4; i++) snapshot.add(inv.armor.get(i).copy());
        snapshot.add(inv.offhand.getFirst().copy());
        lastKnownInventory.put(playerId, snapshot);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (!isEnabled()) return;
        List<ServerPlayer> players = player.server.getPlayerList().getPlayers();
        if (players.size() == 1 || isSharedInventoryEmpty()) {
            copyFromPlayer(player);
        } else {
            applyToPlayer(player);
        }
    }

    public static void onPlayerLeave(ServerPlayer player) {
        UUID playerId = player.getUUID();
        syncingPlayers.remove(playerId);
        lastKnownInventory.remove(playerId);
        playersWithContainerOpen.remove(playerId);
        lastSyncTime.remove(playerId);
        playerSyncVersion.remove(playerId);
    }

    private static boolean isSharedInventoryEmpty() {
        for (ItemStack stack : sharedInventory) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    public static CompoundTag saveToNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag itemList = new ListTag();
        synchronized (inventoryLock) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                if (!sharedInventory.get(i).isEmpty()) {
                    itemTag.put("Item", sharedInventory.get(i).save(provider));
                }
                itemList.add(itemTag);
            }
        }
        tag.put("SharedInventory", itemList);
        tag.putBoolean("Initialized", initialized);
        tag.putLong("SyncVersion", globalSyncVersion.get());
        return tag;
    }

    public static void loadFromNBT(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag == null || !tag.contains("SharedInventory")) {
            initializeEmptyInventory();
            return;
        }
        ListTag itemList = tag.getList("SharedInventory", 10);
        synchronized (inventoryLock) {
            initializeEmptyInventory();
            for (int i = 0; i < itemList.size(); i++) {
                CompoundTag itemTag = itemList.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < INVENTORY_SIZE && itemTag.contains("Item")) {
                    Optional<ItemStack> optStack = ItemStack.parse(provider, itemTag.getCompound("Item"));
                    optStack.ifPresent(stack -> sharedInventory.set(slot, stack));
                }
            }
        }
        initialized = tag.getBoolean("Initialized");
        if (tag.contains("SyncVersion")) {
            globalSyncVersion.set(tag.getLong("SyncVersion"));
        }
    }

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
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SoulLink.LOGGER.info("SharedInventoryManager ready");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        reset();
    }
}
