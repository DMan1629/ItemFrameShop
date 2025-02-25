package me.DMan16.ItemFrameShop.Shop;

import me.DMan16.ItemFrameShop.Events.ItemFrameShopChangeEvent;
import me.DMan16.ItemFrameShop.Events.ItemFrameShopCreateEvent;
import me.DMan16.ItemFrameShop.Events.ItemFrameShopDeleteEvent;
import me.DMan16.ItemFrameShop.ItemFrameShopMain;
import me.DMan16.ItemFrameShop.Utils.*;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ItemFrameShop extends Listener {
	private static final ItemStack empty;
	private static final ItemStack close;
	private static final ItemStack delete;
	
	static {
		empty = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = empty.getItemMeta();
		assert meta != null;
		meta.setDisplayName(" ");
		empty.setItemMeta(meta);
		close = ReflectionUtils.setNameItem(new ItemStack(Material.BARRIER),ReflectionUtils.buildIChatBaseComponentString("spectatorMenu.close",true,"red"));
		delete = ReflectionUtils.setNameItem(new ItemStack(Material.BARRIER),ReflectionUtils.buildIChatBaseComponentString("selectWorld.delete",true,"dark_red"));
	}
	
	private final ItemFrame frame;
	private UUID ownerID = null;
	private ShopType type = null;
	private ItemStack sellItem = null;
	private int nameDisplay = 0;
	private int priceDisplay = 0;
	private double price;
	private boolean open = false;
	private int amount;
	
	private void setInfo(@NotNull UUID ownerID, @NotNull ShopType type, @NotNull ItemStack sellItem, int nameDisplay, int priceDisplay, double price, int amount) {
		this.sellItem = Objects.requireNonNull(Utils.asQuantity(sellItem,1));
		this.ownerID = ownerID;
		this.type = type;
		this.price = fixPrice(price);
		this.nameDisplay = nameDisplay;
		this.priceDisplay = priceDisplay;
		this.amount = setAmount(amount);
	}
	
	public ItemFrameShop(@NotNull ItemFrame frame, @NotNull UUID ownerID, @NotNull ItemStack sellItem) {
		this.frame = frame;
		ItemFrameShop shop = ItemFrameShopMain.getItemFrameShopManager().getShop(frame);
		if (shop != null) setInfo(shop.ownerID,shop.type,shop.sellItem,shop.nameDisplay,shop.priceDisplay,shop.price,shop.amount);
		else setInfo(ownerID,ShopType.INVENTORY,sellItem,ReflectionUtils.getNextEntityID(),ReflectionUtils.getNextEntityID(),-1,0);
	}
	
	public ItemFrameShop(@NotNull ItemFrame frame, @NotNull UUID ownerID, @NotNull ShopType type, @NotNull ItemStack sellItem, double price, int amount, boolean fromFile) {
		this.frame = frame;
		ItemFrameShop shop = fromFile ? null : ItemFrameShopMain.getItemFrameShopManager().getShop(frame);
		if (shop != null) setInfo(shop.ownerID,shop.type,shop.sellItem,shop.nameDisplay,shop.priceDisplay,shop.price,shop.amount);
		else setInfo(ownerID,type,sellItem,ReflectionUtils.getNextEntityID(),ReflectionUtils.getNextEntityID(),price,amount);
		if (isShop()) {
			this.register();
			this.frame.setItem(ReflectionUtils.setNameItem(getSellItem(),null));
			if (!fromFile) ItemFrameShopMain.getItemFrameShopManager().newShop(this);
		}
	}
	
	public boolean isShop() {
		return frame != null && !frame.isDead() && ownerID != null && type != null && sellItem != null && nameDisplay > 0 && priceDisplay > 0;
	}
	
	public boolean createShop(@NotNull Player owner, @NotNull ShopType type, double price) {
		ItemFrameShopCreateEvent event = new ItemFrameShopCreateEvent(this,owner,type,sellItem,fixPrice(price));
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) return false;
		setInfo(event.getOwner().getUniqueId(),event.getType(),sellItem,ReflectionUtils.getNextEntityID(),ReflectionUtils.getNextEntityID(),event.getPrice(),0);
		if (!this.isRegistered) this.register();
		this.frame.setItem(ReflectionUtils.setNameItem(getSellItem(),null));
		ItemFrameShopMain.getItemFrameShopManager().newShop(this);
		return true;
	}
	
	public boolean changeShop(@Nullable Player player, @NotNull ShopType newType, double newPrice) {
		if (newPrice != 0 && (newPrice <= minimum() || newPrice > maximum())) return false;
		ItemFrameShopChangeEvent event = new ItemFrameShopChangeEvent(this,player,newType,newPrice);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled() || !setPrice(event.getNewPrice())) return false;
		this.type = event.getNewType();
		if (this.type.requireInventory()) {
			if (event.getOldType().isIndependent() && event.getOldType() != ShopType.ADMINSHOP) {
				Inventory inv = getInventory();
				int count = 0;
				for (int i = 0; i < this.amount; i++) {
					if (inv == null || !inv.addItem(getSellItem()).isEmpty()) count++;
				}
				for (int i = 0; i < count; i++) {
					if (player == null) Utils.dropItem(this.frame.getLocation(),getSellItem());
					else Utils.givePlayer(player,getSellItem(),false);
				}
			}
			setAmount(0);
		}
		if (!this.isRegistered) this.register();
		canBeOpen();
		return true;
	}
	
	public boolean delete(@Nullable Player player) {
		if (!isShop()) return false;
		ItemFrameShopDeleteEvent event = new ItemFrameShopDeleteEvent(this,player);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) return false;
		ItemStack item = getSellItem();
		this.open = false;
		this.unregister();
		this.sellItem = null;
		this.frame.setItem(null);
		this.ownerID = null;
		ItemFrameShopMain.getItemFrameShopManager().remove(this,player);
		setPrice(0);
		this.amount = 0;
		this.nameDisplay = 0;
		this.priceDisplay = 0;
		ItemFrameShopMain.getItemFrameShopManager().despawnShopArmorStands(this);
		if (this.type.isIndependent() && this.type != ShopType.ADMINSHOP) for (int i = 0; i < this.amount; i++) {
			if (player == null) Utils.dropItem(this.frame.getLocation(),item);
			else Utils.givePlayer(player,item,false);
		}
		this.type = null;
		return true;
	}
	
	@NotNull
	public ItemFrame getFrame() {
		return frame;
	}
	
	@NotNull
	public UUID getOwnerID() {
		return ownerID;
	}
	
	@NotNull
	public ShopType getType() {
		return type;
	}
	
	@NotNull
	public ItemStack getSellItem() {
		return Objects.requireNonNull(Utils.asQuantity(sellItem,1));
	}
	
	public double getPrice() {
		return price;
	}
	
	public boolean setPrice(double price) {
		if (price != 0 && (price <= minimum() || price > maximum())) return false;
		this.price = fixPrice(price);
		canBeOpen();
		return true;
	}
	
	public boolean isOpen() {
		return this.open;
	}
	
	public int getNameDisplay() {
		return nameDisplay;
	}
	
	public int getPriceDisplay() {
		return priceDisplay;
	}
	
	private double fixPrice(double price) {
		return Utils.roundAfterDot(price);
	}
	
	public int getAmount() {
		return getAmount(this.type);
	}
	
	@NotNull
	public String getAmountStr() {
		return getAmountStr(this.type,getAmount());
	}
	
	@NotNull
	private static String getAmountStr(@NotNull ShopType type, int amount) {
		return type == ShopType.ADMINSHOP ? "∞" : "" + amount;
	}
	
	private int getAmount(@NotNull ShopType type) {
		int amount = 0;
		if (type.isIndependent()) amount = this.amount;
		else {
			Inventory inv = getInventory();
			if (inv != null) for (ItemStack item : inv.getStorageContents()) if (Utils.isSame(item,getSellItem())) amount += item.getAmount();
		}
		return amount;
	}
	
	public void canBeOpen() {
		if (isShop() && (this.type == ShopType.ADMINSHOP || getAmount() > 0) && getPrice() != 0 && getPrice() > minimum() && getPrice() <= maximum()) {	// open
			if (isOpen()) return;
			this.open = true;
			ItemFrameShopMain.getItemFrameShopManager().spawnShopArmorStands(this);
		} else {
			if (!isOpen()) return;
			this.open = false;
			ItemFrameShopMain.getItemFrameShopManager().despawnShopArmorStands(this);
		}
	}
	
	/**
	 * Only works for independent shops
	 * @return new amount
	 */
	public int setAmount(int amount) {
		if (this.type != null && this.type.isIndependent() && this.type != ShopType.ADMINSHOP) this.amount = Math.max(0,amount);
		canBeOpen();
		return getAmount();
	}
	
	/**
	 * Only works for independent shops
	 * @return new amount
	 */
	public int addAmount(int amount) {
		int old = getAmount();
		if (this.type != null && this.type.isIndependent() && this.type != ShopType.ADMINSHOP)
			if (((long)old + (long)amount) <= Integer.MAX_VALUE) return setAmount(old + amount);
		return old;
	}
	
	/**
	 * @param amount > 0
	 * @return if removal was successful
	 */
	public boolean removeAmount(int amount) {
		if (!testRemove(amount)) return false;
		if (this.type.isIndependent()) setAmount(this.amount - amount);
		else while (amount > 0) {
			Inventory inv = getInventory();
			if (inv != null) for (int i = 0; i < inv.getSize(); i++) {
				ItemStack item = inv.getItem(i);
				if (Utils.isNull(item) || !Utils.isSame(getSellItem(),item)) continue;
				if (item.getAmount() >= amount) {
					int newAmount = item.getAmount() - amount;
					amount = 0;
					if (newAmount > 0) item.setAmount(newAmount);
					else item = null;
				} else {
					amount -= item.getAmount();
					item = null;
				}
				inv.setItem(i,item);
			}
		}
		canBeOpen();
		return true;
	}
	
	private boolean testRemove(int amount) {
		if (amount <= 0 || this.type == null) return false;
		if (this.type == ShopType.ADMINSHOP) return true;
		return amount <= getAmount() && (!this.type.requireInventory() || getInventory() != null);
	}
	
	@NotNull
	public Block getAttachedBlock() {
		return frame.getLocation().getBlock().getRelative(frame.getAttachedFace());
	}
	
	@Nullable
	public BlockState getAttachedBlockState() {
		Block block = getAttachedBlock();
		if (block.isEmpty()) return null;
		return block.getState();
	}
	
	@Nullable
	public Inventory getInventory() {
		if (this.type == null || this.type.isIndependent()) return null;
		BlockState state = getAttachedBlockState();
		if (!(state instanceof InventoryHolder)) return null;
		boolean barrel = state.getType().name().equals("BARREL");
		boolean shulkerBox = state.getType().name().contains("SHULKER_BOX");
		boolean chest = state.getType() == Material.CHEST || state.getType() == Material.TRAPPED_CHEST;
		
		boolean allow = barrel || shulkerBox || chest;
		if (!allow) return null;
		Inventory inv;
		try {
			inv = ((DoubleChest) ((Chest) state).getInventory().getHolder()).getInventory();
		} catch (Exception e) {
			inv = ((InventoryHolder) state).getInventory();
		}
		return inv;
	}
	
	private boolean isEmpty(ItemStack item) {
		return Utils.isNull(item) || Utils.isSame(item,empty);
	}
	
	public boolean isOwner(@NotNull Player player) {
		return player.getUniqueId().equals(ownerID);
	}
	
	public boolean sameFrame(@NotNull Entity entity) {
		return (entity instanceof ItemFrame) && entity.getUniqueId().equals(this.frame.getUniqueId());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof ItemFrameShop)) return false;
		ItemFrameShop shop = (ItemFrameShop) obj;
		if (this == shop) return true;
		return sameFrame(shop.frame);
	}
	
	private boolean buy(@NotNull Player player, int amount) {
		String msg;
		if (isOpen() && testRemove(amount)) {
			EconomyResponse response = ItemFrameShopMain.getEconomyManager().playerPay(player, amount * this.price);
			if (response.type == ResponseType.SUCCESS) {
				for (int i = 0; i < amount; i++) Utils.givePlayer(player,getSellItem(),false);
				removeAmount(amount);
//				Utils.chatColors(player,"&fPurchased &6" + amount + " &fitems!"); // From config
				if ((msg = ItemFrameShopMain.messages().purchase(player,amount)) != null) player.sendMessage(msg);
				if (this.getType() != ShopType.ADMINSHOP)
					ItemFrameShopMain.getEconomyManager().playerAdd(Bukkit.getOfflinePlayer(getOwnerID()),amount * this.price);
				return true;
			} else player.sendMessage(response.errorMessage);
//		} else Utils.chatColors(player,"&cInsufficient amount of items in shop!"); // From config
		} else if ((msg = ItemFrameShopMain.messages().insufficientAmount(player,getAmount())) != null) player.sendMessage(msg);
		canBeOpen();
		return false;
	}
	
	private double minimum() {
		double min = ItemFrameShopMain.config().getDouble("price-minimum");
		if (min < 0) return 0;
		return min;
	}
	
	private double maximum() {
		double max = ItemFrameShopMain.config().getDouble("price-maximum");
		if (max == 0 || max <= minimum()) return Double.POSITIVE_INFINITY;
		return max;
	}
	
	// Events
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onItemFrameClickOff(PlayerInteractEntityEvent event) {
		if (!sameFrame(event.getRightClicked())) return;
		Player player = event.getPlayer();
		boolean sneak = player.isSneaking();
		boolean allow = (this.type != ShopType.ADMINSHOP && (isOwner(player) || Permissions.changeOtherShopPermission(player))) ||
				(this.type == ShopType.ADMINSHOP && Permissions.AdminShopPermission(player));
		if (!sneak && allow) return;
		boolean isCreate = event.isCancelled();
		event.setCancelled(true);
		if (allow) new EditMenu(player,isCreate);
		else if (sneak) new BuyMenu(player);
		else buy(player,1);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onItemFrameClickMain(EntityDamageEvent event) {
		if (!sameFrame(event.getEntity()) || !isShop()) return;
		event.setCancelled(true);
		if (!(event instanceof EntityDamageByEntityEvent)) return;
		if (!(((EntityDamageByEntityEvent) event).getDamager() instanceof Player)) return;
		Player player = (Player) ((EntityDamageByEntityEvent) event).getDamager();
		if (this.type.requireInventory()) return;
		boolean allow = (this.type != ShopType.ADMINSHOP && (isOwner(player) || Permissions.changeOtherShopPermission(player))) ||
				(this.type == ShopType.ADMINSHOP && Permissions.AdminShopPermission(player));
		if (!allow) return;
		ItemStack item = player.getInventory().getItemInMainHand();
		if (!Utils.isSame(item,this.sellItem)) return;
		boolean sneak = player.isSneaking();
		int old = getAmount();
		int change = addAmount(sneak ? item.getAmount() : 1) - old;
		if (change > 0 && player.getGameMode() != GameMode.CREATIVE) {
			int amount = item.getAmount() - change;
			if (amount > 0) item.setAmount(amount);
			else item = null;
			player.getInventory().setItemInMainHand(item);
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onItemFrameBreak(HangingBreakEvent event) {
		if (!(event.getEntity() instanceof ItemFrame) || !sameFrame(event.getEntity())) return;
		if (isShop()) event.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockPlace(BlockPlaceEvent event) {
		try {
			if (event.getBlock().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(ItemFrameShopMain.getInstance());
		} catch (Exception e) {}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
		boolean check = false;
		for (Block block : event.getBlocks())
			if (block.getLocation().equals(this.frame.getLocation().getBlock().getLocation())) event.setCancelled(true);
			else try {
				if (block.getLocation().distance(getInventory().getLocation()) <= 2) check = true;
			} catch (Exception e) {}
		if (check) new BukkitRunnable() {
			public void run() {
				canBeOpen();
			}
		}.runTask(ItemFrameShopMain.getInstance());
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
		boolean check = false;
		for (Block block : event.getBlocks())
			if (block.getLocation().equals(this.frame.getLocation().getBlock().getLocation())) event.setCancelled(true);
			else try {
				if (block.getLocation().distance(getInventory().getLocation()) <= 2) check = true;
			} catch (Exception e) {}
		if (check) new BukkitRunnable() {
			public void run() {
				canBeOpen();
			}
		}.runTask(ItemFrameShopMain.getInstance());
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockBurnEvent(BlockBurnEvent event) {
		try {
			if (event.getBlock().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(ItemFrameShopMain.getInstance());
		} catch (Exception e) {}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockExplodeEvent(BlockExplodeEvent event) {
		try {
			if (event.getBlock().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(ItemFrameShopMain.getInstance());
		} catch (Exception e) {}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockBreakEvent(BlockBreakEvent event) {
		try {
			if (event.getBlock().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(ItemFrameShopMain.getInstance());
		} catch (Exception e) {}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockInventoryCloseEvent(InventoryCloseEvent event) {
		try {
			if (event.getInventory().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(ItemFrameShopMain.getInstance());
		} catch (Exception e) {}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockMoveItemEvent(InventoryMoveItemEvent event) {
		try {
			if (event.getSource().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(ItemFrameShopMain.getInstance());
		} catch (Exception e) {}
	}
	
	private class EditMenu extends ListenerInventory {
		private final Player player;
		private double shopPrice;
		private ShopType shopType;
		private boolean awaitingInput = false;
		private final boolean isCreate;

		private static final int lines = 3;
		private final int slotItem = 4;
		private final int slotType = 2;
		private final int slotPrice = 6;
		private final int slotConfirm = lines * 9 - 1;
		private final int slotClose = lines * 9 - 5;
		private final int slotDelete = lines * 9 - 9;
		
		public EditMenu(@NotNull Player player, boolean isCreate) {
			super(Bukkit.createInventory(player,lines * 9,isCreate ? ItemFrameShopMain.messages().titleCreate(player) : ItemFrameShopMain.messages().titleEdit(player))); // From config
			this.player = player;
			this.isCreate = isCreate;
			this.shopPrice = getPrice();
			this.shopType = getType();
			for (int i = 0; i < lines * 9; i++) inventory.setItem(i,empty);
			ItemStack item = getSellItem();
			if (shopType != null) {
				Object name;
				if (item.getItemMeta().hasDisplayName()) name = ReflectionUtils.getNameItem(item);
				else name = ReflectionUtils.getItemTranslatable(item);
				item = ReflectionUtils.setNameItem(item,name);
			}
			inventory.setItem(slotItem,item);
			setPriceItem();
			setTypeItem();
			if (getPrice() >= 0) inventory.setItem(slotDelete,delete);
			inventory.setItem(slotClose,close);
			register();
			player.openInventory(inventory);
		}
		
		@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
		public void onMainInventoryClick(InventoryClickEvent event) {
			if (!event.getView().getTopInventory().equals(inventory)) return;
			event.setCancelled(true);
			int slot = event.getRawSlot();
			if (slot >= lines * 9 || slot < 0 || event.getClick() == ClickType.CREATIVE || isEmpty(inventory.getItem(slot))) return;
			if (!event.getClick().isRightClick() && !event.getClick().isLeftClick()) return;
			if (slot == slotClose) event.getView().close();
			else if (slot == slotDelete) {
				delete(player);
				event.getView().close();
			} else if (slot == slotPrice) {
				awaitingInput = true;
				event.getView().close();
				new PriceListener();
			} else if (slot == slotType) {
				ShopType next = null;
				int i = (shopType.ordinal() + 1) % ShopType.values().length;
				while (i != shopType.ordinal() && next == null) {
					ShopType type = ShopType.values()[i];
					if (Permissions.createShopPermission(player,type)) next = type;
					i = (i + 1) % ShopType.values().length;
				}
				if (next != null) {
					shopType = next;
					setTypeItem();
				}
			} else if (slot == slotConfirm) {
				boolean success;
				if (isCreate) success = createShop(player,shopType,shopPrice);
				else success = changeShop(player,shopType,shopPrice);
				event.getView().close();
				String msg;
				if (success) {
//					msg = "&aShop " + (isCreate ? "created" : "changed") + " successfully!"; // From config
					msg = isCreate ? ItemFrameShopMain.messages().createSuccess(player) : ItemFrameShopMain.messages().changeSuccess(player);
					canBeOpen();
//				} else msg = "&cProblem " + (isCreate ? "creating" : "changing") + " shop"; // From config
				} else msg = isCreate ? ItemFrameShopMain.messages().createError(player) : ItemFrameShopMain.messages().changeError(player);
				if (msg != null) player.sendMessage(msg);
			}
		}
		
		private void setPriceItem() {
			ItemStack item = new ItemStack(Material.EMERALD);
			ItemMeta meta = item.getItemMeta();
//			meta.setDisplayName(Utils.chatColors("&aPrice")); // From config
//			meta.setLore(Arrays.asList("",shopPrice >= 0 ? Utils.chatColors("&f") + ItemFrameShopMain.getEconomyManager().currency(shopPrice) : Utils.chatColors("&cNot set"),"",
//					Utils.chatColors("&8>&7> &eClick to change &7<&8<"))); // From config
			meta.setDisplayName(ItemFrameShopMain.messages().priceEditItemName(player,shopPrice));
			meta.setLore(ItemFrameShopMain.messages().priceEditItemLore(player,shopPrice));
			item.setItemMeta(meta);
			inventory.setItem(slotPrice,item);
			inventory.setItem(slotConfirm,shopPrice >= 0 ? ReflectionUtils.setNameItem(new ItemStack(Material.DIAMOND_BLOCK),
					ReflectionUtils.buildIChatBaseComponentString("selectWorld.edit.save",true,"green")) : empty);
		}
		
		private void setTypeItem() {
			ItemStack item;
			String name;
			if (shopType == ShopType.ADMINSHOP) {
				item = new ItemStack(Material.COMMAND_BLOCK);
//				name = ReflectionUtils.buildIChatBaseComponentString("Admin",false,"light_purple"); // From config
				name = ItemFrameShopMain.messages().typeAdmin(player,"light_purple");
			} else if (shopType == ShopType.ITEMFRAME) {
				item = new ItemStack(Material.ITEM_FRAME);
				name = ReflectionUtils.buildIChatBaseComponentString("item.minecraft.item_frame",true,"yellow");
			} else {
				item = new ItemStack(Material.CHEST);
				name = ReflectionUtils.buildIChatBaseComponentString("key.categories.inventory",true,"white");
			}
			item = ReflectionUtils.setNameItem(item,ReflectionUtils.buildIChatBaseComponentString("gui.entity_tooltip.type",true,false,"white",false,name));
			inventory.setItem(slotType,item);
			ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
			ItemMeta meta = border.getItemMeta();
			meta.setDisplayName(Utils.chatColors("&f" + getAmountStr(shopType,getAmount(shopType))));
			border.setItemMeta(meta);
			for (int i : Arrays.asList(-10,-9,-8,-1,1,8,9,10)) if (slotItem + i >= 0 && slotItem + i < lines * 9) inventory.setItem(slotItem + i,border);
		}
		
		@Override
		@EventHandler(ignoreCancelled = true)
		public void unregisterOnClose(InventoryCloseEvent event) {
			if (event.getView().getTopInventory().equals(inventory) && !awaitingInput) unregister();
		}
		
		private class PriceListener extends Listener {
			
			public PriceListener() {
				register();
//				Utils.chatColors(player,"&ePlease insert the price in the chat.\nThe price must be " + (Double.isInfinite(maximum()) ? "bigger than " + minimum() :
//					"between " + minimum() + " and " + maximum()) + ", 0 = closed\nType cancel to return"); // From config
				String msg = Double.isInfinite(maximum()) ? ItemFrameShopMain.messages().priceMessageNoMaximum(player,minimum()) :
						ItemFrameShopMain.messages().priceMessageWithMaximum(player,minimum(),maximum());
				if (msg != null) player.sendMessage(msg);
			}
			
			@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
			public void onInteract(PlayerInteractEvent event) {
				if (event.getPlayer().equals(player)) event.setCancelled(true);
			}
			
			@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
			public void onChatPrice(AsyncPlayerChatEvent event) {
				if (!event.getPlayer().equals(player)) return;
				event.setCancelled(true);
				try {
					String read = event.getMessage().trim();
//					if (!read.equalsIgnoreCase("cancel")) { // From config
					if (!read.equalsIgnoreCase(ItemFrameShopMain.messages().cancel(player))) {
						double suffixMult = 1;
//						if (read.toLowerCase().endsWith("k") || read.toLowerCase().endsWith("m") || read.toLowerCase().endsWith("b")) { // From config
//							String suffix = read.substring(read.length() - 1);
//							read = read.substring(0,read.length() - 1).toLowerCase();
//							if (suffix.equals("k")) suffixMult = 1e3;
//							else if (suffix.equals("m")) suffixMult = 1e6;
//							else suffixMult = 1e9;
//						}
						boolean exact = ItemFrameShopMain.messages().suffixesExact();
						List<String> suffixes = ItemFrameShopMain.messages().suffixes();
						if (suffixes != null) for (int i = 0; i < suffixes.size(); i++) {
							String suffix = suffixes.get(i);
							if (suffix == null) continue;
							if (exact) {
								if (!read.endsWith(suffix)) continue;
							} else if (!read.toLowerCase().endsWith(suffix.toLowerCase())) continue;
							read = read.substring(0,read.length() - suffix.length());
							suffixMult = Math.pow(1000,i + 1);
						}
						double readPrice = Double.parseDouble(read) * suffixMult;
						if (Double.isInfinite(readPrice)) throw new Exception();
						if (readPrice != 0 && (readPrice <= minimum() || readPrice > maximum())) throw new Exception();
						shopPrice = fixPrice(readPrice);
						setPriceItem();
					}
					awaitingInput = false;
					unregister();
					new BukkitRunnable() {
						public void run() {
							player.openInventory(inventory);
						}
					}.runTask(ItemFrameShopMain.getInstance());
				} catch (Exception e) {
//					Utils.chatColors(player,"&cOnly numbers " + (Double.isInfinite(maximum()) ? "bigger than " + minimum() :
//						"between " + minimum() + " and " + maximum()) + " or cancel! 0 = closed"); // From config
					String msg = Double.isInfinite(maximum()) ? ItemFrameShopMain.messages().priceErrorNoMaximum(player,minimum()) :
							ItemFrameShopMain.messages().priceErrorWithMaximum(player,minimum(),maximum());
					if (msg != null) player.sendMessage(msg);
				}
			}
			
			@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
			public void unregisterOnLeaveEvent(PlayerQuitEvent event) {
				if (event.getPlayer().getUniqueId().equals(player.getUniqueId())) unregister();
			}
		}
	}
	
	private class BuyMenu extends ListenerInventory {
		private final Player player;

		private static final int lines = 4;
		private final int slotItem = 4;
		private final int slotOption1 = 20;
		private final int slotOption2 = 21;
		private final int slotOption3 = 22;
		private final int slotOption4 = 23;
		private final int slotOption5 = 24;
		private final int slotClose = lines * 9 - 5;
		
		public BuyMenu(@NotNull Player player) {
//			super(Bukkit.createInventory(Objects.requireNonNull(player),lines * 9,Utils.chatColors("&6Shop"))); // From config
			super(Bukkit.createInventory(Objects.requireNonNull(player),lines * 9,ItemFrameShopMain.messages().titleBuy(player)));
			this.player = player;
			for (int i = 0; i < lines * 9; i++) inventory.setItem(i,empty);
			inventory.setItem(slotItem,getSellItem());
			setOptions();
			inventory.setItem(slotClose,close);
			register();
			player.openInventory(inventory);
		}
		
		@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
		public void onMainInventoryClick(InventoryClickEvent event) {
			if (!event.getView().getTopInventory().equals(inventory)) return;
			event.setCancelled(true);
			int slot = event.getRawSlot();
			if (slot >= lines * 9 || slot < 0 || event.getClick() == ClickType.CREATIVE || isEmpty(inventory.getItem(slot))) return;
			if (!event.getClick().isRightClick() && !event.getClick().isLeftClick()) return;
			if (slot == slotClose) event.getView().close();
			else if (slot == slotOption1 || slot == slotOption2 || slot == slotOption3 || slot == slotOption4 || slot == slotOption5) {
				if (slot == slotOption1) buy(player,optionAmount(1));
				else if (slot == slotOption2) buy(player,optionAmount(2));
				else if (slot == slotOption3) buy(player,optionAmount(3));
				else if (slot == slotOption4) buy(player,optionAmount(4));
				else buy(player,optionAmount(5));
				if (isOpen()) setOptions();
				else event.getView().close();
			}
		}
		
		private void setOptions() {
			inventory.setItem(slotOption1,optionItem(1));
			inventory.setItem(slotOption2,optionItem(2));
			inventory.setItem(slotOption3,optionItem(3));
			inventory.setItem(slotOption4,optionItem(4));
			inventory.setItem(slotOption5,optionItem(5));
			ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
			ItemMeta meta = border.getItemMeta();
//			meta.setDisplayName(Utils.chatColors("&f" + getAmountStr())); // From config
			meta.setDisplayName(ItemFrameShopMain.messages().priceBorderName(player,getAmountStr()));
			meta.setLore(ItemFrameShopMain.messages().priceBorderLore(player,getAmountStr()));
			border.setItemMeta(meta);
			for (int i : Arrays.asList(-10,-9,-8,-1,1,8,9,10)) if (slotItem + i >= 0 && slotItem + i < lines * 9) inventory.setItem(slotItem + i,border);
		}
		
		private ItemStack optionItem(int i) {
			int amount = optionAmount(i);
			if (type != ShopType.ADMINSHOP && amount > getAmount()) return empty.clone();
			ItemStack item = new ItemStack(Material.EMERALD);
			ItemMeta meta = item.getItemMeta();
//			meta.setDisplayName(Utils.chatColors("&f") + ItemFrameShopMain.getEconomyManager().currency(amount * getPrice())); // From config
			meta.setDisplayName(ItemFrameShopMain.messages().priceBuyItemName(player,amount,getPrice()));
			meta.setLore(ItemFrameShopMain.messages().priceBuyItemLore(player,amount,getPrice()));
			item.setItemMeta(meta);
			return Utils.asQuantity(item,amount,true);
		}
		
		private int optionAmount(int i) {
			int stack = getSellItem().getMaxStackSize();
			int amount = 0;
			if (i == 1) amount = 1;
			else if (i == 2) amount = 2;
			else if (i == 3) amount = stack == 1 ? 3 : stack / 4;
			else if (i == 4) amount = stack == 1 ? 4 : stack / 2;
			else if (i == 5) amount = stack == 1 ? 5 : stack;
			if (type != ShopType.ADMINSHOP && amount > getAmount()) return 0;
			return amount;
		}
	}
}