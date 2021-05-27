package me.DMan16.ItemFrameShop.Events;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;

import me.DMan16.ItemFrameShop.Shop.ItemFrameShop;

public class ItemFrameShopDeleteEvent extends ItemFrameShopEvent {
	private final Player player;
	
	public ItemFrameShopDeleteEvent(ItemFrameShop ItemFrameShop, @Nullable Player player) {
		super(ItemFrameShop);
		this.player = player;
	}
	
	/**
	 * Before deletion
	 */
	public ItemFrameShop getItemFrameShop() {
		return ItemFrameShop;
	}
	
	/**
	 * Null if not deleted by a player
	 */
	public Player getWhoDeleted() {
		return player;
	}
}