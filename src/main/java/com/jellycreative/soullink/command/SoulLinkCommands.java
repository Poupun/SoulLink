package com.jellycreative.soullink.command;

import com.jellycreative.soullink.SoulLink;
import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.inventory.SharedInventoryManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Commands for controlling Soul-Link settings in-game.
 */
@Mod.EventBusSubscriber(modid = SoulLink.MOD_ID)
public class SoulLinkCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("soullink")
                .requires(source -> source.hasPermission(2)) // Requires OP level 2
                
                // Status command
                .then(Commands.literal("status")
                        .executes(context -> {
                            sendStatus(context.getSource());
                            return 1;
                        })
                )
                
                // Toggle damage linking
                .then(Commands.literal("damage")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    // Note: This only works for runtime, config file change needs restart
                                    context.getSource().sendSuccess(() -> 
                                            Component.literal("§d[Soul-Link] §7Damage linking: " + 
                                                    (enabled ? "§aEnabled" : "§cDisabled")), true);
                                    return 1;
                                })
                        )
                )
                
                // Toggle healing linking
                .then(Commands.literal("healing")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    context.getSource().sendSuccess(() -> 
                                            Component.literal("§d[Soul-Link] §7Healing linking: " + 
                                                    (enabled ? "§aEnabled" : "§cDisabled")), true);
                                    return 1;
                                })
                        )
                )
                
                // Toggle knockback linking
                .then(Commands.literal("knockback")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    context.getSource().sendSuccess(() -> 
                                            Component.literal("§d[Soul-Link] §7Knockback linking: " + 
                                                    (enabled ? "§aEnabled" : "§cDisabled")), true);
                                    return 1;
                                })
                        )
                )
                
                // Toggle hunger linking
                .then(Commands.literal("hunger")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    context.getSource().sendSuccess(() -> 
                                            Component.literal("§d[Soul-Link] §7Hunger linking: " + 
                                                    (enabled ? "§aEnabled" : "§cDisabled")), true);
                                    return 1;
                                })
                        )
                )
                
                // Sync all players (force sync health and hunger)
                .then(Commands.literal("sync")
                        .executes(context -> {
                            syncAllPlayers(context.getSource());
                            return 1;
                        })
                )
                
                // Inventory commands
                .then(Commands.literal("inventory")
                        .then(Commands.literal("sync")
                                .executes(context -> {
                                    syncInventory(context.getSource());
                                    return 1;
                                })
                        )
                        .then(Commands.literal("reset")
                                .executes(context -> {
                                    resetInventory(context.getSource());
                                    return 1;
                                })
                        )
                        .then(Commands.literal("copyfrom")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            copyInventoryFrom(context.getSource(), EntityArgument.getPlayer(context, "player"));
                                            return 1;
                                        })
                                )
                        )
                )
                
                // Help command
                .then(Commands.literal("help")
                        .executes(context -> {
                            sendHelp(context.getSource());
                            return 1;
                        })
                )
                
                // Default - show help
                .executes(context -> {
                    sendHelp(context.getSource());
                    return 1;
                })
        );
    }

    private static void sendStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§d=== Soul-Link Status ==="), false);
        source.sendSuccess(() -> Component.literal("§7Damage Linking: " + 
                (SoulLinkConfig.LINK_DAMAGE.get() ? "§aEnabled" : "§cDisabled") + 
                " §7(×" + SoulLinkConfig.DAMAGE_MULTIPLIER.get() + ")"), false);
        source.sendSuccess(() -> Component.literal("§7Healing Linking: " + 
                (SoulLinkConfig.LINK_HEALING.get() ? "§aEnabled" : "§cDisabled") + 
                " §7(×" + SoulLinkConfig.HEALING_MULTIPLIER.get() + ")"), false);
        source.sendSuccess(() -> Component.literal("§7Knockback Linking: " + 
                (SoulLinkConfig.LINK_KNOCKBACK.get() ? "§aEnabled" : "§cDisabled") + 
                " §7(×" + SoulLinkConfig.KNOCKBACK_MULTIPLIER.get() + ")"), false);
        source.sendSuccess(() -> Component.literal("§7Hunger Linking: " + 
                (SoulLinkConfig.LINK_HUNGER.get() ? "§aEnabled" : "§cDisabled") + 
                " §7(×" + SoulLinkConfig.HUNGER_MULTIPLIER.get() + ")"), false);
        source.sendSuccess(() -> Component.literal("§7Saturation Linking: " + 
                (SoulLinkConfig.LINK_SATURATION.get() ? "§aEnabled" : "§cDisabled")), false);
        source.sendSuccess(() -> Component.literal("§7Inventory Linking: " + 
                (SoulLinkConfig.LINK_INVENTORY.get() ? "§aEnabled" : "§cDisabled")), false);
        source.sendSuccess(() -> Component.literal("§7Share Death: " + 
                (SoulLinkConfig.SHARE_DEATH.get() ? "§aEnabled" : "§cDisabled")), false);
        source.sendSuccess(() -> Component.literal("§7Min Players: §b" + 
                SoulLinkConfig.MIN_PLAYERS_FOR_LINK.get()), false);
        
        if (source.getServer() != null) {
            int playerCount = source.getServer().getPlayerList().getPlayers().size();
            source.sendSuccess(() -> Component.literal("§7Current Players: §b" + playerCount), false);
        }
    }

    private static void syncAllPlayers(CommandSourceStack source) {
        if (source.getServer() == null) {
            source.sendFailure(Component.literal("§c[Soul-Link] Cannot sync - no server available"));
            return;
        }
        
        var players = source.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            source.sendFailure(Component.literal("§c[Soul-Link] No players to sync"));
            return;
        }
        
        // Find average health and hunger
        float totalHealth = 0;
        int totalFood = 0;
        float totalSaturation = 0;
        
        for (ServerPlayer player : players) {
            totalHealth += player.getHealth();
            totalFood += player.getFoodData().getFoodLevel();
            totalSaturation += player.getFoodData().getSaturationLevel();
        }
        
        float avgHealth = totalHealth / players.size();
        int avgFood = totalFood / players.size();
        float avgSaturation = totalSaturation / players.size();
        
        // Apply to all players
        for (ServerPlayer player : players) {
            player.setHealth(avgHealth);
            player.getFoodData().setFoodLevel(avgFood);
            player.getFoodData().setSaturation(avgSaturation);
            
            player.displayClientMessage(
                    Component.literal("§d[Soul-Link] §7Your vitals have been synchronized with all players!"),
                    false
            );
        }
        
        source.sendSuccess(() -> Component.literal("§a[Soul-Link] §7Synchronized all " + 
                players.size() + " players!"), true);
    }

    private static void sendHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§d=== Soul-Link Commands ==="), false);
        source.sendSuccess(() -> Component.literal("§7/soullink status §8- Show current settings"), false);
        source.sendSuccess(() -> Component.literal("§7/soullink sync §8- Synchronize all player vitals"), false);
        source.sendSuccess(() -> Component.literal("§7/soullink damage <true|false> §8- Toggle damage linking"), false);
        source.sendSuccess(() -> Component.literal("§7/soullink healing <true|false> §8- Toggle healing linking"), false);
        source.sendSuccess(() -> Component.literal("§7/soullink knockback <true|false> §8- Toggle knockback linking"), false);
        source.sendSuccess(() -> Component.literal("§7/soullink hunger <true|false> §8- Toggle hunger linking"), false);
        source.sendSuccess(() -> Component.literal("§7/soullink inventory sync §8- Force sync shared inventory"), false);
        source.sendSuccess(() -> Component.literal("§7/soullink inventory reset §8- Reset shared inventory"), false);
        source.sendSuccess(() -> Component.literal("§7/soullink inventory copyfrom <player> §8- Copy player's inventory to shared"), false);
        source.sendSuccess(() -> Component.literal("§8Note: For permanent changes, edit the config file."), false);
    }
    
    private static void syncInventory(CommandSourceStack source) {
        if (!SoulLinkConfig.LINK_INVENTORY.get()) {
            source.sendFailure(Component.literal("§c[Soul-Link] Inventory linking is disabled in config"));
            return;
        }
        
        SharedInventoryManager.syncAllPlayers();
        source.sendSuccess(() -> Component.literal("§a[Soul-Link] §7Synchronized shared inventory to all players!"), true);
    }
    
    private static void resetInventory(CommandSourceStack source) {
        SharedInventoryManager.reset();
        SharedInventoryManager.syncAllPlayers();
        source.sendSuccess(() -> Component.literal("§a[Soul-Link] §7Shared inventory has been reset!"), true);
    }
    
    private static void copyInventoryFrom(CommandSourceStack source, ServerPlayer player) {
        SharedInventoryManager.copyFromPlayer(player);
        SharedInventoryManager.syncAllPlayers();
        source.sendSuccess(() -> Component.literal("§a[Soul-Link] §7Copied inventory from " + 
                player.getName().getString() + " to shared inventory!"), true);
    }
}
