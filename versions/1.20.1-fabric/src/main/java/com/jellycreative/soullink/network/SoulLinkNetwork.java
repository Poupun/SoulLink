package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Networking for Soul-Link mod using Fabric Networking API.
 */
public class SoulLinkNetwork {
    public static final Identifier SYNC_HEALTH = new Identifier(SoulLink.MOD_ID, "sync_health");
    public static final Identifier SYNC_HUNGER = new Identifier(SoulLink.MOD_ID, "sync_hunger");
    public static final Identifier KNOCKBACK = new Identifier(SoulLink.MOD_ID, "knockback");
    public static final Identifier SYNC_INVENTORY = new Identifier(SoulLink.MOD_ID, "sync_inventory");

    public static void register() {
        // Server-side receivers (if we need any client->server packets)
    }

    public static void sendSyncHealth(ServerPlayerEntity player, float health) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(health);
        ServerPlayNetworking.send(player, SYNC_HEALTH, buf);
    }

    public static void sendSyncHunger(ServerPlayerEntity player, int food, float saturation) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(food);
        buf.writeFloat(saturation);
        ServerPlayNetworking.send(player, SYNC_HUNGER, buf);
    }

    public static void sendKnockback(ServerPlayerEntity player, double strength, double ratioX, double ratioZ) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(strength);
        buf.writeDouble(ratioX);
        buf.writeDouble(ratioZ);
        ServerPlayNetworking.send(player, KNOCKBACK, buf);
    }
}
