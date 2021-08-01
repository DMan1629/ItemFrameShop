package me.DMan16.ItemFrameShop.Events;

import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.DMan16.ItemFrameShop.Main;
import me.DMan16.ItemFrameShop.Shop.ItemFrameShop;
import me.DMan16.ItemFrameShop.Shop.ShopType;
import me.DMan16.ItemFrameShop.Utils.Utils;

public class ItemFrameShopCreateEvent extends ItemFrameShopEvent {
	private final Player owner;
	private ShopType type;
	private ItemStack sellItem;
	private double price;
	
	public ItemFrameShopCreateEvent(ItemFrameShop ItemFrameShop, Player owner, ShopType type, ItemStack sellItem, double price) {
		super(ItemFrameShop);
		this.owner = Objects.requireNonNull(owner);
		this.type = Objects.requireNonNull(type);
		this.sellItem = Objects.requireNonNull(Utils.isNull(sellItem) ? null : sellItem);
		setPrice(price);
	}
	
	/**
	 * Before creation
	 */
	public ItemFrameShop getItemFrameShop() {
		return ItemFrameShop;
	}
	
	public Player getOwner() {
		return owner;
	}
	
	public ShopType getType() {
		return type;
	}
	
	public ItemFrameShopCreateEvent setType(ShopType type) {
		this.type = Objects.requireNonNull(type);
		return this;
	}
	
	public double getPrice() {
		return price;
	}
	
	/**
	 * @param price > 0; closed if <= 0
	 */
	public ItemFrameShopCreateEvent setPrice(double price) {
		this.price = Math.max(0,Utils.roundAfterDot(price,Math.min(Math.max(Main.config().getInt("price-round-after-dot"),0),2)));
		return this;
	}
	
	/**
	 * @return clone of the selling item
	 */
	public ItemStack getSellItem() {
		return this.sellItem.clone();
	}
	
	public ItemFrameShopCreateEvent setSellItem(ItemStack item) {
		this.sellItem = Objects.requireNonNull(Utils.isNull(item) ? null : item);
		return this;
	}
}