package com.jellycreative.soullink.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet to sync knockback to the client.
 * This allows linked players to receive the same knockback as the damaged player.
 */
public class KnockbackPacket {
    private final double motionX;
    private final double motionY;
    private final double motionZ;

    public KnockbackPacket(double motionX, double motionY, double motionZ) {
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
    }

    public KnockbackPacket(Vec3 motion) {
        this(motion.x, motion.y, motion.z);
    }

    public static void encode(KnockbackPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.motionX);
        buf.writeDouble(packet.motionY);
        buf.writeDouble(packet.motionZ);
    }

    public static KnockbackPacket decode(FriendlyByteBuf buf) {
        return new KnockbackPacket(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }

    public static void handle(KnockbackPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side handling
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                // Apply knockback to the local player
                mc.player.setDeltaMovement(
                        mc.player.getDeltaMovement().add(packet.motionX, packet.motionY, packet.motionZ)
                );
                // Mark the player as having received a push
                mc.player.hurtMarked = true;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
