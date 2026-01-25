package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent data storage for the shared inventory system.
 */
public class SharedInventorySavedData extends PersistentState {

    private static final String DATA_NAME = "soullink_shared_inventory";

    private List<ItemStack> mainInventory = new ArrayList<>();
    private List<ItemStack> armor = new ArrayList<>();
    private ItemStack offhand = ItemStack.EMPTY;

    public SharedInventorySavedData() {
        // Initialize empty inventory
        for (int i = 0; i < 36; i++) {
            mainInventory.add(ItemStack.EMPTY);
        }
        for (int i = 0; i < 4; i++) {
            armor.add(ItemStack.EMPTY);
        }
    }

    public static SharedInventorySavedData get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(
            SharedInventorySavedData::fromNbt,
            SharedInventorySavedData::new,
            DATA_NAME
        );
    }

    public static SharedInventorySavedData fromNbt(NbtCompound nbt) {
        SharedInventorySavedData data = new SharedInventorySavedData();

        // Load main inventory
        if (nbt.contains("MainInventory")) {
            NbtList mainList = nbt.getList("MainInventory", 10);
            data.mainInventory.clear();
            for (int i = 0; i < mainList.size(); i++) {
                NbtCompound itemNbt = mainList.getCompound(i);
                int slot = itemNbt.getInt("Slot");
                ItemStack stack = ItemStack.fromNbt(itemNbt);
                while (data.mainInventory.size() <= slot) {
                    data.mainInventory.add(ItemStack.EMPTY);
                }
                data.mainInventory.set(slot, stack);
            }
        }

        // Ensure 36 slots
        while (data.mainInventory.size() < 36) {
            data.mainInventory.add(ItemStack.EMPTY);
        }

        // Load armor
        if (nbt.contains("Armor")) {
            NbtList armorList = nbt.getList("Armor", 10);
            data.armor.clear();
            for (int i = 0; i < armorList.size(); i++) {
                NbtCompound itemNbt = armorList.getCompound(i);
                int slot = itemNbt.getInt("Slot");
                ItemStack stack = ItemStack.fromNbt(itemNbt);
                while (data.armor.size() <= slot) {
                    data.armor.add(ItemStack.EMPTY);
                }
                data.armor.set(slot, stack);
            }
        }

        // Ensure 4 armor slots
        while (data.armor.size() < 4) {
            data.armor.add(ItemStack.EMPTY);
        }

        // Load offhand
        if (nbt.contains("Offhand")) {
            data.offhand = ItemStack.fromNbt(nbt.getCompound("Offhand"));
        }

        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // Save main inventory
        NbtList mainList = new NbtList();
        for (int i = 0; i < mainInventory.size(); i++) {
            ItemStack stack = mainInventory.get(i);
            if (!stack.isEmpty()) {
                NbtCompound itemNbt = new NbtCompound();
                itemNbt.putInt("Slot", i);
                stack.writeNbt(itemNbt);
                mainList.add(itemNbt);
            }
        }
        nbt.put("MainInventory", mainList);

        // Save armor
        NbtList armorList = new NbtList();
        for (int i = 0; i < armor.size(); i++) {
            ItemStack stack = armor.get(i);
            if (!stack.isEmpty()) {
                NbtCompound itemNbt = new NbtCompound();
                itemNbt.putInt("Slot", i);
                stack.writeNbt(itemNbt);
                armorList.add(itemNbt);
            }
        }
        nbt.put("Armor", armorList);

        // Save offhand
        if (!offhand.isEmpty()) {
            NbtCompound offhandNbt = new NbtCompound();
            offhand.writeNbt(offhandNbt);
            nbt.put("Offhand", offhandNbt);
        }

        return nbt;
    }

    // Getters and setters

    public List<ItemStack> getMainInventory() {
        return mainInventory;
    }

    public void setMainInventory(List<ItemStack> mainInventory) {
        this.mainInventory = mainInventory;
        this.markDirty();
    }

    public List<ItemStack> getArmor() {
        return armor;
    }

    public void setArmor(List<ItemStack> armor) {
        this.armor = armor;
        this.markDirty();
    }

    public ItemStack getOffhand() {
        return offhand;
    }

    public void setOffhand(ItemStack offhand) {
        this.offhand = offhand;
        this.markDirty();
    }

    public void clear() {
        for (int i = 0; i < mainInventory.size(); i++) {
            mainInventory.set(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < armor.size(); i++) {
            armor.set(i, ItemStack.EMPTY);
        }
        offhand = ItemStack.EMPTY;
        this.markDirty();
    }
}
