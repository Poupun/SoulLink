package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Handles network packet registration and sending for Soul-Link.
 * Updated for NeoForge 1.21.2 payload system.
 */
public class SoulLinkNetwork {
    
    public static final String PROTOCOL_VERSION = "1";
    
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(SoulLink.MOD_ID)
                .versioned(PROTOCOL_VERSION);
        
        registrar.playToClient(
                KnockbackPayload.TYPE,
                KnockbackPayload.STREAM_CODEC,
                SoulLinkNetwork::handleKnockback
        );
        
        registrar.playToClient(
                SyncHealthPayload.TYPE,
                SyncHealthPayload.STREAM_CODEC,
                SoulLinkNetwork::handleSyncHealth
        );
        
        registrar.playToClient(
                SyncHungerPayload.TYPE,
                SyncHungerPayload.STREAM_CODEC,
                SoulLinkNetwork::handleSyncHunger
        );
        
        registrar.playToClient(
                SyncInventoryPayload.TYPE,
                SyncInventoryPayload.STREAM_CODEC,
                SoulLinkNetwork::handleSyncInventory
        );
        
        SoulLink.LOGGER.info("Soul-Link network payloads registered");
    }
    
    private static void handleKnockback(KnockbackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> payload.handleClient());
    }
    
    private static void handleSyncHealth(SyncHealthPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> payload.handleClient());
    }
    
    private static void handleSyncHunger(SyncHungerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> payload.handleClient());
    }
    
    private static void handleSyncInventory(SyncInventoryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> payload.handleClient());
    }
    
    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload);
    }
    
    public static void sendToAllPlayers(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }
}
