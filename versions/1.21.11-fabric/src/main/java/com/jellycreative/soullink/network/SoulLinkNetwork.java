package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Networking for Soul-Link mod using Fabric Networking API (1.21+).
 */
public class SoulLinkNetwork {

    // Payload IDs
    public static final Identifier SYNC_HEALTH_ID = Identifier.of(SoulLink.MOD_ID, "sync_health");
    public static final Identifier SYNC_HUNGER_ID = Identifier.of(SoulLink.MOD_ID, "sync_hunger");
    public static final Identifier KNOCKBACK_ID = Identifier.of(SoulLink.MOD_ID, "knockback");
    public static final Identifier SYNC_INVENTORY_ID = Identifier.of(SoulLink.MOD_ID, "sync_inventory");

    // Payload records
    public record SyncHealthPayload(float health) implements CustomPayload {
        public static final Id<SyncHealthPayload> ID = new Id<>(SYNC_HEALTH_ID);
        public static final PacketCodec<RegistryByteBuf, SyncHealthPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT, SyncHealthPayload::health,
            SyncHealthPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SyncHungerPayload(int food, float saturation) implements CustomPayload {
        public static final Id<SyncHungerPayload> ID = new Id<>(SYNC_HUNGER_ID);
        public static final PacketCodec<RegistryByteBuf, SyncHungerPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, SyncHungerPayload::food,
            PacketCodecs.FLOAT, SyncHungerPayload::saturation,
            SyncHungerPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record KnockbackPayload(double strength, double ratioX, double ratioZ) implements CustomPayload {
        public static final Id<KnockbackPayload> ID = new Id<>(KNOCKBACK_ID);
        public static final PacketCodec<RegistryByteBuf, KnockbackPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, KnockbackPayload::strength,
            PacketCodecs.DOUBLE, KnockbackPayload::ratioX,
            PacketCodecs.DOUBLE, KnockbackPayload::ratioZ,
            KnockbackPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SyncInventoryPayload() implements CustomPayload {
        public static final Id<SyncInventoryPayload> ID = new Id<>(SYNC_INVENTORY_ID);
        public static final PacketCodec<RegistryByteBuf, SyncInventoryPayload> CODEC = PacketCodec.unit(new SyncInventoryPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void register() {
        // Register S2C payloads
        PayloadTypeRegistry.playS2C().register(SyncHealthPayload.ID, SyncHealthPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncHungerPayload.ID, SyncHungerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(KnockbackPayload.ID, KnockbackPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncInventoryPayload.ID, SyncInventoryPayload.CODEC);
    }

    public static void sendSyncHealth(ServerPlayerEntity player, float health) {
        ServerPlayNetworking.send(player, new SyncHealthPayload(health));
    }

    public static void sendSyncHunger(ServerPlayerEntity player, int food, float saturation) {
        ServerPlayNetworking.send(player, new SyncHungerPayload(food, saturation));
    }

    public static void sendKnockback(ServerPlayerEntity player, double strength, double ratioX, double ratioZ) {
        ServerPlayNetworking.send(player, new KnockbackPayload(strength, ratioX, ratioZ));
    }
}
