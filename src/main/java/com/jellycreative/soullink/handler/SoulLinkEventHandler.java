package com.jellycreative.soullink.handler;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.network.KnockbackPacket;
import com.jellycreative.soullink.network.SoulLinkNetwork;
import com.jellycreative.soullink.network.SyncHealthPacket;
import com.jellycreative.soullink.network.SyncHungerPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main event handler for Soul-Link.
 * Handles damage, healing, knockback, and hunger synchronization between all players.
 */
public class SoulLinkEventHandler {
    
    // Track players being processed to prevent infinite loops
    private static final Set<UUID> processingDamage = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> processingHealing = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> processingKnockback = ConcurrentHashMap.newKeySet();
    
    // Track previous hunger values for each player to detect changes
    private static final Map<UUID, Integer> previousFoodLevel = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> previousSaturation = new ConcurrentHashMap<>();
    
    // Flag to prevent hunger sync loops
    private static final Set<UUID> processingHunger = ConcurrentHashMap.newKeySet();
    
    // Cooldown to prevent spam damage from synchronized effects
    private static final Map<UUID, Long> damageCooldown = new ConcurrentHashMap<>();
    private static final long DAMAGE_COOLDOWN_MS = 50; // 50ms cooldown

    /**
     * Handle player damage - sync to all other players
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer damagedPlayer)) {
            return;
        }
        
        if (!SoulLinkConfig.LINK_DAMAGE.get()) {
            return;
        }
        
        // Check if we're already processing damage for this player (prevent loops)
        if (processingDamage.contains(damagedPlayer.getUUID())) {
            return;
        }
        
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        Long lastDamageTime = damageCooldown.get(damagedPlayer.getUUID());
        if (lastDamageTime != null && currentTime - lastDamageTime < DAMAGE_COOLDOWN_MS) {
            return;
        }
        
        // Get all online players
        List<ServerPlayer> allPlayers = damagedPlayer.server.getPlayerList().getPlayers();
        
        // Check minimum players requirement
        if (allPlayers.size() < SoulLinkConfig.MIN_PLAYERS_FOR_LINK.get()) {
            return;
        }
        
        // Prevent PvP loop if configured
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (SoulLinkConfig.PREVENT_PLAYER_VS_PLAYER_LOOP.get() && attacker instanceof Player) {
            // If another player caused this damage, check if that player is currently processing damage
            if (processingDamage.contains(((Player) attacker).getUUID())) {
                return;
            }
        }
        
        float damage = event.getAmount();
        float linkedDamage = (float) (damage * SoulLinkConfig.DAMAGE_MULTIPLIER.get());
        
        if (linkedDamage <= 0) {
            return;
        }
        
        // Mark this player as being processed
        processingDamage.add(damagedPlayer.getUUID());
        damageCooldown.put(damagedPlayer.getUUID(), currentTime);
        
        try {
            // Create a custom damage source for linked damage
            DamageSource linkedSource = damagedPlayer.level().damageSources().magic();
            
            for (ServerPlayer otherPlayer : allPlayers) {
                if (otherPlayer.getUUID().equals(damagedPlayer.getUUID())) {
                    continue; // Skip the original damaged player
                }
                
                if (otherPlayer.isDeadOrDying()) {
                    continue; // Skip dead players
                }
                
                // Mark other player as processing to prevent recursive loops
                processingDamage.add(otherPlayer.getUUID());
                damageCooldown.put(otherPlayer.getUUID(), currentTime);
                
                try {
                    // Apply damage to linked player
                    otherPlayer.hurt(linkedSource, linkedDamage);
                    
                    // Sync health to client
                    SoulLinkNetwork.sendToPlayer(new SyncHealthPacket(otherPlayer.getHealth()), otherPlayer);
                    
                    if (SoulLinkConfig.SHOW_LINK_MESSAGES.get()) {
                        otherPlayer.displayClientMessage(
                                Component.literal("§c[Soul-Link] §7You felt §c" + damagedPlayer.getName().getString() + "'s§7 pain!"),
                                true
                        );
                    }
                } finally {
                    processingDamage.remove(otherPlayer.getUUID());
                }
            }
        } finally {
            // Use a delayed removal to prevent rapid re-triggering
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    processingDamage.remove(damagedPlayer.getUUID());
                } catch (InterruptedException e) {
                    processingDamage.remove(damagedPlayer.getUUID());
                }
            }).start();
        }
    }

    /**
     * Handle player healing - sync to all other players
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer healedPlayer)) {
            return;
        }
        
        if (!SoulLinkConfig.LINK_HEALING.get()) {
            return;
        }
        
        // Check if we're already processing healing for this player (prevent loops)
        if (processingHealing.contains(healedPlayer.getUUID())) {
            return;
        }
        
        // Get all online players
        List<ServerPlayer> allPlayers = healedPlayer.server.getPlayerList().getPlayers();
        
        // Check minimum players requirement
        if (allPlayers.size() < SoulLinkConfig.MIN_PLAYERS_FOR_LINK.get()) {
            return;
        }
        
        float healing = event.getAmount();
        float linkedHealing = (float) (healing * SoulLinkConfig.HEALING_MULTIPLIER.get());
        
        if (linkedHealing <= 0) {
            return;
        }
        
        // Mark this player as being processed
        processingHealing.add(healedPlayer.getUUID());
        
        try {
            for (ServerPlayer otherPlayer : allPlayers) {
                if (otherPlayer.getUUID().equals(healedPlayer.getUUID())) {
                    continue; // Skip the original healed player
                }
                
                if (otherPlayer.isDeadOrDying()) {
                    continue; // Skip dead players
                }
                
                // Mark other player as processing
                processingHealing.add(otherPlayer.getUUID());
                
                try {
                    // Apply healing to linked player
                    otherPlayer.heal(linkedHealing);
                    
                    // Sync health to client
                    SoulLinkNetwork.sendToPlayer(new SyncHealthPacket(otherPlayer.getHealth()), otherPlayer);
                    
                    if (SoulLinkConfig.SHOW_LINK_MESSAGES.get()) {
                        otherPlayer.displayClientMessage(
                                Component.literal("§a[Soul-Link] §7You felt §a" + healedPlayer.getName().getString() + "'s§7 vitality!"),
                                true
                        );
                    }
                } finally {
                    processingHealing.remove(otherPlayer.getUUID());
                }
            }
        } finally {
            processingHealing.remove(healedPlayer.getUUID());
        }
    }

    /**
     * Handle knockback - sync to all other players
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingKnockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer knockedPlayer)) {
            return;
        }
        
        if (!SoulLinkConfig.LINK_KNOCKBACK.get()) {
            return;
        }
        
        // Don't sync knockback if the event was canceled (e.g., shield block)
        if (event.isCanceled()) {
            return;
        }
        
        // Check if the player is blocking with a shield
        if (knockedPlayer.isBlocking()) {
            return;
        }
        
        // Check if we're already processing knockback for this player (prevent loops)
        if (processingKnockback.contains(knockedPlayer.getUUID())) {
            return;
        }
        
        // Get all online players
        List<ServerPlayer> allPlayers = knockedPlayer.server.getPlayerList().getPlayers();
        
        // Check minimum players requirement
        if (allPlayers.size() < SoulLinkConfig.MIN_PLAYERS_FOR_LINK.get()) {
            return;
        }
        
        // Get knockback values - use the actual event values which are already calculated correctly
        // strength is typically 0.4 for normal attacks, ratioX/Z are direction components
        double strength = event.getStrength() * SoulLinkConfig.KNOCKBACK_MULTIPLIER.get();
        double ratioX = event.getRatioX();
        double ratioZ = event.getRatioZ();
        
        // Calculate knockback vector similar to how vanilla does it in LivingEntity.knockback()
        // The ratios are already normalized direction vectors
        // Use a much smaller base value to match vanilla feel
        double horizontalStrength = strength * 0.5D; // Reduce horizontal knockback
        double verticalStrength = Math.min(0.4D, strength * 0.4D); // Cap vertical at 0.4
        
        Vec3 knockbackVec = new Vec3(-ratioX * horizontalStrength, verticalStrength, -ratioZ * horizontalStrength);
        
        // Mark this player as being processed
        processingKnockback.add(knockedPlayer.getUUID());
        
        try {
            for (ServerPlayer otherPlayer : allPlayers) {
                if (otherPlayer.getUUID().equals(knockedPlayer.getUUID())) {
                    continue; // Skip the original knocked player
                }
                
                if (otherPlayer.isDeadOrDying()) {
                    continue; // Skip dead players
                }
                
                // Send knockback packet to the other player
                SoulLinkNetwork.sendToPlayer(new KnockbackPacket(knockbackVec), otherPlayer);
            }
        } finally {
            processingKnockback.remove(knockedPlayer.getUUID());
        }
    }

    /**
     * Handle player death - optionally kill all linked players
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) {
            return;
        }
        
        if (!SoulLinkConfig.SHARE_DEATH.get()) {
            return;
        }
        
        // Get all online players
        List<ServerPlayer> allPlayers = deadPlayer.server.getPlayerList().getPlayers();
        
        // Check minimum players requirement
        if (allPlayers.size() < SoulLinkConfig.MIN_PLAYERS_FOR_LINK.get()) {
            return;
        }
        
        // Create a death source
        DamageSource deathSource = deadPlayer.level().damageSources().magic();
        
        for (ServerPlayer otherPlayer : allPlayers) {
            if (otherPlayer.getUUID().equals(deadPlayer.getUUID())) {
                continue;
            }
            
            if (otherPlayer.isDeadOrDying()) {
                continue;
            }
            
            // Mark as processing to prevent loops
            processingDamage.add(otherPlayer.getUUID());
            
            try {
                // Send death message
                if (SoulLinkConfig.SHOW_LINK_MESSAGES.get()) {
                    otherPlayer.displayClientMessage(
                            Component.literal("§4[Soul-Link] §c" + deadPlayer.getName().getString() + " died. Your souls are linked in death!"),
                            false
                    );
                }
                
                // Kill the linked player
                otherPlayer.hurt(deathSource, Float.MAX_VALUE);
            } finally {
                processingDamage.remove(otherPlayer.getUUID());
            }
        }
    }

    /**
     * Track hunger changes every tick
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        if (!(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        if (!SoulLinkConfig.LINK_HUNGER.get() && !SoulLinkConfig.LINK_SATURATION.get()) {
            return;
        }
        
        // Check if we're processing hunger for this player
        if (processingHunger.contains(serverPlayer.getUUID())) {
            return;
        }
        
        UUID playerId = serverPlayer.getUUID();
        int currentFoodLevel = serverPlayer.getFoodData().getFoodLevel();
        float currentSaturation = serverPlayer.getFoodData().getSaturationLevel();
        
        // Get previous values (defaults if not tracked yet)
        int prevFood = previousFoodLevel.getOrDefault(playerId, currentFoodLevel);
        float prevSat = previousSaturation.getOrDefault(playerId, currentSaturation);
        
        // Calculate changes
        int foodChange = currentFoodLevel - prevFood;
        float satChange = currentSaturation - prevSat;
        
        // Update stored values
        previousFoodLevel.put(playerId, currentFoodLevel);
        previousSaturation.put(playerId, currentSaturation);
        
        // Only proceed if there's a change
        if (foodChange == 0 && Math.abs(satChange) < 0.001f) {
            return;
        }
        
        // Get all online players
        List<ServerPlayer> allPlayers = serverPlayer.server.getPlayerList().getPlayers();
        
        // Check minimum players requirement
        if (allPlayers.size() < SoulLinkConfig.MIN_PLAYERS_FOR_LINK.get()) {
            return;
        }
        
        // Apply multiplier
        float multiplier = SoulLinkConfig.HUNGER_MULTIPLIER.get().floatValue();
        int linkedFoodChange = Math.round(foodChange * multiplier);
        float linkedSatChange = satChange * multiplier;
        
        // Mark this player as processing
        processingHunger.add(playerId);
        
        try {
            for (ServerPlayer otherPlayer : allPlayers) {
                if (otherPlayer.getUUID().equals(playerId)) {
                    continue;
                }
                
                if (otherPlayer.isDeadOrDying()) {
                    continue;
                }
                
                // Mark other player as processing
                processingHunger.add(otherPlayer.getUUID());
                
                try {
                    // Apply hunger changes
                    if (SoulLinkConfig.LINK_HUNGER.get() && linkedFoodChange != 0) {
                        int newFoodLevel = Math.max(0, Math.min(20, 
                                otherPlayer.getFoodData().getFoodLevel() + linkedFoodChange));
                        otherPlayer.getFoodData().setFoodLevel(newFoodLevel);
                        
                        // Update tracked value for the other player
                        previousFoodLevel.put(otherPlayer.getUUID(), newFoodLevel);
                    }
                    
                    if (SoulLinkConfig.LINK_SATURATION.get() && Math.abs(linkedSatChange) > 0.001f) {
                        float newSaturation = Math.max(0, Math.min(20, 
                                otherPlayer.getFoodData().getSaturationLevel() + linkedSatChange));
                        otherPlayer.getFoodData().setSaturation(newSaturation);
                        
                        // Update tracked value
                        previousSaturation.put(otherPlayer.getUUID(), newSaturation);
                    }
                    
                    // Sync hunger to client
                    SoulLinkNetwork.sendToPlayer(new SyncHungerPacket(
                            otherPlayer.getFoodData().getFoodLevel(),
                            otherPlayer.getFoodData().getSaturationLevel()
                    ), otherPlayer);
                    
                    // Show message for significant changes
                    if (SoulLinkConfig.SHOW_LINK_MESSAGES.get() && Math.abs(linkedFoodChange) >= 1) {
                        String message = linkedFoodChange > 0 
                                ? "§a[Soul-Link] §7You shared §a" + serverPlayer.getName().getString() + "'s§7 meal!"
                                : "§6[Soul-Link] §7You felt §6" + serverPlayer.getName().getString() + "'s§7 hunger!";
                        otherPlayer.displayClientMessage(Component.literal(message), true);
                    }
                } finally {
                    processingHunger.remove(otherPlayer.getUUID());
                }
            }
        } finally {
            processingHunger.remove(playerId);
        }
    }

    /**
     * Clean up player data when they log out
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        previousFoodLevel.remove(playerId);
        previousSaturation.remove(playerId);
        processingDamage.remove(playerId);
        processingHealing.remove(playerId);
        processingKnockback.remove(playerId);
        processingHunger.remove(playerId);
        damageCooldown.remove(playerId);
    }

    /**
     * Initialize player tracking when they log in
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UUID playerId = serverPlayer.getUUID();
            previousFoodLevel.put(playerId, serverPlayer.getFoodData().getFoodLevel());
            previousSaturation.put(playerId, serverPlayer.getFoodData().getSaturationLevel());
            
            // Send welcome message
            if (SoulLinkConfig.SHOW_LINK_MESSAGES.get()) {
                int playerCount = serverPlayer.server.getPlayerList().getPlayers().size();
                serverPlayer.displayClientMessage(
                        Component.literal("§d[Soul-Link] §7Your soul is now linked with §d" + (playerCount - 1) + "§7 other player(s)!"),
                        false
                );
            }
        }
    }
}
