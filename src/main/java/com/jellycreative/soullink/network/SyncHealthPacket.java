package com.jellycreative.soullink.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet to sync health changes to the client.
 * Ensures the client displays the correct health value.
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

    public static void handle(SyncHealthPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.setHealth(packet.health);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
