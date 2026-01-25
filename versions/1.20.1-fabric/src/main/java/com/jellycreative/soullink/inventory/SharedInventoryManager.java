package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages synchronization of shared inventory between all players.
 */
public class SharedInventoryManager {

    private static boolean syncLock = false;

    /**
     * Synchronize FROM a player's inventory TO the shared storage.
     */
    public static void syncFromPlayer(ServerPlayerEntity player) {
        if (syncLock) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        ServerWorld overworld = server.getOverworld();
        SharedInventorySavedData data = SharedInventorySavedData.get(overworld);

        // Main inventory (0-35)
        List<ItemStack> mainInventory = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            mainInventory.add(player.getInventory().getStack(i).copy());
        }

        // Armor (36-39)
        List<ItemStack> armor = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            armor.add(player.getInventory().getArmorStack(i).copy());
        }

        // Offhand
        ItemStack offhand = player.getInventory().offHand.get(0).copy();

        // Update shared data
        data.setMainInventory(mainInventory);
        data.setArmor(armor);
        data.setOffhand(offhand);
        data.markDirty();

        // Sync to all other players
        syncToAllPlayersExcept(server, player);
    }

    /**
     * Synchronize TO a player FROM the shared storage.
     */
    public static void syncToPlayer(ServerPlayerEntity player) {
        if (syncLock) return;
        syncLock = true;

        try {
            MinecraftServer server = player.getServer();
            if (server == null) return;

            ServerWorld overworld = server.getOverworld();
            SharedInventorySavedData data = SharedInventorySavedData.get(overworld);

            // Clear current inventory first
            player.getInventory().clear();

            // Main inventory
            List<ItemStack> mainInventory = data.getMainInventory();
            for (int i = 0; i < Math.min(36, mainInventory.size()); i++) {
                player.getInventory().setStack(i, mainInventory.get(i).copy());
            }

            // Armor
            List<ItemStack> armor = data.getArmor();
            for (int i = 0; i < Math.min(4, armor.size()); i++) {
                player.getInventory().armor.set(i, armor.get(i).copy());
            }

            // Offhand
            ItemStack offhand = data.getOffhand();
            if (offhand != null) {
                player.getInventory().offHand.set(0, offhand.copy());
            }

            // Update client
            player.currentScreenHandler.sendContentUpdates();
        } finally {
            syncLock = false;
        }
    }

    /**
     * Sync shared inventory to all players except the source.
     */
    public static void syncToAllPlayersExcept(MinecraftServer server, ServerPlayerEntity except) {
        if (syncLock) return;

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player != except) {
                    syncToPlayer(player);
                }
            }
        }
    }

    /**
     * Sync shared inventory to all players.
     */
    public static void syncToAllPlayers(MinecraftServer server) {
        if (syncLock) return;

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                syncToPlayer(player);
            }
        }
    }

    /**
     * Clear the shared inventory.
     */
    public static void clearSharedInventory(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        SharedInventorySavedData data = SharedInventorySavedData.get(overworld);
        data.clear();
        data.markDirty();

        // Clear all player inventories
        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                player.getInventory().clear();
                player.currentScreenHandler.sendContentUpdates();
            }
        }
    }
}
