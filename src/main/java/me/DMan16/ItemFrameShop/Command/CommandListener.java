package me.DMan16.ItemFrameShop.Command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

import me.DMan16.ItemFrameShop.ItemFrameShopMain;
import me.DMan16.ItemFrameShop.Utils.Utils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandListener implements CommandExecutor {
	
	public CommandListener() {
		PluginCommand command = ItemFrameShopMain.getInstance().getCommand("ItemFrameShop");
		assert command != null;
		command.setExecutor(this);
		command.setDescription(Utils.chatColors(ItemFrameShopMain.pluginNameColors + " &fcommand"));
	}
	
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
		ItemFrameShopMain.reloadConfigs();
//		sender.sendMessage(Utils.chatColors("&aConfig reloaded")); // From config
		String msg = ItemFrameShopMain.messages().configReloaded(sender instanceof Player ? (Player) sender : null);
		if (msg != null) sender.sendMessage(msg);
		return true;
	}
}