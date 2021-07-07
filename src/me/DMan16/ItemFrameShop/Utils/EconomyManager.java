package me.DMan16.ItemFrameShop.Utils;

import me.DMan16.ItemFrameShop.Main;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

public class EconomyManager {
	public final Economy economy;
	
	public EconomyManager(Economy economy) {
		this.economy = economy;
	}
	
	public String currency(double amount) {
		return economy.format(Utils.roundAfterDot(amount,Math.min(Math.max(Main.config().getInt("price-round-after-dot"),0),2))); // From config
	}
	
	public EconomyResponse playerPay(OfflinePlayer player, double amount) {
		return economy.withdrawPlayer(player,Math.abs(amount));
	}
	
	public EconomyResponse playerAdd(OfflinePlayer player, double amount) {
		return economy.depositPlayer(player,Math.abs(amount));
	}
}