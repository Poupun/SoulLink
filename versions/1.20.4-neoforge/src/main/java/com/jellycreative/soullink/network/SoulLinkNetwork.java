package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

/**
 * Handles network packet registration and sending for Soul-Link.
 * NeoForge 1.20.4 version with payload system.
 */
public class SoulLinkNetwork {
    
    public static final String PROTOCOL_VERSION = "1";
    
    public static void register(RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar registrar = event.registrar(SoulLink.MOD_ID)
                .versioned(PROTOCOL_VERSION);
        
        registrar.play(
                KnockbackPayload.ID,
                KnockbackPayload::new,
                handler -> handler.client(SoulLinkNetwork::handleKnockback)
        );
        
        registrar.play(
                SyncHealthPayload.ID,
                SyncHealthPayload::new,
                handler -> handler.client(SoulLinkNetwork::handleSyncHealth)
        );
        
        registrar.play(
                SyncHungerPayload.ID,
                SyncHungerPayload::new,
                handler -> handler.client(SoulLinkNetwork::handleSyncHunger)
        );
        
        registrar.play(
                SyncInventoryPayload.ID,
                SyncInventoryPayload::new,
                handler -> handler.client(SoulLinkNetwork::handleSyncInventory)
        );
        
        SoulLink.LOGGER.info("Soul-Link network payloads registered");
    }
    
    private static void handleKnockback(KnockbackPayload payload, PlayPayloadContext context) {
        context.workHandler().execute(() -> {
            payload.handleClient();
        });
    }
    
    private static void handleSyncHealth(SyncHealthPayload payload, PlayPayloadContext context) {
        context.workHandler().execute(() -> {
            payload.handleClient();
        });
    }
    
    private static void handleSyncHunger(SyncHungerPayload payload, PlayPayloadContext context) {
        context.workHandler().execute(() -> {
            payload.handleClient();
        });
    }
    
    private static void handleSyncInventory(SyncInventoryPayload payload, PlayPayloadContext context) {
        context.workHandler().execute(() -> {
            payload.handleClient();
        });
    }
    
    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        PacketDistributor.PLAYER.with(player).send(payload);
    }
    
    public static void sendToAllPlayers(CustomPacketPayload payload) {
        PacketDistributor.ALL.noArg().send(payload);
    }
}
