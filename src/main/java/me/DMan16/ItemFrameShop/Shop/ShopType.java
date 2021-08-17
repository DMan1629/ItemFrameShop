package me.DMan16.ItemFrameShop.Shop;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ShopType {
	INVENTORY("inventory"),
	ITEMFRAME("frame"),
	ADMINSHOP("admin");
	
	public final String name;
	
	ShopType(@NotNull String name) {
		this.name = name;
	}
	
	@Nullable
	public static ShopType getType(String str) {
		if (str == null) return null;
		for (ShopType type : values()) if (type.name.equals(str)) return type;
		return null;
	}
	
	public boolean requireInventory() {
		return this == INVENTORY;
	}
	
	public boolean isIndependent() {
		return !requireInventory();
	}
}