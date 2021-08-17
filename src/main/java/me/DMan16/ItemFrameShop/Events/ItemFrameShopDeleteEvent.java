package me.DMan16.ItemFrameShop.Events;

import org.bukkit.entity.Player;

import me.DMan16.ItemFrameShop.Shop.ItemFrameShop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemFrameShopDeleteEvent extends ItemFrameShopEvent {
	private final Player player;
	
	public ItemFrameShopDeleteEvent(@NotNull ItemFrameShop ItemFrameShop, @Nullable Player player) {
		super(ItemFrameShop);
		this.player = player;
	}
	
	/**
	 * Null if not deleted by a player
	 */
	@Nullable
	public Player getWhoDeleted() {
		return player;
	}
}