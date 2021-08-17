package me.DMan16.ItemFrameShop.Events;

import java.util.Objects;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.DMan16.ItemFrameShop.ItemFrameShopMain;
import me.DMan16.ItemFrameShop.Shop.ItemFrameShop;
import me.DMan16.ItemFrameShop.Shop.ShopType;
import me.DMan16.ItemFrameShop.Utils.Utils;
import org.jetbrains.annotations.NotNull;

public class ItemFrameShopCreateEvent extends ItemFrameShopEvent {
	private final Player owner;
	private ShopType type;
	private ItemStack sellItem;
	private double price;
	
	public ItemFrameShopCreateEvent(@NotNull ItemFrameShop ItemFrameShop, @NotNull Player owner, @NotNull ShopType type, @NotNull ItemStack sellItem, double price) {
		super(ItemFrameShop);
		this.owner = owner;
		this.type = type;
		this.sellItem = Objects.requireNonNull(Utils.isNull(sellItem) ? null : sellItem);
		setPrice(price);
	}
	
	@NotNull
	public Player getOwner() {
		return owner;
	}
	
	@NotNull
	public ShopType getType() {
		return type;
	}
	
	@NotNull
	public ItemFrameShopCreateEvent setType(@NotNull ShopType type) {
		this.type = type;
		return this;
	}
	
	public double getPrice() {
		return price;
	}
	
	/**
	 * @param price > 0; closed if <= 0
	 */
	@NotNull
	public ItemFrameShopCreateEvent setPrice(double price) {
		this.price = Math.max(0,Utils.roundAfterDot(price));
		return this;
	}
	
	/**
	 * @return clone of the selling item
	 */
	@NotNull
	public ItemStack getSellItem() {
		return this.sellItem.clone();
	}
	
	@NotNull
	public ItemFrameShopCreateEvent setSellItem(@NotNull ItemStack item) {
		this.sellItem = Objects.requireNonNull(Utils.isNull(item) ? null : item);
		return this;
	}
}