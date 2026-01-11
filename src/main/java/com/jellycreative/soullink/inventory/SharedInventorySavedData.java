package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;

/**
 * Handles persistent storage of the shared inventory across world saves.
 * The shared inventory is saved with the overworld data.
 */
public class SharedInventorySavedData extends SavedData {
    
    private static final String DATA_NAME = SoulLink.MOD_ID + "_shared_inventory";
    
    public SharedInventorySavedData() {
        super();
    }
    
    /**
     * Load from existing NBT data
     */
    public static SharedInventorySavedData load(CompoundTag tag) {
        SharedInventorySavedData data = new SharedInventorySavedData();
        SharedInventoryManager.loadFromNBT(tag);
        return data;
    }
    
    @Override
    @Nonnull
    public CompoundTag save(@Nonnull CompoundTag tag) {
        CompoundTag inventoryData = SharedInventoryManager.saveToNBT();
        tag.merge(inventoryData);
        return tag;
    }
    
    /**
     * Get or create the saved data for a server level
     */
    public static SharedInventorySavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                SharedInventorySavedData::load,
                SharedInventorySavedData::new,
                DATA_NAME
        );
    }
    
    /**
     * Mark the data as dirty so it will be saved
     */
    public void markDirty() {
        setDirty();
    }
}
