package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.event.SoulLinkEventHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Event handler for shared inventory synchronization.
 */
@Mod.EventBusSubscriber(modid = SoulLink.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SharedInventoryEventHandler {
    
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension() == ServerLevel.OVERWORLD) {
                SharedInventorySavedData.get(serverLevel);
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.server.execute(() -> {
                SharedInventoryManager.loadForPlayer(serverPlayer);
            });
        }
    }
    
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbstractContainerMenu container = event.getContainer();
            if (!(container instanceof InventoryMenu)) {
                SharedInventoryManager.markContainerOpen(serverPlayer.getUUID());
            }
        }
    }
    
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SharedInventoryManager.markContainerClosed(serverPlayer.getUUID());
            
            serverPlayer.server.execute(() -> {
                if (SoulLinkConfig.SYNC_INVENTORY.get() && SoulLinkEventHandler.isLinkEnabled()) {
                    SharedInventoryManager.syncFromPlayer(serverPlayer);
                }
            });
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!SoulLinkConfig.SYNC_INVENTORY.get()) return;
        if (!SoulLinkEventHandler.isLinkEnabled()) return;
        if (event.phase != TickEvent.Phase.END) return;
        
        if (event.player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.tickCount % 10 != 0) return;
            if (SharedInventoryManager.hasContainerOpen(serverPlayer.getUUID())) return;
            
            SharedInventoryManager.syncFromPlayer(serverPlayer);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player p = event.getEntity();
        SharedInventoryManager.clearPlayerTracking(p.getUUID());
    }
}
