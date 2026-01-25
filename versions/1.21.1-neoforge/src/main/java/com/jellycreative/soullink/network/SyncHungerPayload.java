package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload to synchronize hunger to clients.
 */
public record SyncHungerPayload(int foodLevel, float saturation) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SyncHungerPayload> TYPE = 
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SoulLink.MOD_ID, "sync_hunger"));
    
    public static final StreamCodec<ByteBuf, SyncHungerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncHungerPayload::foodLevel,
            ByteBufCodecs.FLOAT, SyncHungerPayload::saturation,
            SyncHungerPayload::new
    );
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getFoodData().setFoodLevel(foodLevel);
            mc.player.getFoodData().setSaturation(saturation);
        }
    }
}
