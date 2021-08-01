package me.DMan16.ItemFrameShop;

import com.google.common.base.Charsets;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main extends JavaPlugin {
	private static Main instance;
	public static final String pluginName = "ItemFrameShop";
	public static final String pluginNameColors = "&6&lItem&b&lFrame&a&lShop";
	private static EconomyManager EconomyManager;
	private static ItemFrameShopManager ItemFrameShopManager;
	private int count = 0;
	private FileConfiguration configMessages = null;

	public void onEnable() {
		instance = this;
		String versionMC = Bukkit.getServer().getVersion().split("\\(MC:")[1].split("\\)")[0].trim().split(" ")[0].trim();
		if (Integer.parseInt(versionMC.split("\\.")[0]) < 1 || Integer.parseInt(versionMC.split("\\.")[1]) < 13) {
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
				count++;
				if (count > 30) {
					Utils.chatColorsLogPlugin("&cno supported Economy provider found!");
					Bukkit.getPluginManager().disablePlugin(instance);
					return;
				}
				RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
				try {
					EconomyManager = new EconomyManager(economyProvider.getProvider());
					Utils.chatColorsLogPlugin("&aHook to Economy provider!");
					new CommandListener();
					new ShopListener();
					Utils.chatColorsLogPlugin("&aLoaded, running on version: &f" + versionMC);
					this.cancel();
				} catch (Exception e) {}
			}
		}.runTaskTimer(this,1,20);
		loadConfigs();
	}
	
	private void loadConfigs() {
		saveDefaultConfig();
		this.configMessages = loadCustomConfig("messages.yml");
	}
	
	public static void reloadConfigs() {
		instance.reloadConfig();
		instance.configMessages = loadCustomConfig("messages.yml");
	}
	
	private static FileConfiguration loadCustomConfig(String name) {
		File file = new File(instance.getDataFolder(),name);
		if (!file.exists()) instance.saveResource(name,false);
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		InputStream inputStream = instance.getResource(name);
		if (inputStream != null) config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream,Charsets.UTF_8)));
		return config;
	}
	
	public static Main getInstance() {
		return instance;
	}
	
	public static FileConfiguration config() {
		return instance.getConfig();
	}
	
	public static FileConfiguration configMessages() {
		return instance.configMessages;
	}
	
	public static List<String> getStringsConfig(FileConfiguration config, String option) {
		if (!configMessages().contains(option)) return null;
		if (configMessages().isString(option)) return new ArrayList(Arrays.asList((configMessages().getString(option))));
		if (configMessages().isList(option)) return configMessages().getStringList(option);
		return null;
	}
	
	public static EconomyManager getEconomyManager() {
		return EconomyManager;
	}
	
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