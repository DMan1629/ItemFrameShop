package me.DMan16.ItemFrameShop.Shop;

import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import me.DMan16.ItemFrameShop.Main;
import me.DMan16.ItemFrameShop.Utils.Listener;
import me.DMan16.ItemFrameShop.Utils.Permissions;
import me.DMan16.ItemFrameShop.Utils.Utils;

public class ShopListener extends Listener {
	
	public ShopListener() {
		register();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onShopCreateEvent(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof ItemFrame) || event.isCancelled()) return;
		ItemFrame frame = (ItemFrame) event.getRightClicked();
		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItemInMainHand();
		if (frame == null || player == null || !player.isSneaking() || Utils.isNull(item) || !Utils.isNull(frame.getItem()) ||
				Main.getItemFrameShopManager().isShop(frame)) return;
		event.setCancelled(true);
		boolean createInventory = Permissions.createInventoryShopPermission(player);
		boolean createFrame = Permissions.createFrameShopPermission(player);
		boolean createAdmin = Permissions.AdminShopPermission(player);
		ItemFrameShop shop = new ItemFrameShop(frame,player.getUniqueId(),item);
		if (shop.getAttachedBlockState() != null && (createInventory || createFrame || createAdmin)) shop.onItemFrameClickOff(event);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		new BukkitRunnable() {
			public void run() {
				Main.getItemFrameShopManager().spawnShopArmorStands(event.getPlayer());
			}
		}.runTask(Main.getInstance());
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
		new BukkitRunnable() {
			public void run() {
				Main.getItemFrameShopManager().spawnShopArmorStands(event.getPlayer());
			}
		}.runTask(Main.getInstance());
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onSaveEvent(WorldSaveEvent event) {
		Main.getItemFrameShopManager().write(event.getWorld());
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onLoadEvent(WorldLoadEvent event) {
		Main.getItemFrameShopManager().loadInfoFromPath(event.getWorld());
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onUnloadEvent(WorldUnloadEvent event) {
		Main.getItemFrameShopManager().unload(event.getWorld());
	}
}