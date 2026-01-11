package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Handles network communication for Soul-Link.
 * Used to sync knockback and other effects to clients.
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

    public static void register() {
        CHANNEL.messageBuilder(KnockbackPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(KnockbackPacket::decode)
                .encoder(KnockbackPacket::encode)
                .consumerMainThread(KnockbackPacket::handle)
                .add();
        
        CHANNEL.messageBuilder(SyncHealthPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncHealthPacket::decode)
                .encoder(SyncHealthPacket::encode)
                .consumerMainThread(SyncHealthPacket::handle)
                .add();
        
        CHANNEL.messageBuilder(SyncHungerPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncHungerPacket::decode)
                .encoder(SyncHungerPacket::encode)
                .consumerMainThread(SyncHungerPacket::handle)
                .add();
        
        CHANNEL.messageBuilder(SyncInventoryPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncInventoryPacket::decode)
                .encoder(SyncInventoryPacket::encode)
                .consumerMainThread(SyncInventoryPacket::handle)
                .add();
        
        SoulLink.LOGGER.info("Soul-Link network packets registered.");
    }

    /**
     * Send a packet to a specific player
     */
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /**
     * Send a packet to all players
     */
    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
