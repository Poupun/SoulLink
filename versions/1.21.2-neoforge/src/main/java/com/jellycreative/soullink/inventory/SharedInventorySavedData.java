package com.jellycreative.soullink.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Saved data for the shared inventory.
 */
public class SharedInventorySavedData extends SavedData {
    
    private List<ItemStack> inventory;
    private final HolderLookup.Provider registries;
    
    public SharedInventorySavedData(HolderLookup.Provider registries) {
        this.registries = registries;
        this.inventory = createEmptyInventory();
    }
    
    public SharedInventorySavedData(CompoundTag tag, HolderLookup.Provider registries) {
        this.registries = registries;
        this.inventory = createEmptyInventory();
        load(tag);
    }
    
    public static SavedData.Factory<SharedInventorySavedData> factory(HolderLookup.Provider registries) {
        return new SavedData.Factory<>(
                () -> new SharedInventorySavedData(registries),
                (tag, provider) -> new SharedInventorySavedData(tag, registries)
        );
    }
    
    private void load(CompoundTag tag) {
        if (tag.contains("Inventory", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("Inventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < SharedInventoryManager.TOTAL_SIZE) {
                    inventory.set(slot, ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY));
                }
            }
        }
    }
    
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                listTag.add(stack.save(registries, itemTag));
            }
        }
        tag.put("Inventory", listTag);
        return tag;
    }
    
    public List<ItemStack> getInventory() {
        return inventory;
    }
    
    public void setInventory(List<ItemStack> inventory) {
        this.inventory = inventory;
        setDirty();
    }
    
    private static List<ItemStack> createEmptyInventory() {
        List<ItemStack> inv = new ArrayList<>();
        for (int i = 0; i < SharedInventoryManager.TOTAL_SIZE; i++) {
            inv.add(ItemStack.EMPTY);
        }
        return inv;
    }
}
