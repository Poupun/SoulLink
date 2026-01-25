package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.inventory.SharedInventoryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload to synchronize inventory to clients.
 */
public record SyncInventoryPayload(List<ItemStack> inventory) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation(SoulLink.MOD_ID, "sync_inventory");
    
    public SyncInventoryPayload(FriendlyByteBuf buf) {
        this(readInventory(buf));
    }
    
    private static List<ItemStack> readInventory(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ItemStack> inv = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            CompoundTag tag = buf.readNbt();
            inv.add(tag != null ? ItemStack.of(tag) : ItemStack.EMPTY);
        }
        return inv;
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(inventory.size());
        for (ItemStack stack : inventory) {
            buf.writeNbt(stack.save(new CompoundTag()));
        }
    }
    
    @Override
    public ResourceLocation id() {
        return ID;
    }
    
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            for (int i = 0; i < Math.min(inventory.size(), 36); i++) {
                mc.player.getInventory().setItem(i, inventory.get(i).copy());
            }
            
            for (int i = 0; i < 4 && (SharedInventoryManager.ARMOR_START + i) < inventory.size(); i++) {
                mc.player.getInventory().armor.set(i, inventory.get(SharedInventoryManager.ARMOR_START + i).copy());
            }
            
            if (SharedInventoryManager.OFFHAND_SLOT < inventory.size()) {
                mc.player.getInventory().offhand.set(0, inventory.get(SharedInventoryManager.OFFHAND_SLOT).copy());
            }
        }
    }
}
