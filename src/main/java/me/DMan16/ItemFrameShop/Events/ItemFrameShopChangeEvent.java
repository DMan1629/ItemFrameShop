package me.DMan16.ItemFrameShop.Events;

import org.bukkit.entity.Player;

import me.DMan16.ItemFrameShop.Shop.ItemFrameShop;
import me.DMan16.ItemFrameShop.Shop.ShopType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemFrameShopChangeEvent extends ItemFrameShopEvent {
	private final Player player;
	private ShopType type;
	private double newPrice;
	
	public ItemFrameShopChangeEvent(@NotNull ItemFrameShop ItemFrameShop, @Nullable Player player, @NotNull ShopType newType, double newPrice) {
		super(ItemFrameShop);
		this.player = player;
		this.type = newType;
		this.newPrice = newPrice;
	}
	
	@Nullable
	public Player getWhoChanged() {
		return player;
	}
	
	@NotNull
	public ShopType getOldType() {
		return ItemFrameShop.getType();
	}
	
	@NotNull
	public ShopType getNewType() {
		return type;
	}
	
	public double getOldPrice() {
		return ItemFrameShop.getPrice();
	}
	
	public double getNewPrice() {
		return newPrice;
	}
	
	@NotNull
	public ItemFrameShopChangeEvent setNewType(@NotNull ShopType type) {
		this.type = type;
		return this;
	}
	
	@NotNull
	public ItemFrameShopChangeEvent setNewType(double price) {
		this.newPrice = price;
		return this;
	}
}