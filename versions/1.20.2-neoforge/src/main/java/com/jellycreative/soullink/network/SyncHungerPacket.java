package com.jellycreative.soullink.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;

/**
 * Packet to synchronize hunger to clients.
 */
public class SyncHungerPacket {
    private final int foodLevel;
    private final float saturation;
    
    public SyncHungerPacket(int foodLevel, float saturation) {
        this.foodLevel = foodLevel;
        this.saturation = saturation;
    }
    
    public static void encode(SyncHungerPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.foodLevel);
        buf.writeFloat(packet.saturation);
    }
    
    public static SyncHungerPacket decode(FriendlyByteBuf buf) {
        return new SyncHungerPacket(buf.readInt(), buf.readFloat());
    }
    
    public static void handle(SyncHungerPacket packet, NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.getFoodData().setFoodLevel(packet.foodLevel);
                mc.player.getFoodData().setSaturation(packet.saturation);
            }
        });
        ctx.setPacketHandled(true);
    }
}
