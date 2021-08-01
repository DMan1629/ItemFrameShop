package me.DMan16.ItemFrameShop.Utils;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import me.DMan16.ItemFrameShop.Main;
import me.DMan16.ItemFrameShop.Shop.ShopType;

public class Permissions {
	public static boolean createInventoryShopPermission(Player player) {
		return hasPermission(player,"itemframeshop.create.inventory");
	}
	
	public static boolean createFrameShopPermission(Player player) {
		return hasPermission(player,"itemframeshop.create.frameshop");
	}
	
	public static boolean AdminShopPermission(Player player) {
		return (hasPermission(player,"itemframeshop.adminshop") || (Main.config().getBoolean("adminshop-op") && player.isOp())) &&
				player.getGameMode() == GameMode.CREATIVE;
	}
	
	public static boolean createShopPermission(Player player, ShopType type) {
		if (type == ShopType.INVENTORY) return createInventoryShopPermission(player);
		if (type == ShopType.ITEMFRAME) return createFrameShopPermission(player);
		if (type == ShopType.ADMINSHOP) return AdminShopPermission(player);
		return false;
	}
	
	public static boolean changeOtherShopPermission(Player player) {
		return hasPermission(player,"itemframeshop.change.othershop");
	}
	
	private static boolean hasPermission(Player player, String perm) {
		Permission permission = Bukkit.getServer().getPluginManager().getPermission(perm);
		return permission != null && Objects.requireNonNull(player).hasPermission(permission);
	}
}