package com.jellycreative.soullink.network;

import com.jellycreative.soullink.inventory.SharedInventoryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet to sync the full inventory from server to client.
 * Used for shared inventory synchronization.
 */
public class SyncInventoryPacket {
    
    private final List<ItemStack> inventory;

    public SyncInventoryPacket(List<ItemStack> inventory) {
        this.inventory = new ArrayList<>(inventory.size());
        for (ItemStack stack : inventory) {
            this.inventory.add(stack.copy());
        }
    }

    public static void encode(SyncInventoryPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.inventory.size());
        for (ItemStack stack : packet.inventory) {
            buf.writeItem(stack);
        }
    }

    public static SyncInventoryPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ItemStack> inventory = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            inventory.add(buf.readItem());
        }
        return new SyncInventoryPacket(inventory);
    }

    public static void handle(SyncInventoryPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && packet.inventory.size() >= SharedInventoryManager.INVENTORY_SIZE) {
                Inventory inv = mc.player.getInventory();
                
                // Apply main inventory (slots 0-35)
                for (int i = 0; i < 36 && i < packet.inventory.size(); i++) {
                    inv.setItem(i, packet.inventory.get(i).copy());
                }
                
                // Apply armor (slots 36-39)
                for (int i = 0; i < 4; i++) {
                    int packetSlot = SharedInventoryManager.ARMOR_START + i;
                    if (packetSlot < packet.inventory.size()) {
                        inv.armor.set(i, packet.inventory.get(packetSlot).copy());
                    }
                }
                
                // Apply offhand (slot 40)
                if (SharedInventoryManager.OFFHAND_SLOT < packet.inventory.size()) {
                    inv.offhand.set(0, packet.inventory.get(SharedInventoryManager.OFFHAND_SLOT).copy());
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
