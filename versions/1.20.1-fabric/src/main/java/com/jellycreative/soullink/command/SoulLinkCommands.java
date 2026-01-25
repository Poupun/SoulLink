package com.jellycreative.soullink.command;

import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.event.SoulLinkEventHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Commands for the Soul-Link mod.
 */
public class SoulLinkCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("soullink")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("toggle")
                    .executes(SoulLinkCommands::toggleLink))
                .then(CommandManager.literal("status")
                    .executes(SoulLinkCommands::showStatus))
        );
    }

    private static int toggleLink(CommandContext<ServerCommandSource> context) {
        boolean newState = !SoulLinkEventHandler.isLinkEnabled();
        SoulLinkEventHandler.setLinkEnabled(newState);

        String message = newState ? "§a[Soul-Link] Link enabled!" : "§c[Soul-Link] Link disabled!";
        context.getSource().sendFeedback(() -> Text.literal(message), true);

        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) {
        boolean enabled = SoulLinkEventHandler.isLinkEnabled();
        StringBuilder status = new StringBuilder();
        status.append("§6[Soul-Link] Status:\n");
        status.append("  §7Link: ").append(enabled ? "§aEnabled" : "§cDisabled").append("\n");
        status.append("  §7Health Sync: ").append(SoulLinkConfig.isSyncHealth() ? "§aOn" : "§cOff").append("\n");
        status.append("  §7Hunger Sync: ").append(SoulLinkConfig.isSyncHunger() ? "§aOn" : "§cOff").append("\n");
        status.append("  §7Knockback Sync: ").append(SoulLinkConfig.isSyncKnockback() ? "§aOn" : "§cOff").append("\n");
        status.append("  §7Inventory Sync: ").append(SoulLinkConfig.isSyncInventory() ? "§aOn" : "§cOff");

        context.getSource().sendFeedback(() -> Text.literal(status.toString()), false);

        return 1;
    }
}
