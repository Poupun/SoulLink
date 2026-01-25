package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Saved data for the shared inventory.
 */
public class SharedInventorySavedData extends SavedData {

    public static final Codec<SharedInventorySavedData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("inventory").forGetter(data -> data.inventory)
        ).apply(instance, SharedInventorySavedData::new)
    );
    
    public static final SavedDataType<SharedInventorySavedData> TYPE = new SavedDataType<>(
        SoulLink.MOD_ID + "_inventory",
        context -> new SharedInventorySavedData(),
        context -> CODEC
    );
    
    private List<ItemStack> inventory;
    
    public SharedInventorySavedData() {
        this.inventory = createEmptyInventory();
    }
    
    public SharedInventorySavedData(List<ItemStack> inventory) {
        this.inventory = new ArrayList<>(inventory);
        // Ensure we have the right size
        while (this.inventory.size() < SharedInventoryManager.TOTAL_SIZE) {
            this.inventory.add(ItemStack.EMPTY);
        }
    }
    
    public List<ItemStack> getInventory() {
        return inventory;
    }
    
    public void setInventory(List<ItemStack> inventory) {
        this.inventory = new ArrayList<>(inventory);
        while (this.inventory.size() < SharedInventoryManager.TOTAL_SIZE) {
            this.inventory.add(ItemStack.EMPTY);
        }
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
