package me.DMan16.ItemFrameShop.Events;

import java.util.Objects;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import me.DMan16.ItemFrameShop.Shop.ItemFrameShop;

public abstract class ItemFrameShopEvent extends Event implements Cancellable {
	private final HandlerList Handlers = new HandlerList();
	protected boolean cancel = false;
	protected final ItemFrameShop ItemFrameShop;
	
	protected ItemFrameShopEvent(ItemFrameShop shop) {
		this.ItemFrameShop = Objects.requireNonNull(shop);
	}
	
	public ItemFrameShop getShop() {
		return ItemFrameShop;
	}
	
	public final void setCancelled(final boolean cancel) {
		this.cancel = cancel;
	}
	
	public final boolean isCancelled() {
		return cancel;
	}

	@Override
	public HandlerList getHandlers() {
		return Handlers;
	}
}