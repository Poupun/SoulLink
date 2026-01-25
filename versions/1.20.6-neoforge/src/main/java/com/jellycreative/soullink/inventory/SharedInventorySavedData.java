package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * SavedData class for persisting shared inventory to world data.
 */
public class SharedInventorySavedData extends SavedData {
    
    private static final String DATA_NAME = SoulLink.MOD_ID + "_shared_inventory";
    
    public SharedInventorySavedData() {
    }
    
    public static SharedInventorySavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        SharedInventorySavedData data = new SharedInventorySavedData();
        SharedInventoryManager.loadFromNBT(tag, provider);
        return data;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag inventoryTag = SharedInventoryManager.saveToNBT(provider);
        tag.merge(inventoryTag);
        return tag;
    }
    
    public static SharedInventorySavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(
                        SharedInventorySavedData::new,
                        SharedInventorySavedData::load,
                        null
                ),
                DATA_NAME
        );
    }
}
