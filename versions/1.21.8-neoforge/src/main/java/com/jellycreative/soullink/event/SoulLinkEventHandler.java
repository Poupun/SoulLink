package com.jellycreative.soullink.event;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.network.KnockbackPayload;
import com.jellycreative.soullink.network.SyncHealthPayload;
import com.jellycreative.soullink.network.SyncHungerPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * Event handler for Soul Link - synchronizes health, hunger, and knockback between linked players.
 */
@EventBusSubscriber(modid = SoulLink.MOD_ID)
public class SoulLinkEventHandler {

    // Track which players are currently linked
    private static boolean linkEnabled = true;

    // Track last synced values to prevent feedback loops
    private static final Map<UUID, Float> lastSyncedHealth = new HashMap<>();
    private static final Map<UUID, Integer> lastSyncedFood = new HashMap<>();

    // Track players currently being synced to prevent recursion
    private static final Set<UUID> currentlySyncing = new HashSet<>();

    // Track players currently taking knockback to prevent recursion
    private static final Set<UUID> currentlyKnockingBack = new HashSet<>();

    // Track if message has been sent this session
    private static final Set<UUID> messageSent = new HashSet<>();

    public static boolean isLinkEnabled() {
        return linkEnabled;
    }

    public static void setLinkEnabled(boolean enabled) {
        linkEnabled = enabled;
    }

    /**
     * Handle damage events - sync health reduction to all linked players.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!linkEnabled || !SoulLinkConfig.SYNC_HEALTH.get()) return;

        if (event.getEntity() instanceof ServerPlayer damagedPlayer) {
            // Skip if this player is currently being synced
            if (currentlySyncing.contains(damagedPlayer.getUUID())) {
                return;
            }

            float newHealth = damagedPlayer.getHealth();

            // Record the new health as synced for this player
            lastSyncedHealth.put(damagedPlayer.getUUID(), newHealth);

            // Sync to all other online players
            List<ServerPlayer> allPlayers = damagedPlayer.getServer().getPlayerList().getPlayers();
            for (ServerPlayer otherPlayer : allPlayers) {
                if (!otherPlayer.equals(damagedPlayer)) {
                    try {
                        currentlySyncing.add(otherPlayer.getUUID());
                        lastSyncedHealth.put(otherPlayer.getUUID(), newHealth);
                        otherPlayer.setHealth(newHealth);
                        PacketDistributor.sendToPlayer(otherPlayer, new SyncHealthPayload(newHealth));
                    } finally {
                        currentlySyncing.remove(otherPlayer.getUUID());
                    }
                }
            }
        }
    }

    /**
     * Handle knockback events - share knockback between all linked players.
     */
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!linkEnabled || !SoulLinkConfig.SYNC_KNOCKBACK.get()) return;

        if (event.getEntity() instanceof ServerPlayer knockedPlayer) {
            // Skip if player is blocking with a shield
            if (knockedPlayer.isBlocking()) {
                return;
            }

            // Skip if this player is currently receiving knockback from sync
            if (currentlyKnockingBack.contains(knockedPlayer.getUUID())) {
                return;
            }

            double strength = event.getStrength();
            double ratioX = event.getRatioX();
            double ratioZ = event.getRatioZ();

            // Sync knockback to all other players
            List<ServerPlayer> allPlayers = knockedPlayer.getServer().getPlayerList().getPlayers();
            for (ServerPlayer otherPlayer : allPlayers) {
                if (!otherPlayer.equals(knockedPlayer)) {
                    // Skip players who are blocking
                    if (otherPlayer.isBlocking()) {
                        continue;
                    }

                    try {
                        currentlyKnockingBack.add(otherPlayer.getUUID());

                        // Send knockback packet to client
                        PacketDistributor.sendToPlayer(otherPlayer, new KnockbackPayload(strength, ratioX, ratioZ));

                        // Also apply server-side knockback
                        otherPlayer.knockback(strength * 0.5, ratioX, ratioZ);
                    } finally {
                        currentlyKnockingBack.remove(otherPlayer.getUUID());
                    }
                }
            }
        }
    }

    /**
     * Handle player tick - sync hunger periodically.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!linkEnabled || !SoulLinkConfig.SYNC_HUNGER.get()) return;

        Player player = event.getEntity();

        if (player instanceof ServerPlayer serverPlayer) {
            // Only sync every 10 ticks (0.5 seconds) to reduce network traffic
            if (serverPlayer.tickCount % 10 != 0) return;

            int currentFood = serverPlayer.getFoodData().getFoodLevel();
            float currentSaturation = serverPlayer.getFoodData().getSaturationLevel();

            // Check if food changed
            Integer lastFood = lastSyncedFood.get(serverPlayer.getUUID());
            if (lastFood != null && lastFood == currentFood) {
                return; // No change
            }

            // Skip if this player is currently being synced
            if (currentlySyncing.contains(serverPlayer.getUUID())) {
                return;
            }

            lastSyncedFood.put(serverPlayer.getUUID(), currentFood);

            // Sync to all other players
            List<ServerPlayer> allPlayers = serverPlayer.getServer().getPlayerList().getPlayers();
            for (ServerPlayer otherPlayer : allPlayers) {
                if (!otherPlayer.equals(serverPlayer)) {
                    Integer otherLastFood = lastSyncedFood.get(otherPlayer.getUUID());
                    if (otherLastFood != null && otherLastFood == currentFood) {
                        continue; // Already synced
                    }

                    try {
                        currentlySyncing.add(otherPlayer.getUUID());
                        lastSyncedFood.put(otherPlayer.getUUID(), currentFood);
                        otherPlayer.getFoodData().setFoodLevel(currentFood);
                        otherPlayer.getFoodData().setSaturation(currentSaturation);
                        PacketDistributor.sendToPlayer(otherPlayer, new SyncHungerPayload(currentFood, currentSaturation));
                    } finally {
                        currentlySyncing.remove(otherPlayer.getUUID());
                    }
                }
            }
        }
    }

    /**
     * Handle player login - show link status message.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Clear tracking for this player
            lastSyncedHealth.remove(serverPlayer.getUUID());
            lastSyncedFood.remove(serverPlayer.getUUID());
            messageSent.remove(serverPlayer.getUUID());

            // Send status message
            if (!messageSent.contains(serverPlayer.getUUID())) {
                messageSent.add(serverPlayer.getUUID());

                if (linkEnabled) {
                    StringBuilder features = new StringBuilder();
                    if (SoulLinkConfig.SYNC_HEALTH.get()) features.append("Health");
                    if (SoulLinkConfig.SYNC_HUNGER.get()) {
                        if (features.length() > 0) features.append(", ");
                        features.append("Hunger");
                    }
                    if (SoulLinkConfig.SYNC_KNOCKBACK.get()) {
                        if (features.length() > 0) features.append(", ");
                        features.append("Knockback");
                    }
                    if (SoulLinkConfig.SYNC_INVENTORY.get()) {
                        if (features.length() > 0) features.append(", ");
                        features.append("Inventory");
                    }

                    serverPlayer.sendSystemMessage(Component.literal("§6[Soul-Link] §aLink is enabled! Syncing: " + features));
                } else {
                    serverPlayer.sendSystemMessage(Component.literal("§6[Soul-Link] §cLink is currently disabled."));
                }
            }
        }
    }

    /**
     * Handle player logout - clean up tracking.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof Player player) {
            lastSyncedHealth.remove(player.getUUID());
            lastSyncedFood.remove(player.getUUID());
            currentlySyncing.remove(player.getUUID());
            currentlyKnockingBack.remove(player.getUUID());
            messageSent.remove(player.getUUID());
        }
    }
}
