package me.DMan16.ItemFrameShop.Utils;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class EconomyManager {
	public final Economy economy;
	
	public EconomyManager(Economy economy) {
		this.economy = economy;
	}
	
	public String currency(double amount) {
		return economy.format(Utils.roundAfterDot(amount));
	}
	
	public EconomyResponse playerPay(@NotNull OfflinePlayer player, double amount) {
		return economy.withdrawPlayer(player,Math.abs(amount));
	}
	
	public EconomyResponse playerAdd(@NotNull OfflinePlayer player, double amount) {
		return economy.depositPlayer(player,Math.abs(amount));
	}
}