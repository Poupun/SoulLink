package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload to synchronize hunger to clients.
 */
public record SyncHungerPayload(int foodLevel, float saturation) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation(SoulLink.MOD_ID, "sync_hunger");
    
    public SyncHungerPayload(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readFloat());
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(foodLevel);
        buf.writeFloat(saturation);
    }
    
    @Override
    public ResourceLocation id() {
        return ID;
    }
    
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getFoodData().setFoodLevel(foodLevel);
            mc.player.getFoodData().setSaturation(saturation);
        }
    }
}
