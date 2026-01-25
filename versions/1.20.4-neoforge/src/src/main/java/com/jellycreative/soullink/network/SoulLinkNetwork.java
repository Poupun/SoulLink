package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.simple.SimpleChannel;

/**
 * Handles network packet registration and sending for Soul-Link.
 */
public class SoulLinkNetwork {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SoulLink.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    private static int nextId() {
        return packetId++;
    }
    
    public static void register() {
        CHANNEL.messageBuilder(KnockbackPacket.class, nextId())
                .encoder(KnockbackPacket::encode)
                .decoder(KnockbackPacket::decode)
                .consumerMainThread(KnockbackPacket::handle)
                .add();
        
        CHANNEL.messageBuilder(SyncHealthPacket.class, nextId())
                .encoder(SyncHealthPacket::encode)
                .decoder(SyncHealthPacket::decode)
                .consumerMainThread(SyncHealthPacket::handle)
                .add();
        
        CHANNEL.messageBuilder(SyncHungerPacket.class, nextId())
                .encoder(SyncHungerPacket::encode)
                .decoder(SyncHungerPacket::decode)
                .consumerMainThread(SyncHungerPacket::handle)
                .add();
        
        CHANNEL.messageBuilder(SyncInventoryPacket.class, nextId())
                .encoder(SyncInventoryPacket::encode)
                .decoder(SyncInventoryPacket::decode)
                .consumerMainThread(SyncInventoryPacket::handle)
                .add();
        
        SoulLink.LOGGER.info("Soul-Link network packets registered");
    }
    
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    public static void sendToAllPlayers(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
