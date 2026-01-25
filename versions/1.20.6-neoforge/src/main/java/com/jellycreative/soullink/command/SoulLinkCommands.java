package com.jellycreative.soullink.command;

import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.inventory.SharedInventoryManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Commands for Soul-Link mod administration.
 */
@EventBusSubscriber(modid = "soullink")
public class SoulLinkCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("soullink")
                .then(Commands.literal("status")
                        .executes(context -> showStatus(context.getSource())))
                .then(Commands.literal("sync")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> syncAllPlayers(context.getSource())))
                .then(Commands.literal("inventory")
                        .then(Commands.literal("sync")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> syncInventory(context.getSource())))
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> resetInventory(context.getSource())))
                        .then(Commands.literal("copyfrom")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> copyFromPlayer(context.getSource(), 
                                                EntityArgument.getPlayer(context, "player"))))))
        );
    }

    private static int showStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Soul-Link Status ==="), false);
        source.sendSuccess(() -> Component.literal("§7Damage Link: " + (SoulLinkConfig.LINK_DAMAGE.get() ? "§aEnabled" : "§cDisabled")), false);
        source.sendSuccess(() -> Component.literal("§7Healing Link: " + (SoulLinkConfig.LINK_HEALING.get() ? "§aEnabled" : "§cDisabled")), false);
        source.sendSuccess(() -> Component.literal("§7Knockback Link: " + (SoulLinkConfig.LINK_KNOCKBACK.get() ? "§aEnabled" : "§cDisabled")), false);
        source.sendSuccess(() -> Component.literal("§7Hunger Link: " + (SoulLinkConfig.LINK_HUNGER.get() ? "§aEnabled" : "§cDisabled")), false);
        source.sendSuccess(() -> Component.literal("§7Inventory Link: " + (SoulLinkConfig.LINK_INVENTORY.get() ? "§aEnabled" : "§cDisabled")), false);
        return 1;
    }

    private static int syncAllPlayers(CommandSourceStack source) {
        SharedInventoryManager.syncAllPlayers();
        source.sendSuccess(() -> Component.literal("§aSynced all players to shared inventory"), true);
        return 1;
    }

    private static int syncInventory(CommandSourceStack source) {
        SharedInventoryManager.syncAllPlayers();
        source.sendSuccess(() -> Component.literal("§aInventory synced to all players"), true);
        return 1;
    }

    private static int resetInventory(CommandSourceStack source) {
        SharedInventoryManager.reset();
        source.sendSuccess(() -> Component.literal("§cShared inventory has been reset"), true);
        return 1;
    }

    private static int copyFromPlayer(CommandSourceStack source, ServerPlayer player) {
        SharedInventoryManager.copyFromPlayer(player);
        SharedInventoryManager.syncAllPlayers();
        source.sendSuccess(() -> Component.literal("§aCopied inventory from " + player.getName().getString() + " and synced to all players"), true);
        return 1;
    }
}
