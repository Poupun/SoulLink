package com.jellycreative.soullink.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet to sync hunger (food level) and saturation to the client.
 * Ensures the client displays the correct hunger values.
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

    public static void handle(SyncHungerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.getFoodData().setFoodLevel(packet.foodLevel);
                mc.player.getFoodData().setSaturation(packet.saturation);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
