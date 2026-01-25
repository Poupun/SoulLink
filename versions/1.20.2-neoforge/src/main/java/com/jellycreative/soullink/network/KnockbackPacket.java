package com.jellycreative.soullink.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;

/**
 * Packet to synchronize knockback effects to clients.
 */
public class KnockbackPacket {
    private final double strength;
    private final double ratioX;
    private final double ratioZ;
    
    public KnockbackPacket(double strength, double ratioX, double ratioZ) {
        this.strength = strength;
        this.ratioX = ratioX;
        this.ratioZ = ratioZ;
    }
    
    public static void encode(KnockbackPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.strength);
        buf.writeDouble(packet.ratioX);
        buf.writeDouble(packet.ratioZ);
    }
    
    public static KnockbackPacket decode(FriendlyByteBuf buf) {
        return new KnockbackPacket(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
    
    public static void handle(KnockbackPacket packet, NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                double horizontalStrength = packet.strength * 0.5;
                double verticalBoost = Math.min(0.4, packet.strength * 0.15);
                
                mc.player.setDeltaMovement(
                        mc.player.getDeltaMovement().x - packet.ratioX * horizontalStrength,
                        mc.player.getDeltaMovement().y + verticalBoost,
                        mc.player.getDeltaMovement().z - packet.ratioZ * horizontalStrength
                );
            }
        });
        ctx.setPacketHandled(true);
    }
}
