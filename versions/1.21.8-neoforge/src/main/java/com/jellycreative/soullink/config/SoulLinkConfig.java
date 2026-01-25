package com.jellycreative.soullink.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration for Soul-Link mod.
 */
public class SoulLinkConfig {

    public static final ModConfigSpec SPEC;
    public static final SoulLinkConfig CONFIG;

    public static ModConfigSpec.BooleanValue LINK_DAMAGE;
    public static ModConfigSpec.BooleanValue LINK_HEALING;
    public static ModConfigSpec.BooleanValue LINK_KNOCKBACK;
    public static ModConfigSpec.BooleanValue LINK_HUNGER;
    public static ModConfigSpec.BooleanValue LINK_INVENTORY;

    // Aliases for backwards compatibility
    public static ModConfigSpec.BooleanValue SYNC_HEALTH;
    public static ModConfigSpec.BooleanValue SYNC_HUNGER;
    public static ModConfigSpec.BooleanValue SYNC_KNOCKBACK;
    public static ModConfigSpec.BooleanValue SYNC_INVENTORY;

    public static ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue HEALING_MULTIPLIER;
    public static ModConfigSpec.DoubleValue KNOCKBACK_MULTIPLIER;
    public static ModConfigSpec.DoubleValue HUNGER_MULTIPLIER;

    public static ModConfigSpec.BooleanValue SHOW_LINK_MESSAGES;
    public static ModConfigSpec.BooleanValue KEEP_INVENTORY_ON_DEATH;

    static {
        Pair<SoulLinkConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(SoulLinkConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();

        // Set up aliases after config is built
        SYNC_HEALTH = LINK_DAMAGE;
        SYNC_HUNGER = LINK_HUNGER;
        SYNC_KNOCKBACK = LINK_KNOCKBACK;
        SYNC_INVENTORY = LINK_INVENTORY;
    }

    public SoulLinkConfig(ModConfigSpec.Builder builder) {
        builder.comment("Soul-Link Configuration").push("features");

        LINK_DAMAGE = builder.comment("Whether damage should be shared between all players").define("linkDamage", true);
        LINK_HEALING = builder.comment("Whether healing should be shared between all players").define("linkHealing", true);
        LINK_KNOCKBACK = builder.comment("Whether knockback should be shared between all players").define("linkKnockback", true);
        LINK_HUNGER = builder.comment("Whether hunger should be shared between all players").define("linkHunger", true);
        LINK_INVENTORY = builder.comment("Whether inventory should be shared between all players").define("linkInventory", true);

        builder.pop().push("multipliers");

        DAMAGE_MULTIPLIER = builder.comment("Multiplier for shared damage (1.0 = 100%)").defineInRange("damageMultiplier", 1.0, 0.0, 10.0);
        HEALING_MULTIPLIER = builder.comment("Multiplier for shared healing (1.0 = 100%)").defineInRange("healingMultiplier", 1.0, 0.0, 10.0);
        KNOCKBACK_MULTIPLIER = builder.comment("Multiplier for shared knockback (1.0 = 100%)").defineInRange("knockbackMultiplier", 1.0, 0.0, 10.0);
        HUNGER_MULTIPLIER = builder.comment("Multiplier for shared hunger drain (1.0 = 100%)").defineInRange("hungerMultiplier", 1.0, 0.0, 10.0);

        builder.pop().push("misc");

        SHOW_LINK_MESSAGES = builder.comment("Whether to show messages when link effects trigger").define("showLinkMessages", false);
        KEEP_INVENTORY_ON_DEATH = builder.comment("Whether to keep the shared inventory when a player dies").define("keepInventoryOnDeath", true);

        builder.pop();
    }
}
