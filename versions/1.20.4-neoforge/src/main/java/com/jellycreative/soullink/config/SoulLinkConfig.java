package com.jellycreative.soullink.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for Soul-Link mod.
 */
public class SoulLinkConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    public static final ModConfigSpec.BooleanValue SYNC_HEALTH;
    public static final ModConfigSpec.BooleanValue SYNC_HUNGER;
    public static final ModConfigSpec.BooleanValue SYNC_KNOCKBACK;
    public static final ModConfigSpec.BooleanValue SYNC_INVENTORY;
    
    static {
        BUILDER.push("Soul-Link Settings");
        
        SYNC_HEALTH = BUILDER
                .comment("Synchronize health between all linked players")
                .define("syncHealth", true);
        
        SYNC_HUNGER = BUILDER
                .comment("Synchronize hunger between all linked players")
                .define("syncHunger", true);
        
        SYNC_KNOCKBACK = BUILDER
                .comment("Synchronize knockback between all linked players")
                .define("syncKnockback", true);
        
        SYNC_INVENTORY = BUILDER
                .comment("Share inventory between all linked players")
                .define("syncInventory", true);
        
        BUILDER.pop();
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();
}
