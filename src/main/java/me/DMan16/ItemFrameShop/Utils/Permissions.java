package me.DMan16.ItemFrameShop.Utils;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import me.DMan16.ItemFrameShop.ItemFrameShopMain;
import me.DMan16.ItemFrameShop.Shop.ShopType;
import org.jetbrains.annotations.NotNull;

public class Permissions {
	public static boolean createInventoryShopPermission(@NotNull Player player) {
		return hasPermission(player,"itemframeshop.create.inventory");
	}
	
	public static boolean createFrameShopPermission(@NotNull Player player) {
		return hasPermission(player,"itemframeshop.create.frameshop");
	}
	
	public static boolean AdminShopPermission(@NotNull Player player) {
		return (hasPermission(player,"itemframeshop.adminshop") || (ItemFrameShopMain.config().getBoolean("adminshop-op") && player.isOp())) &&
				player.getGameMode() == GameMode.CREATIVE;
	}
	
	public static boolean createShopPermission(@NotNull Player player, @NotNull ShopType type) {
		if (type == ShopType.INVENTORY) return createInventoryShopPermission(player);
		if (type == ShopType.ITEMFRAME) return createFrameShopPermission(player);
		if (type == ShopType.ADMINSHOP) return AdminShopPermission(player);
		return false;
	}
	
	public static boolean changeOtherShopPermission(@NotNull Player player) {
		return hasPermission(player,"itemframeshop.change.othershop");
	}
	
	private static boolean hasPermission(@NotNull Player player, @NotNull String perm) {
		Permission permission = Bukkit.getServer().getPluginManager().getPermission(perm);
		return permission != null && Objects.requireNonNull(player).hasPermission(permission);
	}
}