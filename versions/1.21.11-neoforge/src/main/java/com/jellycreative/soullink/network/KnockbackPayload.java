package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Payload to synchronize knockback effects to clients.
 */
public record KnockbackPayload(double strength, double ratioX, double ratioZ) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<KnockbackPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SoulLink.MOD_ID, "knockback"));

    public static final StreamCodec<ByteBuf, KnockbackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, KnockbackPayload::strength,
            ByteBufCodecs.DOUBLE, KnockbackPayload::ratioX,
            ByteBufCodecs.DOUBLE, KnockbackPayload::ratioZ,
            KnockbackPayload::new
    );

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
