package me.DMan16.ItemFrameShop.Utils;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import me.DMan16.ItemFrameShop.ItemFrameShopMain;

public abstract class Listener implements org.bukkit.event.Listener {
	protected boolean isRegistered = false;
	
	protected void register() {
		Bukkit.getServer().getPluginManager().registerEvents(this, ItemFrameShopMain.getInstance());
		isRegistered = true;
	}
	
	protected void unregister() {
		HandlerList.unregisterAll(this);
		isRegistered = false;
	}
}