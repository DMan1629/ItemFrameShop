package me.DMan16.ItemFrameShop.Events;

import java.util.Objects;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import me.DMan16.ItemFrameShop.Shop.ItemFrameShop;
import org.jetbrains.annotations.NotNull;

public abstract class ItemFrameShopEvent extends Event implements Cancellable {
	private final HandlerList Handlers = new HandlerList();
	protected boolean cancel = false;
	protected final ItemFrameShop ItemFrameShop;
	
	protected ItemFrameShopEvent(@NotNull ItemFrameShop shop) {
		this.ItemFrameShop = shop;
	}
	
	@NotNull
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
	@NotNull
	public HandlerList getHandlers() {
		return Handlers;
	}
}