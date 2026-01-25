package com.jellycreative.soullink.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;

/**
 * Packet to synchronize health to clients.
 */
public class SyncHealthPacket {
    private final float health;
    
    public SyncHealthPacket(float health) {
        this.health = health;
    }
    
    public static void encode(SyncHealthPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.health);
    }
    
    public static SyncHealthPacket decode(FriendlyByteBuf buf) {
        return new SyncHealthPacket(buf.readFloat());
    }
    
    public static void handle(SyncHealthPacket packet, NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.setHealth(packet.health);
            }
        });
        ctx.setPacketHandled(true);
    }
}
