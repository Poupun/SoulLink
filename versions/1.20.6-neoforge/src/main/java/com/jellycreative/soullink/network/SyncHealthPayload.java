package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload to synchronize health to clients.
 */
public record SyncHealthPayload(float health) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SyncHealthPayload> TYPE = 
            new CustomPacketPayload.Type<>(new ResourceLocation(SoulLink.MOD_ID, "sync_health"));
    
    public static final StreamCodec<FriendlyByteBuf, SyncHealthPayload> STREAM_CODEC = StreamCodec.of(
            SyncHealthPayload::encode,
            SyncHealthPayload::decode
    );
    
    private static void encode(FriendlyByteBuf buf, SyncHealthPayload payload) {
        buf.writeFloat(payload.health);
    }
    
    private static SyncHealthPayload decode(FriendlyByteBuf buf) {
        return new SyncHealthPayload(buf.readFloat());
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setHealth(health);
        }
    }
}
