package com.jellycreative.soullink.network;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.inventory.SharedInventoryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Payload to synchronize inventory to clients.
 */
public record SyncInventoryPayload(List<ItemStack> inventory) implements CustomPacketPayload {
    
    public static final CustomPacketPayload.Type<SyncInventoryPayload> TYPE = 
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SoulLink.MOD_ID, "sync_inventory"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInventoryPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, SyncInventoryPayload::inventory,
            SyncInventoryPayload::new
    );
    
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
                mc.player.getInventory().setItem(SharedInventoryManager.ARMOR_START + i, inventory.get(SharedInventoryManager.ARMOR_START + i).copy());
            }
            
            if (SharedInventoryManager.OFFHAND_SLOT < inventory.size()) {
                mc.player.getInventory().setItem(SharedInventoryManager.OFFHAND_SLOT, inventory.get(SharedInventoryManager.OFFHAND_SLOT).copy());
            }
        }
    }
}
