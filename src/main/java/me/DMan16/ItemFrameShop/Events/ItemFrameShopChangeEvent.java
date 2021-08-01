package me.DMan16.ItemFrameShop.Events;

import java.util.Objects;

import org.bukkit.entity.Player;

import me.DMan16.ItemFrameShop.Shop.ItemFrameShop;
import me.DMan16.ItemFrameShop.Shop.ShopType;

public class ItemFrameShopChangeEvent extends ItemFrameShopEvent {
	private final Player player;
	private ShopType type;
	private double newPrice;
	
	public ItemFrameShopChangeEvent(ItemFrameShop ItemFrameShop, Player player, ShopType newType, double newPrice) {
		super(ItemFrameShop);
		this.player = Objects.requireNonNull(player);
		this.type = Objects.requireNonNull(newType);
		this.newPrice = newPrice;
	}
	
	/**
	 * Before change
	 */
	public ItemFrameShop getItemFrameShop() {
		return ItemFrameShop;
	}
	
	public Player getWhoChanged() {
		return player;
	}
	
	public ShopType getOldType() {
		return ItemFrameShop.getType();
	}
		
	public ShopType getNewType() {
		return type;
	}
	
	public double getOldPrice() {
		return ItemFrameShop.getPrice();
	}
	
	public double getNewPrice() {
		return newPrice;
	}
	
	public ItemFrameShopChangeEvent setNewType(ShopType type) {
		this.type = Objects.requireNonNull(type);
		return this;
	}
	
	public ItemFrameShopChangeEvent setNewType(double price) {
		this.newPrice = price;
		return this;
	}
}