// ===== BEGIN CommandLCReload.java (UPDATED) =====
package com.ultimatekevin.lucraftcorefix.command;

import com.ultimatekevin.lucraftcorefix.util.ReloadHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class CommandLCReload extends CommandBase {

    @Override
    public String getName() {
        return "lcreload";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/lcreload";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (server.isDedicatedServer() || server.isSinglePlayer()) {
            // 将 sender 传递给 ReloadHandler
            ReloadHandler.reloadAddons(server, sender);
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "This command can only be run on the server side (dedicated or single-player)."));
        }
    }
}
// ===== END CommandLCReload.java (UPDATED) =====