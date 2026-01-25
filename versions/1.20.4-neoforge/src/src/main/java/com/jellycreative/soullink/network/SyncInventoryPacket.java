package com.jellycreative.soullink.network;

import com.jellycreative.soullink.inventory.SharedInventoryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet to synchronize inventory to clients.
 */
public class SyncInventoryPacket {
    private final List<ItemStack> inventory;
    
    public SyncInventoryPacket(List<ItemStack> inventory) {
        this.inventory = new ArrayList<>(inventory);
    }
    
    public static void encode(SyncInventoryPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.inventory.size());
        for (ItemStack stack : packet.inventory) {
            buf.writeNbt(stack.save(new CompoundTag()));
        }
    }
    
    public static SyncInventoryPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ItemStack> inv = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            CompoundTag tag = buf.readNbt();
            inv.add(tag != null ? ItemStack.of(tag) : ItemStack.EMPTY);
        }
        return new SyncInventoryPacket(inv);
    }
    
    public static void handle(SyncInventoryPacket packet, NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                for (int i = 0; i < Math.min(packet.inventory.size(), 36); i++) {
                    mc.player.getInventory().setItem(i, packet.inventory.get(i).copy());
                }
                
                for (int i = 0; i < 4 && (SharedInventoryManager.ARMOR_START + i) < packet.inventory.size(); i++) {
                    mc.player.getInventory().armor.set(i, packet.inventory.get(SharedInventoryManager.ARMOR_START + i).copy());
                }
                
                if (SharedInventoryManager.OFFHAND_SLOT < packet.inventory.size()) {
                    mc.player.getInventory().offhand.set(0, packet.inventory.get(SharedInventoryManager.OFFHAND_SLOT).copy());
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
