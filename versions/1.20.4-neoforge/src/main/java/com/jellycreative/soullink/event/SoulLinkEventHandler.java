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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.*;

/**
 * Event handler for Soul Link - synchronizes health, hunger, and knockback between linked players.
 */
@Mod.EventBusSubscriber(modid = SoulLink.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SoulLinkEventHandler {
    
    private static boolean linkEnabled = true;
    
    private static final Map<UUID, Float> lastSyncedHealth = new HashMap<>();
    private static final Map<UUID, Integer> lastSyncedFood = new HashMap<>();
    private static final Set<UUID> currentlySyncing = new HashSet<>();
    private static final Set<UUID> currentlyKnockingBack = new HashSet<>();
    private static final Set<UUID> messageSent = new HashSet<>();
    
    public static boolean isLinkEnabled() {
        return linkEnabled;
    }
    
    public static void setLinkEnabled(boolean enabled) {
        linkEnabled = enabled;
    }
    
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!linkEnabled || !SoulLinkConfig.SYNC_HEALTH.get()) return;
        
        if (event.getEntity() instanceof ServerPlayer damagedPlayer) {
            if (currentlySyncing.contains(damagedPlayer.getUUID())) {
                return;
            }
            
            float newHealth = damagedPlayer.getHealth() - event.getAmount();
            if (newHealth < 0) newHealth = 0;
            
            lastSyncedHealth.put(damagedPlayer.getUUID(), newHealth);
            
            List<ServerPlayer> allPlayers = damagedPlayer.server.getPlayerList().getPlayers();
            for (ServerPlayer otherPlayer : allPlayers) {
                if (!otherPlayer.equals(damagedPlayer)) {
                    try {
                        currentlySyncing.add(otherPlayer.getUUID());
                        lastSyncedHealth.put(otherPlayer.getUUID(), newHealth);
                        otherPlayer.setHealth(newHealth);
                        SoulLinkNetwork.sendToPlayer(new SyncHealthPayload(newHealth), otherPlayer);
                    } finally {
                        currentlySyncing.remove(otherPlayer.getUUID());
                    }
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!linkEnabled || !SoulLinkConfig.SYNC_KNOCKBACK.get()) return;
        
        if (event.getEntity() instanceof ServerPlayer knockedPlayer) {
            if (knockedPlayer.isBlocking()) {
                return;
            }
            
            if (currentlyKnockingBack.contains(knockedPlayer.getUUID())) {
                return;
            }
            
            double strength = event.getStrength();
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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!linkEnabled || !SoulLinkConfig.SYNC_HUNGER.get()) return;
        if (event.phase != TickEvent.Phase.END) return;
        
        if (event.player instanceof ServerPlayer serverPlayer) {
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
