package me.DMan16.ItemFrameShop.Utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class ListenerInventory extends Listener {
	protected final Inventory inventory;
	
	public ListenerInventory(@NotNull Inventory inv) {
		this.inventory = inv;
	}
	
	@EventHandler(ignoreCancelled = true)
	public void unregisterOnClose(InventoryCloseEvent event) {
		if (event.getView().getTopInventory().equals(inventory)) unregister();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void unregisterOnLeaveEvent(PlayerQuitEvent event) {
		if ((inventory.getHolder() instanceof OfflinePlayer) && event.getPlayer().getUniqueId().equals(((OfflinePlayer) inventory.getHolder()).getUniqueId()))
			unregister();
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onInventoryDrag(InventoryDragEvent event) {
		if (event.getView().getTopInventory().equals(inventory)) event.setCancelled(true);
	}
}