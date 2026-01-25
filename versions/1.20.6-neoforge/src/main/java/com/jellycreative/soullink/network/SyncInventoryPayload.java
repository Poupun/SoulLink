package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.inventory.SharedInventoryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload to synchronize inventory to clients.
 * Updated for 1.20.6 with Data Components system.
 */
public record SyncInventoryPayload(List<ItemStack> inventory) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SyncInventoryPayload> TYPE = 
            new CustomPacketPayload.Type<>(new ResourceLocation(SoulLink.MOD_ID, "sync_inventory"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInventoryPayload> STREAM_CODEC = StreamCodec.of(
            SyncInventoryPayload::encode,
            SyncInventoryPayload::decode
    );
    
    private static void encode(RegistryFriendlyByteBuf buf, SyncInventoryPayload payload) {
        buf.writeInt(payload.inventory.size());
        for (ItemStack stack : payload.inventory) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }
    
    private static SyncInventoryPayload decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ItemStack> inventory = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            inventory.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return new SyncInventoryPayload(inventory);
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
