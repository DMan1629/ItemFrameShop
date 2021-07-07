package me.DMan16.ItemFrameShop.Command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import me.DMan16.ItemFrameShop.Main;
import me.DMan16.ItemFrameShop.Utils.Utils;

public class CommandListener implements CommandExecutor {
	
	public CommandListener() {
		PluginCommand command = Main.getInstance().getCommand(Main.pluginName);
		command.setExecutor(this);
		command.setDescription(Utils.chatColors(Main.pluginNameColors + " &fcommand"));
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
//		if (args.length == 1 && args[0].equalsIgnoreCase("test")) sender.sendMessage(Main.ItemFrameShopManager.toString());
//		else if (args.length == 1 && args[0].equalsIgnoreCase("config")) sender.sendMessage(Main.config().saveToString());
//		else {
		Main.getInstance().reloadConfig();
		sender.sendMessage(Utils.chatColors("&aConfig reloaded"));
//		}
		return true;
	}
}