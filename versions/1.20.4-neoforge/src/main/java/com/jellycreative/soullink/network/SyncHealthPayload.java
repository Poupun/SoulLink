package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload to synchronize health to clients.
 */
public record SyncHealthPayload(float health) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation(SoulLink.MOD_ID, "sync_health");
    
    public SyncHealthPayload(FriendlyByteBuf buf) {
        this(buf.readFloat());
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(health);
    }
    
    @Override
    public ResourceLocation id() {
        return ID;
    }
    
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setHealth(health);
        }
    }
}
