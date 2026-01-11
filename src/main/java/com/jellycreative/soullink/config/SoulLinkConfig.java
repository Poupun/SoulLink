package com.jellycreative.soullink.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuration for the Soul-Link mod.
 * Allows server operators to customize the linking behavior.
 */
public class SoulLinkConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // Feature toggles
    public static final ForgeConfigSpec.BooleanValue LINK_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue LINK_HEALING;
    public static final ForgeConfigSpec.BooleanValue LINK_KNOCKBACK;
    public static final ForgeConfigSpec.BooleanValue LINK_HUNGER;
    public static final ForgeConfigSpec.BooleanValue LINK_SATURATION;
    public static final ForgeConfigSpec.BooleanValue LINK_INVENTORY;
    public static final ForgeConfigSpec.BooleanValue KEEP_INVENTORY_ON_DEATH;

    // Damage settings
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue SHARE_DEATH;
    
    // Healing settings
    public static final ForgeConfigSpec.DoubleValue HEALING_MULTIPLIER;
    
    // Knockback settings
    public static final ForgeConfigSpec.DoubleValue KNOCKBACK_MULTIPLIER;
    
    // Hunger settings
    public static final ForgeConfigSpec.DoubleValue HUNGER_MULTIPLIER;
    
    // General settings
    public static final ForgeConfigSpec.IntValue MIN_PLAYERS_FOR_LINK;
    public static final ForgeConfigSpec.BooleanValue SHOW_LINK_MESSAGES;
    public static final ForgeConfigSpec.BooleanValue PREVENT_PLAYER_VS_PLAYER_LOOP;

    static {
        BUILDER.comment("Soul-Link Configuration").push("general");
        
        MIN_PLAYERS_FOR_LINK = BUILDER
                .comment("Minimum number of players required for soul-link to activate (default: 2)")
                .defineInRange("minPlayersForLink", 2, 1, 100);
        
        SHOW_LINK_MESSAGES = BUILDER
                .comment("Show messages in chat when link events occur (default: false)")
                .define("showLinkMessages", false);
        
        PREVENT_PLAYER_VS_PLAYER_LOOP = BUILDER
                .comment("Prevent infinite loops when players attack each other (default: true)")
                .define("preventPvPLoop", true);
        
        BUILDER.pop();
        
        BUILDER.comment("Damage Linking Settings").push("damage");
        
        LINK_DAMAGE = BUILDER
                .comment("Link damage between all players (default: true)")
                .define("linkDamage", true);
        
        DAMAGE_MULTIPLIER = BUILDER
                .comment("Multiplier for linked damage (1.0 = 100% of original damage)")
                .defineInRange("damageMultiplier", 1.0, 0.0, 10.0);
        
        SHARE_DEATH = BUILDER
                .comment("If one player dies, should all players die? (default: false)")
                .define("shareDeath", false);
        
        BUILDER.pop();
        
        BUILDER.comment("Healing Linking Settings").push("healing");
        
        LINK_HEALING = BUILDER
                .comment("Link healing between all players (default: true)")
                .define("linkHealing", true);
        
        HEALING_MULTIPLIER = BUILDER
                .comment("Multiplier for linked healing (1.0 = 100% of original healing)")
                .defineInRange("healingMultiplier", 1.0, 0.0, 10.0);
        
        BUILDER.pop();
        
        BUILDER.comment("Knockback Linking Settings").push("knockback");
        
        LINK_KNOCKBACK = BUILDER
                .comment("Link knockback between all players (default: true)")
                .define("linkKnockback", true);
        
        KNOCKBACK_MULTIPLIER = BUILDER
                .comment("Multiplier for linked knockback (1.0 = 100% of original knockback)")
                .defineInRange("knockbackMultiplier", 1.0, 0.0, 5.0);
        
        BUILDER.pop();
        
        BUILDER.comment("Hunger Linking Settings").push("hunger");
        
        LINK_HUNGER = BUILDER
                .comment("Link hunger (food level) between all players (default: true)")
                .define("linkHunger", true);
        
        LINK_SATURATION = BUILDER
                .comment("Link saturation between all players (default: true)")
                .define("linkSaturation", true);
        
        HUNGER_MULTIPLIER = BUILDER
                .comment("Multiplier for linked hunger changes (1.0 = 100% of original change)")
                .defineInRange("hungerMultiplier", 1.0, 0.0, 10.0);
        
        BUILDER.pop();
        
        BUILDER.comment("Inventory Linking Settings").push("inventory");
        
        LINK_INVENTORY = BUILDER
                .comment("Link inventory between all players - all players share the same inventory (default: true)")
                .define("linkInventory", true);
        
        KEEP_INVENTORY_ON_DEATH = BUILDER
                .comment("Keep the shared inventory when a player dies (default: true)")
                .define("keepInventoryOnDeath", true);
        
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}
