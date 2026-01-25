package com.jellycreative.soullink.event;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.network.KnockbackPacket;
import com.jellycreative.soullink.network.SoulLinkNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles all Soul-Link events for damage, healing, knockback, and hunger synchronization.
 */
@Mod.EventBusSubscriber(modid = SoulLink.MOD_ID)
public class SoulLinkEventHandler {
    
    // Track which players are currently having damage applied to prevent infinite loops
    private static final Set<UUID> processingDamage = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> processingHealing = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> processingKnockback = ConcurrentHashMap.newKeySet();
    
    // Track hunger for each player to detect changes
    private static final Map<UUID, Integer> lastFoodLevel = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> lastSaturation = new ConcurrentHashMap<>();
    
    // Track if we're currently syncing hunger to prevent loops
    private static final Set<UUID> syncingHunger = ConcurrentHashMap.newKeySet();

    /**
     * Handle damage events - share damage with all other players
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDamage(LivingDamageEvent event) {
        if (!SoulLinkConfig.LINK_DAMAGE.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer damagedPlayer)) return;
        
        // Prevent infinite loops
        if (processingDamage.contains(damagedPlayer.getUUID())) return;
        
        float damage = event.getAmount();
        if (damage <= 0) return;
        
        // Apply multiplier
        float sharedDamage = damage * SoulLinkConfig.DAMAGE_MULTIPLIER.get().floatValue();
        
        // Share damage with all other players
        for (ServerPlayer otherPlayer : damagedPlayer.server.getPlayerList().getPlayers()) {
            if (otherPlayer.getUUID().equals(damagedPlayer.getUUID())) continue;
            
            processingDamage.add(otherPlayer.getUUID());
            try {
                otherPlayer.hurt(damagedPlayer.damageSources().generic(), sharedDamage);
            } finally {
                processingDamage.remove(otherPlayer.getUUID());
            }
        }
        
        if (SoulLinkConfig.SHOW_LINK_MESSAGES.get()) {
            SoulLink.LOGGER.debug("Shared {} damage from {} to all players", sharedDamage, damagedPlayer.getName().getString());
        }
    }

    /**
     * Handle healing events - share healing with all other players
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerHeal(LivingHealEvent event) {
        if (!SoulLinkConfig.LINK_HEALING.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer healedPlayer)) return;
        
        // Prevent infinite loops
        if (processingHealing.contains(healedPlayer.getUUID())) return;
        
        float healing = event.getAmount();
        if (healing <= 0) return;
        
        // Apply multiplier
        float sharedHealing = healing * SoulLinkConfig.HEALING_MULTIPLIER.get().floatValue();
        
        // Share healing with all other players
        for (ServerPlayer otherPlayer : healedPlayer.server.getPlayerList().getPlayers()) {
            if (otherPlayer.getUUID().equals(healedPlayer.getUUID())) continue;
            
            processingHealing.add(otherPlayer.getUUID());
            try {
                otherPlayer.heal(sharedHealing);
            } finally {
                processingHealing.remove(otherPlayer.getUUID());
            }
        }
        
        if (SoulLinkConfig.SHOW_LINK_MESSAGES.get()) {
            SoulLink.LOGGER.debug("Shared {} healing from {} to all players", sharedHealing, healedPlayer.getName().getString());
        }
    }

    /**
     * Handle knockback events - share knockback with all other players
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerKnockback(LivingKnockBackEvent event) {
        if (!SoulLinkConfig.LINK_KNOCKBACK.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer knockedPlayer)) return;
        
        // Prevent infinite loops
        if (processingKnockback.contains(knockedPlayer.getUUID())) return;
        
        // Don't share knockback if the event was already canceled
        if (event.isCanceled()) return;
        
        // Don't apply knockback if the player is blocking with a shield
        if (knockedPlayer.isBlocking()) return;
        
        double strength = event.getStrength() * SoulLinkConfig.KNOCKBACK_MULTIPLIER.get();
        double ratioX = event.getRatioX();
        double ratioZ = event.getRatioZ();
        
        // Share knockback with all other players via network packet
        for (ServerPlayer otherPlayer : knockedPlayer.server.getPlayerList().getPlayers()) {
            if (otherPlayer.getUUID().equals(knockedPlayer.getUUID())) continue;
            
            // Don't knock back players who are blocking
            if (otherPlayer.isBlocking()) continue;
            
            processingKnockback.add(otherPlayer.getUUID());
            try {
                // Send knockback packet to client
                SoulLinkNetwork.sendToPlayer(new KnockbackPacket(strength, ratioX, ratioZ), otherPlayer);
            } finally {
                processingKnockback.remove(otherPlayer.getUUID());
            }
        }
        
        if (SoulLinkConfig.SHOW_LINK_MESSAGES.get()) {
            SoulLink.LOGGER.debug("Shared knockback from {} to all players", knockedPlayer.getName().getString());
        }
    }

    /**
     * Handle player death - sync health to prevent issues
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) return;
        
        // Clear processing flags for the dead player
        processingDamage.remove(deadPlayer.getUUID());
        processingHealing.remove(deadPlayer.getUUID());
        processingKnockback.remove(deadPlayer.getUUID());
        syncingHunger.remove(deadPlayer.getUUID());
        lastFoodLevel.remove(deadPlayer.getUUID());
        lastSaturation.remove(deadPlayer.getUUID());
    }

    /**
     * Handle player tick - sync hunger changes
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!SoulLinkConfig.LINK_HUNGER.get()) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        
        UUID playerId = player.getUUID();
        
        // Don't process if we're syncing this player
        if (syncingHunger.contains(playerId)) return;
        
        int currentFood = player.getFoodData().getFoodLevel();
        float currentSaturation = player.getFoodData().getSaturationLevel();
        
        Integer lastFood = lastFoodLevel.get(playerId);
        Float lastSat = lastSaturation.get(playerId);
        
        // Check if hunger changed
        if (lastFood != null && lastSat != null) {
            int foodDiff = currentFood - lastFood;
            float satDiff = currentSaturation - lastSat;
            
            // Apply multiplier to changes
            if (foodDiff != 0 || Math.abs(satDiff) > 0.01f) {
                int sharedFoodDiff = (int) (foodDiff * SoulLinkConfig.HUNGER_MULTIPLIER.get());
                float sharedSatDiff = satDiff * SoulLinkConfig.HUNGER_MULTIPLIER.get().floatValue();
                
                // Sync to all other players
                for (ServerPlayer otherPlayer : player.server.getPlayerList().getPlayers()) {
                    if (otherPlayer.getUUID().equals(playerId)) continue;
                    
                    syncingHunger.add(otherPlayer.getUUID());
                    try {
                        int newFood = Math.max(0, Math.min(20, otherPlayer.getFoodData().getFoodLevel() + sharedFoodDiff));
                        float newSat = Math.max(0, Math.min(newFood, otherPlayer.getFoodData().getSaturationLevel() + sharedSatDiff));
                        
                        otherPlayer.getFoodData().setFoodLevel(newFood);
                        otherPlayer.getFoodData().setSaturation(newSat);
                        
                        // Update their last known values
                        lastFoodLevel.put(otherPlayer.getUUID(), newFood);
                        lastSaturation.put(otherPlayer.getUUID(), newSat);
                    } finally {
                        syncingHunger.remove(otherPlayer.getUUID());
                    }
                }
            }
        }
        
        // Update last known values
        lastFoodLevel.put(playerId, currentFood);
        lastSaturation.put(playerId, currentSaturation);
    }
}
