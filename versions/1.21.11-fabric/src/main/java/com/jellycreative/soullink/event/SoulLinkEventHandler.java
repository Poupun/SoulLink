package com.jellycreative.soullink.event;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.network.SoulLinkNetwork;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Event handler for Soul Link - synchronizes health, hunger, and knockback between linked players.
 */
public class SoulLinkEventHandler {

    private static boolean linkEnabled = true;

    // Track last synced values to prevent feedback loops
    private static final Map<UUID, Float> lastSyncedHealth = new HashMap<>();
    private static final Map<UUID, Integer> lastSyncedFood = new HashMap<>();

    // Track players currently being synced to prevent recursion
    private static final Set<UUID> currentlySyncing = new HashSet<>();

    // Track if message has been sent this session
    private static final Set<UUID> messageSent = new HashSet<>();

    // Hunger sync tracking
    private static int tickCounter = 0;

    public static boolean isLinkEnabled() {
        return linkEnabled;
    }

    public static void setLinkEnabled(boolean enabled) {
        linkEnabled = enabled;
    }

    private static MinecraftServer getServer(ServerPlayerEntity player) {
        return player.getCommandSource().getServer();
    }

    /**
     * Handle server tick for hunger sync
     */
    public static void onServerTick(MinecraftServer server) {
        if (!linkEnabled || !SoulLinkConfig.isSyncHunger()) return;

        // Only sync every 10 ticks (0.5 seconds)
        tickCounter++;
        if (tickCounter < 10) return;
        tickCounter = 0;

        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) return;

        // Find lowest food level
        int lowestFood = 20;
        float lowestSaturation = 20.0f;
        ServerPlayerEntity lowestPlayer = null;

        for (ServerPlayerEntity player : players) {
            int food = player.getHungerManager().getFoodLevel();
            if (food < lowestFood) {
                lowestFood = food;
                lowestSaturation = player.getHungerManager().getSaturationLevel();
                lowestPlayer = player;
            }
        }

        if (lowestPlayer == null) return;

        // Sync to all players
        for (ServerPlayerEntity player : players) {
            UUID uuid = player.getUuid();
            Integer lastFood = lastSyncedFood.get(uuid);

            if (lastFood == null || lastFood != lowestFood) {
                lastSyncedFood.put(uuid, lowestFood);
                player.getHungerManager().setFoodLevel(lowestFood);
                player.getHungerManager().setSaturationLevel(lowestSaturation);
                SoulLinkNetwork.sendSyncHunger(player, lowestFood, lowestSaturation);
            }
        }
    }

    /**
     * Handle player join
     */
    public static void onPlayerJoin(ServerPlayerEntity player) {
        MinecraftServer server = getServer(player);
        if (server == null) return;

        List<ServerPlayerEntity> allPlayers = server.getPlayerManager().getPlayerList();
        
        // Show link enabled message
        if (linkEnabled && SoulLinkConfig.isShowLinkMessages() && !messageSent.contains(player.getUuid())) {
            StringBuilder features = new StringBuilder();
            if (SoulLinkConfig.isSyncHealth()) features.append("Health");
            if (SoulLinkConfig.isSyncHunger()) {
                if (features.length() > 0) features.append(", ");
                features.append("Hunger");
            }
            if (SoulLinkConfig.isSyncKnockback()) {
                if (features.length() > 0) features.append(", ");
                features.append("Knockback");
            }
            if (SoulLinkConfig.isSyncInventory()) {
                if (features.length() > 0) features.append(", ");
                features.append("Inventory");
            }

            player.sendMessage(Text.literal("§6[Soul-Link] §aYou are now linked with all players! (Syncing: " + features + ")"), false);
            messageSent.add(player.getUuid());
        }

        // Sync existing player's health to the new player
        if (allPlayers.size() > 1 && SoulLinkConfig.isSyncHealth()) {
            for (ServerPlayerEntity existingPlayer : allPlayers) {
                if (!existingPlayer.equals(player)) {
                    float health = existingPlayer.getHealth();
                    player.setHealth(health);
                    SoulLinkNetwork.sendSyncHealth(player, health);
                    break;
                }
            }
        }
    }

    /**
     * Handle player leave
     */
    public static void onPlayerLeave(ServerPlayerEntity player) {
        // Clean up tracking data
        lastSyncedHealth.remove(player.getUuid());
        lastSyncedFood.remove(player.getUuid());
        currentlySyncing.remove(player.getUuid());
    }

    /**
     * Handle damage from mixin - called after damage is applied.
     */
    public static void onDamageAfter(LivingEntity entity, DamageSource source, float amount) {
        if (!linkEnabled || !SoulLinkConfig.isSyncHealth()) return;

        if (entity instanceof ServerPlayerEntity damagedPlayer) {
            // Skip if this player is currently being synced
            if (currentlySyncing.contains(damagedPlayer.getUuid())) {
                return;
            }

            // Only run on server side
            if (damagedPlayer.getCommandSource().getServer() == null) return;

            float newHealth = damagedPlayer.getHealth();

            // Record the new health as synced for this player
            lastSyncedHealth.put(damagedPlayer.getUuid(), newHealth);

            // Sync to all other online players
            MinecraftServer server = getServer(damagedPlayer);
            if (server == null) return;

            List<ServerPlayerEntity> allPlayers = server.getPlayerManager().getPlayerList();
            for (ServerPlayerEntity otherPlayer : allPlayers) {
                if (!otherPlayer.equals(damagedPlayer)) {
                    try {
                        currentlySyncing.add(otherPlayer.getUuid());
                        lastSyncedHealth.put(otherPlayer.getUuid(), newHealth);
                        otherPlayer.setHealth(newHealth);
                        SoulLinkNetwork.sendSyncHealth(otherPlayer, newHealth);

                        // Sync knockback if enabled
                        if (SoulLinkConfig.isSyncKnockback() && source.getAttacker() != null) {
                            double dx = otherPlayer.getX() - source.getAttacker().getX();
                            double dz = otherPlayer.getZ() - source.getAttacker().getZ();
                            double dist = Math.sqrt(dx * dx + dz * dz);
                            if (dist > 0.001) {
                                double strength = 0.4;
                                double ratioX = dx / dist;
                                double ratioZ = dz / dist;
                                SoulLinkNetwork.sendKnockback(otherPlayer, strength, ratioX, ratioZ);
                            }
                        }
                    } finally {
                        currentlySyncing.remove(otherPlayer.getUuid());
                    }
                }
            }
        }
    }
}
