package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.event.SoulLinkEventHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Event handler for shared inventory synchronization.
 */
@EventBusSubscriber(modid = SoulLink.MOD_ID)
public class SharedInventoryEventHandler {

    /**
     * Initialize shared inventory when world loads.
     */
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            if (serverLevel.dimension() == ServerLevel.OVERWORLD) {
                SharedInventoryManager.init(serverLevel);
            }
        }
    }

    /**
     * Load shared inventory for player when they join.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Small delay to ensure world data is loaded
            MinecraftServer server = ((ServerLevel) serverPlayer.level()).getServer();
            server.execute(() -> {
                SharedInventoryManager.loadForPlayer(serverPlayer);
            });
        }
    }

    /**
     * Track container opens to prevent sync during container use.
     */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            AbstractContainerMenu container = event.getContainer();
            // Only track non-player inventory containers
            if (!(container instanceof InventoryMenu)) {
                SharedInventoryManager.markContainerOpen(serverPlayer.getUUID());
            }
        }
    }

    /**
     * Track container closes and sync after closing.
     */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SharedInventoryManager.markContainerClosed(serverPlayer.getUUID());

            // Schedule sync after a short delay to let items settle
            MinecraftServer server = ((ServerLevel) serverPlayer.level()).getServer();
            server.execute(() -> {
                // Additional delay of 5 ticks
                for (int i = 0; i < 5; i++) {
                    server.execute(() -> {});
                }
                if (SoulLinkConfig.SYNC_INVENTORY.get() && SoulLinkEventHandler.isLinkEnabled()) {
                    SharedInventoryManager.syncFromPlayer(serverPlayer);
                }
            });
        }
    }

    /**
     * Sync inventory periodically on player tick.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!SoulLinkConfig.SYNC_INVENTORY.get()) return;
        if (!SoulLinkEventHandler.isLinkEnabled()) return;

        Player player = event.getEntity();

        if (player instanceof ServerPlayer serverPlayer) {
            // Only sync every 10 ticks (0.5 seconds) and avoid sync during container use
            if (serverPlayer.tickCount % 10 != 0) return;
            if (SharedInventoryManager.hasContainerOpen(serverPlayer.getUUID())) return;

            SharedInventoryManager.syncFromPlayer(serverPlayer);
        }
    }

    /**
     * Clean up when player logs out.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof Player player) {
            SharedInventoryManager.clearPlayerTracking(player.getUUID());
        }
    }
}
