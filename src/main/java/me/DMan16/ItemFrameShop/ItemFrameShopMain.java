package me.DMan16.ItemFrameShop;

import me.DMan16.ItemFrameShop.Utils.Metrics;
import me.DMan16.ItemFrameShop.Utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.DMan16.ItemFrameShop.Command.CommandListener;
import me.DMan16.ItemFrameShop.Shop.ItemFrameShopManager;
import me.DMan16.ItemFrameShop.Shop.ShopListener;
import me.DMan16.ItemFrameShop.Utils.EconomyManager;
import me.DMan16.ItemFrameShop.Utils.Utils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class ItemFrameShopMain extends JavaPlugin {
	private static ItemFrameShopMain instance;
	public static final String pluginName = "ItemFrameShop";
	public static final String pluginNameColors = "&6&lItem&b&lFrame&a&lShop";
	private static EconomyManager EconomyManager;
	private static ItemFrameShopManager ItemFrameShopManager;
	private int count = 0;
	private MessagesManager messages = null;

	public void onEnable() {
		instance = this;
		saveDefaultConfig();
		messages = new MessagesManager();
		if (Utils.getVersionMain() < 1 || Utils.getVersionInt() < 13) {
			Utils.chatColorsLogPlugin("&cunsupported version! Minimum supported version: 1.13");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		try {
			ItemFrameShopManager = new ItemFrameShopManager();
		} catch (Exception e) {
			Utils.chatColorsLogPlugin("&cerror reading files! Error:");
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(getInstance());
			return;
		}
		Utils.chatColorsLogPlugin("&fTrying to hook to Economy provider...");
		new BukkitRunnable() {
			public void run() {
				if (count++ > 30) {
					Utils.chatColorsLogPlugin("&cno supported Economy provider found!");
					Bukkit.getPluginManager().disablePlugin(instance);
					return;
				}
				RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
				try {
					assert economyProvider != null;
					EconomyManager = new EconomyManager(economyProvider.getProvider());
					this.cancel();
					finishLoading();
				} catch (Exception e) {}
			}
		}.runTaskTimer(this,1,20);
	}
	
	private void finishLoading() {
		Utils.chatColorsLogPlugin("&aHooked to &fVault&a economy! Provider: &b" + EconomyManager.economy.getName());
		new CommandListener();
		new ShopListener();
		Utils.chatColorsLogPlugin("&aLoaded, running on version: &b" + Utils.getVersion());
		try {
			Metrics metrics = new Metrics(instance,12470);
			Utils.chatColorsLogPlugin("&aHooked to &fMetrics&a provider!");
		} catch (Exception e) {}
		try {
			new UpdateChecker(this,94017).getVersion(version -> {
				if (this.getDescription().getVersion().equalsIgnoreCase(version)) {
					Utils.chatColorsLogPlugin("&aRunning latest version!");
				} else {
					Utils.chatColorsLogPlugin("&aNew version available &f- &av&b" + version);
				}
			});
		} catch (Exception e) {}
	}
	
	public static void reloadConfigs() {
		instance.reloadConfig();
		instance.messages.reload();
	}
	
	@NotNull
	public static ItemFrameShopMain getInstance() {
		return instance;
	}
	
	@NotNull
	public static FileConfiguration config() {
		return instance.getConfig();
	}
	
	@NotNull
	public static MessagesManager messages() {
		return instance.messages;
	}
	
	@NotNull
	public static EconomyManager getEconomyManager() {
		return EconomyManager;
	}
	
	@NotNull
	public static ItemFrameShopManager getItemFrameShopManager() {
		return ItemFrameShopManager;
	}

	public void onDisable() {
		Bukkit.getServer().getWorlds().forEach(world -> ItemFrameShopManager.write(world));
		HandlerList.unregisterAll(this);
		Bukkit.getScheduler().cancelTasks(this);
		Utils.chatColorsLogPlugin("&aDisabed successfully!");
	}
}