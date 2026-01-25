package com.jellycreative.soullink.command;

import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.event.SoulLinkEventHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Commands for controlling Soul Link functionality.
 */
public class SoulLinkCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("soullink")
                .requires(source -> source.hasPermission(2)) // Requires operator permission
                .then(Commands.literal("toggle")
                        .executes(context -> {
                            boolean newState = !SoulLinkEventHandler.isLinkEnabled();
                            SoulLinkEventHandler.setLinkEnabled(newState);
                            context.getSource().sendSuccess(() -> 
                                    Component.literal("§6[Soul-Link] §fLink " + (newState ? "§aenabled" : "§cdisabled")), 
                                    true);
                            return 1;
                        })
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    SoulLinkEventHandler.setLinkEnabled(enabled);
                                    context.getSource().sendSuccess(() -> 
                                            Component.literal("§6[Soul-Link] §fLink " + (enabled ? "§aenabled" : "§cdisabled")), 
                                            true);
                                    return 1;
                                })))
                .then(Commands.literal("status")
                        .executes(context -> {
                            boolean enabled = SoulLinkEventHandler.isLinkEnabled();
                            StringBuilder features = new StringBuilder();
                            if (SoulLinkConfig.SYNC_HEALTH.get()) features.append("Health");
                            if (SoulLinkConfig.SYNC_HUNGER.get()) {
                                if (features.length() > 0) features.append(", ");
                                features.append("Hunger");
                            }
                            if (SoulLinkConfig.SYNC_KNOCKBACK.get()) {
                                if (features.length() > 0) features.append(", ");
                                features.append("Knockback");
                            }
                            if (SoulLinkConfig.SYNC_INVENTORY.get()) {
                                if (features.length() > 0) features.append(", ");
                                features.append("Inventory");
                            }
                            
                            context.getSource().sendSuccess(() -> 
                                    Component.literal("§6[Soul-Link] §fStatus: " + (enabled ? "§aEnabled" : "§cDisabled")), 
                                    false);
                            context.getSource().sendSuccess(() -> 
                                    Component.literal("§6[Soul-Link] §fSyncing: §e" + features), 
                                    false);
                            return 1;
                        }))
                .then(Commands.literal("help")
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> 
                                    Component.literal("§6=== Soul-Link Commands ==="), false);
                            context.getSource().sendSuccess(() -> 
                                    Component.literal("§e/soullink toggle [true|false] §7- Toggle or set link status"), false);
                            context.getSource().sendSuccess(() -> 
                                    Component.literal("§e/soullink status §7- Show current status"), false);
                            context.getSource().sendSuccess(() -> 
                                    Component.literal("§e/soullink help §7- Show this help message"), false);
                            return 1;
                        })));
    }
}
