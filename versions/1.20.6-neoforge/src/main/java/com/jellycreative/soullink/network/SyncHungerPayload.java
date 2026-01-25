package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload to synchronize hunger to clients.
 */
public record SyncHungerPayload(int foodLevel, float saturation) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SyncHungerPayload> TYPE = 
            new CustomPacketPayload.Type<>(new ResourceLocation(SoulLink.MOD_ID, "sync_hunger"));
    
    public static final StreamCodec<FriendlyByteBuf, SyncHungerPayload> STREAM_CODEC = StreamCodec.of(
            SyncHungerPayload::encode,
            SyncHungerPayload::decode
    );
    
    private static void encode(FriendlyByteBuf buf, SyncHungerPayload payload) {
        buf.writeInt(payload.foodLevel);
        buf.writeFloat(payload.saturation);
    }
    
    private static SyncHungerPayload decode(FriendlyByteBuf buf) {
        return new SyncHungerPayload(buf.readInt(), buf.readFloat());
    }
    
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
