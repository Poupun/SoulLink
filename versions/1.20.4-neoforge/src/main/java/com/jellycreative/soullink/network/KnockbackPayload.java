package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload to synchronize knockback effects to clients.
 */
public record KnockbackPayload(double strength, double ratioX, double ratioZ) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation(SoulLink.MOD_ID, "knockback");
    
    public KnockbackPayload(FriendlyByteBuf buf) {
        this(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(strength);
        buf.writeDouble(ratioX);
        buf.writeDouble(ratioZ);
    }
    
    @Override
    public ResourceLocation id() {
        return ID;
    }
    
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            double horizontalStrength = strength * 0.5;
            double verticalBoost = Math.min(0.4, strength * 0.15);
            
            mc.player.setDeltaMovement(
                    mc.player.getDeltaMovement().x - ratioX * horizontalStrength,
                    mc.player.getDeltaMovement().y + verticalBoost,
                    mc.player.getDeltaMovement().z - ratioZ * horizontalStrength
            );
        }
    }
}
