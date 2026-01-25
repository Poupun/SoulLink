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
        ClientPlayNetworking.registerGlobalReceiver(SoulLinkNetwork.SyncHealthPayload.ID, (payload, context) -> {
            float health = payload.health();
            context.client().execute(() -> {
                if (context.client().player != null) {
                    context.client().player.setHealth(health);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SoulLinkNetwork.SyncHungerPayload.ID, (payload, context) -> {
            int food = payload.food();
            float saturation = payload.saturation();
            context.client().execute(() -> {
                if (context.client().player != null) {
                    context.client().player.getHungerManager().setFoodLevel(food);
                    context.client().player.getHungerManager().setSaturationLevel(saturation);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SoulLinkNetwork.KnockbackPayload.ID, (payload, context) -> {
            double strength = payload.strength();
            double ratioX = payload.ratioX();
            double ratioZ = payload.ratioZ();
            context.client().execute(() -> {
                if (context.client().player != null) {
                    context.client().player.takeKnockback(strength, ratioX, ratioZ);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SoulLinkNetwork.SyncInventoryPayload.ID, (payload, context) -> {
            // Inventory sync is handled server-side
        });
    }
}
