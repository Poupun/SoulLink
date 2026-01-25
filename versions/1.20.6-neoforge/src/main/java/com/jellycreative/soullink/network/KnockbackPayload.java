package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload to synchronize knockback effects to clients.
 */
public record KnockbackPayload(double strength, double ratioX, double ratioZ) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<KnockbackPayload> TYPE = 
            new CustomPacketPayload.Type<>(new ResourceLocation(SoulLink.MOD_ID, "knockback"));
    
    public static final StreamCodec<FriendlyByteBuf, KnockbackPayload> STREAM_CODEC = StreamCodec.of(
            KnockbackPayload::encode,
            KnockbackPayload::decode
    );
    
    private static void encode(FriendlyByteBuf buf, KnockbackPayload payload) {
        buf.writeDouble(payload.strength);
        buf.writeDouble(payload.ratioX);
        buf.writeDouble(payload.ratioZ);
    }
    
    private static KnockbackPayload decode(FriendlyByteBuf buf) {
        return new KnockbackPayload(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
