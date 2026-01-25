package com.jellycreative.soullink.client;

import com.jellycreative.soullink.network.SoulLinkNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-side initializer for Soul-Link mod.
 */
@Environment(EnvType.CLIENT)
public class SoulLinkClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register client-side networking handlers
        registerClientNetworking();
    }

    private void registerClientNetworking() {
        // Client-side receivers for server->client packets
        ClientPlayNetworking.registerGlobalReceiver(SoulLinkNetwork.SYNC_HEALTH, (client, handler, buf, responseSender) -> {
            float health = buf.readFloat();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.setHealth(health);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SoulLinkNetwork.SYNC_HUNGER, (client, handler, buf, responseSender) -> {
            int food = buf.readInt();
            float saturation = buf.readFloat();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.getHungerManager().setFoodLevel(food);
                    client.player.getHungerManager().setSaturationLevel(saturation);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SoulLinkNetwork.KNOCKBACK, (client, handler, buf, responseSender) -> {
            double strength = buf.readDouble();
            double ratioX = buf.readDouble();
            double ratioZ = buf.readDouble();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.takeKnockback(strength, ratioX, ratioZ);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SoulLinkNetwork.SYNC_INVENTORY, (client, handler, buf, responseSender) -> {
            // Inventory sync is handled server-side
        });
    }
}
