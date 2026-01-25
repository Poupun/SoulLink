package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload to synchronize health to clients.
 */
public record SyncHealthPayload(float health) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SyncHealthPayload> TYPE = 
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SoulLink.MOD_ID, "sync_health"));
    
    public static final StreamCodec<ByteBuf, SyncHealthPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SyncHealthPayload::health,
            SyncHealthPayload::new
    );
    
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
