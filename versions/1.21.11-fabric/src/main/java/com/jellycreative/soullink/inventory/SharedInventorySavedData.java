package com.jellycreative.soullink.inventory;

import com.jellycreative.soullink.SoulLink;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Persistent data storage for the shared inventory system.
 */
public class SharedInventorySavedData extends PersistentState {

    private static final String DATA_NAME = "soullink_shared_inventory";

    private List<ItemStack> mainInventory = new ArrayList<>();
    private List<ItemStack> armor = new ArrayList<>();
    private ItemStack offhand = ItemStack.EMPTY;
    
    // Store the registry lookup for serialization
    private RegistryWrapper.WrapperLookup registryLookup;

    public SharedInventorySavedData() {
        // Initialize empty inventory
        for (int i = 0; i < 36; i++) {
            mainInventory.add(ItemStack.EMPTY);
        }
        for (int i = 0; i < 4; i++) {
            armor.add(ItemStack.EMPTY);
        }
    }
    
    public SharedInventorySavedData(RegistryWrapper.WrapperLookup registryLookup) {
        this();
        this.registryLookup = registryLookup;
    }

    // Create a Codec for SharedInventorySavedData
    public static Codec<SharedInventorySavedData> createCodec(RegistryWrapper.WrapperLookup registryLookup) {
        return NbtCompound.CODEC.xmap(
            nbt -> fromNbt(nbt, registryLookup),
            data -> {
                NbtCompound nbt = new NbtCompound();
                data.writeToNbt(nbt, registryLookup);
                return nbt;
            }
        );
    }

    public static SharedInventorySavedData get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        RegistryWrapper.WrapperLookup registryLookup = world.getRegistryManager();
        
        PersistentStateType<SharedInventorySavedData> type = new PersistentStateType<>(
            DATA_NAME,
            () -> new SharedInventorySavedData(registryLookup),
            createCodec(registryLookup),
            null
        );
        
        return manager.getOrCreate(type);
    }

    public static SharedInventorySavedData fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        SharedInventorySavedData data = new SharedInventorySavedData(registryLookup);
        RegistryOps<NbtElement> registryOps = registryLookup.getOps(NbtOps.INSTANCE);

        // Load main inventory
        if (nbt.contains("MainInventory")) {
            NbtList mainList = nbt.getListOrEmpty("MainInventory");
            data.mainInventory.clear();
            for (int i = 0; i < 36; i++) {
                data.mainInventory.add(ItemStack.EMPTY);
            }
            for (int i = 0; i < mainList.size(); i++) {
                Optional<NbtCompound> itemNbtOpt = mainList.getCompound(i);
                if (itemNbtOpt.isPresent()) {
                    NbtCompound itemNbt = itemNbtOpt.get();
                    int slot = itemNbt.getInt("Slot", 0);
                    ItemStack stack = decodeItemStack(itemNbt, registryOps);
                    if (slot >= 0 && slot < 36) {
                        data.mainInventory.set(slot, stack);
                    }
                }
            }
        }

        // Load armor
        if (nbt.contains("Armor")) {
            NbtList armorList = nbt.getListOrEmpty("Armor");
            data.armor.clear();
            for (int i = 0; i < 4; i++) {
                data.armor.add(ItemStack.EMPTY);
            }
            for (int i = 0; i < armorList.size(); i++) {
                Optional<NbtCompound> itemNbtOpt = armorList.getCompound(i);
                if (itemNbtOpt.isPresent()) {
                    NbtCompound itemNbt = itemNbtOpt.get();
                    int slot = itemNbt.getInt("Slot", 0);
                    ItemStack stack = decodeItemStack(itemNbt, registryOps);
                    if (slot >= 0 && slot < 4) {
                        data.armor.set(slot, stack);
                    }
                }
            }
        }

        // Load offhand
        if (nbt.contains("Offhand")) {
            Optional<NbtCompound> offhandNbtOpt = nbt.getCompound("Offhand");
            if (offhandNbtOpt.isPresent()) {
                data.offhand = decodeItemStack(offhandNbtOpt.get(), registryOps);
            }
        }

        return data;
    }

    private static ItemStack decodeItemStack(NbtCompound nbt, RegistryOps<NbtElement> registryOps) {
        return ItemStack.OPTIONAL_CODEC.decode(registryOps, nbt)
            .result()
            .map(pair -> pair.getFirst())
            .orElse(ItemStack.EMPTY);
    }

    private static NbtElement encodeItemStack(ItemStack stack, RegistryOps<NbtElement> registryOps) {
        return ItemStack.CODEC.encodeStart(registryOps, stack)
            .result()
            .orElse(new NbtCompound());
    }

    public void writeToNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        RegistryOps<NbtElement> registryOps = registryLookup.getOps(NbtOps.INSTANCE);
        
        // Save main inventory
        NbtList mainList = new NbtList();
        for (int i = 0; i < mainInventory.size(); i++) {
            ItemStack stack = mainInventory.get(i);
            if (!stack.isEmpty()) {
                NbtElement itemNbt = encodeItemStack(stack, registryOps);
                if (itemNbt instanceof NbtCompound compound) {
                    compound.putInt("Slot", i);
                    mainList.add(compound);
                }
            }
        }
        nbt.put("MainInventory", mainList);

        // Save armor
        NbtList armorList = new NbtList();
        for (int i = 0; i < armor.size(); i++) {
            ItemStack stack = armor.get(i);
            if (!stack.isEmpty()) {
                NbtElement itemNbt = encodeItemStack(stack, registryOps);
                if (itemNbt instanceof NbtCompound compound) {
                    compound.putInt("Slot", i);
                    armorList.add(compound);
                }
            }
        }
        nbt.put("Armor", armorList);

        // Save offhand
        if (!offhand.isEmpty()) {
            nbt.put("Offhand", encodeItemStack(offhand, registryOps));
        }
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
