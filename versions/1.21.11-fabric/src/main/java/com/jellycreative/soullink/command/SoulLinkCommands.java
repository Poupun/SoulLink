package com.jellycreative.soullink.command;

import com.jellycreative.soullink.config.SoulLinkConfig;
import com.jellycreative.soullink.event.SoulLinkEventHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Commands for Soul-Link mod.
 */
public class SoulLinkCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("soullink")
                .then(CommandManager.literal("toggle")
                    .executes(SoulLinkCommands::toggleLink))
                .then(CommandManager.literal("status")
                    .executes(SoulLinkCommands::showStatus))
        );
    }
    
    private static boolean hasPermission(ServerCommandSource source) {
        try {
            // Console always has permission
            if (source.getEntity() == null) return true;
            // For singleplayer, the host always has permission
            if (source.getServer().isSingleplayer()) return true;
            // In 1.21.11, check if player is an operator by checking ops list
            if (source.getEntity() instanceof ServerPlayerEntity player) {
                var opList = source.getServer().getPlayerManager().getOpList();
                var profile = player.getGameProfile();
                // GameProfile in 1.21.11 uses name() as a record accessor
                String playerName = profile.name();
                var opNames = opList.getNames();
                for (String name : opNames) {
                    if (name.equalsIgnoreCase(playerName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // If any error, default to no permission
            return false;
        }
        return false;
    }

    private static int toggleLink(CommandContext<ServerCommandSource> context) {
        // Check permission manually
        if (!hasPermission(context.getSource())) {
            context.getSource().sendFeedback(() -> Text.literal("§c[Soul-Link] You don't have permission to use this command."), false);
            return 0;
        }
        
        boolean newState = !SoulLinkEventHandler.isLinkEnabled();
        SoulLinkEventHandler.setLinkEnabled(newState);

        String message = newState ? 
            "§6[Soul-Link] §aLink enabled! All players are now synchronized." :
            "§6[Soul-Link] §cLink disabled! Players are no longer synchronized.";

        context.getSource().getServer().getPlayerManager().broadcast(
            Text.literal(message), false
        );

        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> context) {
        boolean enabled = SoulLinkEventHandler.isLinkEnabled();

        StringBuilder status = new StringBuilder();
        status.append("§6[Soul-Link] Status:\n");
        status.append("§7Link: ").append(enabled ? "§aEnabled" : "§cDisabled").append("\n");
        status.append("§7Syncing: ");

        if (SoulLinkConfig.isSyncHealth()) status.append("§aHealth ");
        if (SoulLinkConfig.isSyncHunger()) status.append("§aHunger ");
        if (SoulLinkConfig.isSyncKnockback()) status.append("§aKnockback ");
        if (SoulLinkConfig.isSyncInventory()) status.append("§aInventory ");

        context.getSource().sendFeedback(() -> Text.literal(status.toString()), false);

        return 1;
    }
}
