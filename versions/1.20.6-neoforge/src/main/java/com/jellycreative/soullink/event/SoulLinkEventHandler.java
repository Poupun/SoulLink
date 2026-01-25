package com.jellycreative.soullink.event;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.network.KnockbackPayload;
import com.jellycreative.soullink.network.SoulLinkNetwork;
import com.jellycreative.soullink.network.SyncHealthPayload;
import com.jellycreative.soullink.network.SyncHungerPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event handler for Soul Link - synchronizes health, hunger, and knockback between linked players.
 */
@EventBusSubscriber(modid = SoulLink.MOD_ID)
public class SoulLinkEventHandler {
    
    private static boolean linkEnabled = true;
    
    private static final Map<UUID, Float> lastSyncedHealth = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lastSyncedFood = new ConcurrentHashMap<>();
    private static final Set<UUID> currentlySyncing = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> currentlyKnockingBack = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> messageSent = ConcurrentHashMap.newKeySet();
    
    public static boolean isLinkEnabled() {
        return linkEnabled;
    }
    
    public static void setLinkEnabled(boolean enabled) {
        linkEnabled = enabled;
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!linkEnabled || !SoulLinkConfig.LINK_DAMAGE.get()) return;
        
        if (event.getEntity() instanceof ServerPlayer damagedPlayer) {
            if (currentlySyncing.contains(damagedPlayer.getUUID())) {
                return;
            }
            
            float damage = event.getAmount();
            float newHealth = Math.max(0, damagedPlayer.getHealth() - damage);
            float sharedDamage = damage * SoulLinkConfig.DAMAGE_MULTIPLIER.get().floatValue();
            
            lastSyncedHealth.put(damagedPlayer.getUUID(), newHealth);
            
            List<ServerPlayer> allPlayers = damagedPlayer.server.getPlayerList().getPlayers();
            for (ServerPlayer otherPlayer : allPlayers) {
                if (!otherPlayer.equals(damagedPlayer)) {
                    try {
                        currentlySyncing.add(otherPlayer.getUUID());
                        float otherNewHealth = Math.max(0, otherPlayer.getHealth() - sharedDamage);
                        lastSyncedHealth.put(otherPlayer.getUUID(), otherNewHealth);
                        otherPlayer.hurt(damagedPlayer.damageSources().generic(), sharedDamage);
                        SoulLinkNetwork.sendToPlayer(new SyncHealthPayload(otherNewHealth), otherPlayer);
                    } finally {
                        currentlySyncing.remove(otherPlayer.getUUID());
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!linkEnabled || !SoulLinkConfig.LINK_KNOCKBACK.get()) return;
        
        if (event.getEntity() instanceof ServerPlayer knockedPlayer) {
            if (knockedPlayer.isBlocking()) {
                return;
            }
            
            if (currentlyKnockingBack.contains(knockedPlayer.getUUID())) {
                return;
            }
            
            double strength = event.getStrength() * SoulLinkConfig.KNOCKBACK_MULTIPLIER.get();
            double ratioX = event.getRatioX();
            double ratioZ = event.getRatioZ();
            
            List<ServerPlayer> allPlayers = knockedPlayer.server.getPlayerList().getPlayers();
            for (ServerPlayer otherPlayer : allPlayers) {
                if (!otherPlayer.equals(knockedPlayer)) {
                    if (otherPlayer.isBlocking()) {
                        continue;
                    }
                    
                    try {
                        currentlyKnockingBack.add(otherPlayer.getUUID());
                        SoulLinkNetwork.sendToPlayer(new KnockbackPayload(strength, ratioX, ratioZ), otherPlayer);
                        otherPlayer.knockback(strength * 0.5, ratioX, ratioZ);
                    } finally {
                        currentlyKnockingBack.remove(otherPlayer.getUUID());
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!linkEnabled || !SoulLinkConfig.LINK_HUNGER.get()) return;
        
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.tickCount % 10 != 0) return;
            
            int currentFood = serverPlayer.getFoodData().getFoodLevel();
            float currentSaturation = serverPlayer.getFoodData().getSaturationLevel();
            
            Integer lastFood = lastSyncedFood.get(serverPlayer.getUUID());
            if (lastFood != null && lastFood == currentFood) {
                return;
            }
            
            if (currentlySyncing.contains(serverPlayer.getUUID())) {
                return;
            }
            
            lastSyncedFood.put(serverPlayer.getUUID(), currentFood);
            
            List<ServerPlayer> allPlayers = serverPlayer.server.getPlayerList().getPlayers();
            for (ServerPlayer otherPlayer : allPlayers) {
                if (!otherPlayer.equals(serverPlayer)) {
                    Integer otherLastFood = lastSyncedFood.get(otherPlayer.getUUID());
                    if (otherLastFood != null && otherLastFood == currentFood) {
                        continue;
                    }
                    
                    try {
                        currentlySyncing.add(otherPlayer.getUUID());
                        lastSyncedFood.put(otherPlayer.getUUID(), currentFood);
                        otherPlayer.getFoodData().setFoodLevel(currentFood);
                        otherPlayer.getFoodData().setSaturation(currentSaturation);
                        SoulLinkNetwork.sendToPlayer(new SyncHungerPayload(currentFood, currentSaturation), otherPlayer);
                    } finally {
                        currentlySyncing.remove(otherPlayer.getUUID());
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            lastSyncedHealth.remove(serverPlayer.getUUID());
            lastSyncedFood.remove(serverPlayer.getUUID());
            messageSent.remove(serverPlayer.getUUID());
            
            if (!messageSent.contains(serverPlayer.getUUID())) {
                messageSent.add(serverPlayer.getUUID());
                
                if (linkEnabled && SoulLinkConfig.SHOW_LINK_MESSAGES.get()) {
                    StringBuilder features = new StringBuilder();
                    if (SoulLinkConfig.LINK_DAMAGE.get()) features.append("Health");
                    if (SoulLinkConfig.LINK_HUNGER.get()) {
                        if (features.length() > 0) features.append(", ");
                        features.append("Hunger");
                    }
                    if (SoulLinkConfig.LINK_KNOCKBACK.get()) {
                        if (features.length() > 0) features.append(", ");
                        features.append("Knockback");
                    }
                    if (SoulLinkConfig.LINK_INVENTORY.get()) {
                        if (features.length() > 0) features.append(", ");
                        features.append("Inventory");
                    }
                    
                    serverPlayer.sendSystemMessage(Component.literal("§6[Soul-Link] §aLink is enabled! Syncing: " + features));
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player p = event.getEntity();
        lastSyncedHealth.remove(p.getUUID());
        lastSyncedFood.remove(p.getUUID());
        currentlySyncing.remove(p.getUUID());
        currentlyKnockingBack.remove(p.getUUID());
        messageSent.remove(p.getUUID());
    }
}
