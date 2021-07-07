package me.DMan16.ItemFrameShop.Shop;

import me.DMan16.ItemFrameShop.Events.ItemFrameShopChangeEvent;
import me.DMan16.ItemFrameShop.Events.ItemFrameShopCreateEvent;
import me.DMan16.ItemFrameShop.Events.ItemFrameShopDeleteEvent;
import me.DMan16.ItemFrameShop.Main;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class ItemFrameShop extends Listener {
	private static ItemStack empty = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
	private static ItemStack close;
	private static ItemStack delete;
	
	static {
		ItemMeta meta = empty.getItemMeta();
		meta.setDisplayName(" ");
		empty.setItemMeta(meta);
		close = ReflectionUtils.setNameItem(new ItemStack(Material.BARRIER),ReflectionUtils.buildIChatBaseComponentString("spectatorMenu.close",true,"red"));
		delete = ReflectionUtils.setNameItem(new ItemStack(Material.BARRIER),ReflectionUtils.buildIChatBaseComponentString("selectWorld.delete",true,"dark_red"));
	}
	
	private ItemFrame frame;
	private UUID ownerID = null;
	private ShopType type = null;
	private ItemStack sellItem = null;
	private int nameDisplay = 0;
	private int priceDisplay = 0;
	private double price;
	private boolean open = false;
	private int amount;
	
	private void setInfo(UUID ownerID, ShopType type, ItemStack sellItem, int nameDisplay, int priceDisplay, double price, int amount) {
		this.sellItem = Objects.requireNonNull(Utils.asQuantity(sellItem,1));
		this.ownerID = Objects.requireNonNull(ownerID);
		this.type = Objects.requireNonNull(type);
		this.price = fixPrice(price);
		this.nameDisplay = nameDisplay;
		this.priceDisplay = priceDisplay;
		this.amount = setAmount(amount);
	}
	
	public ItemFrameShop(ItemFrame frame, UUID ownerID, ItemStack sellItem) {
		this.frame = Objects.requireNonNull(frame);
		ItemFrameShop shop = Main.ItemFrameShopManager.getShop(frame);
		if (shop != null) setInfo(shop.ownerID,shop.type,shop.sellItem,shop.nameDisplay,shop.priceDisplay,shop.price,shop.amount);
		else setInfo(ownerID,ShopType.INVENTORY,sellItem,ReflectionUtils.getNextEntityID(),ReflectionUtils.getNextEntityID(),-1,0);
	}
	
	public ItemFrameShop(ItemFrame frame, UUID ownerID, ShopType type, ItemStack sellItem, double price, int amount, boolean fromFile) {
		this.frame = Objects.requireNonNull(frame);
		ItemFrameShop shop = fromFile ? null : Main.ItemFrameShopManager.getShop(frame);
		if (shop != null) setInfo(shop.ownerID,shop.type,shop.sellItem,shop.nameDisplay,shop.priceDisplay,shop.price,shop.amount);
		else setInfo(ownerID,type,sellItem,ReflectionUtils.getNextEntityID(),ReflectionUtils.getNextEntityID(),price,amount);
		if (isShop()) {
			this.register();
			this.frame.setItem(ReflectionUtils.setNameItem(getSellItem(),null));
			if (!fromFile) Main.ItemFrameShopManager.newShop(this);
		}
	}
	
	public boolean isShop() {
		return frame != null && !frame.isDead() && ownerID != null && type != null && sellItem != null && nameDisplay > 0 && priceDisplay > 0;
	}
	
	public boolean createShop(Player owner, ShopType type, double price) {
		ItemFrameShopCreateEvent event = new ItemFrameShopCreateEvent(this,owner,type,sellItem,fixPrice(price));
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) return false;
		setInfo(event.getOwner().getUniqueId(),event.getType(),sellItem,ReflectionUtils.getNextEntityID(),ReflectionUtils.getNextEntityID(),event.getPrice(),0);
		if (!this.isRegistered) this.register();
		this.frame.setItem(ReflectionUtils.setNameItem(getSellItem(),null));
		Main.ItemFrameShopManager.newShop(this);
		return true;
	}
	
	public boolean changeShop(@Nullable Player player, ShopType newType, double newPrice) {
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
		this.open = false;
		Main.ItemFrameShopManager.despawnShopArmorStands(this);
		this.ownerID = null;
		if (this.type.isIndependent() && this.type != ShopType.ADMINSHOP) for (int i = 0; i < this.amount; i++) {
			if (player == null) Utils.dropItem(this.frame.getLocation(),getSellItem());
			else Utils.givePlayer(player,getSellItem(),false);
		}
		this.frame.setItem(null);
		this.type = null;
		this.sellItem = null;
		setPrice(0);
		this.amount = 0;
		this.nameDisplay = 0;
		this.priceDisplay = 0;
		this.unregister();
		Main.ItemFrameShopManager.remove(this,player);
		return true;
	}
	
	public ItemFrame getFrame() {
		return frame;
	}
	
	public UUID getOwnerID() {
		return ownerID;
	}
	
	public ShopType getType() {
		return type;
	}
	
	public ItemStack getSellItem() {
		return Utils.asQuantity(sellItem,1);
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
		return Utils.roundAfterDot(price,Math.min(Math.max(Main.config().getInt("price-round-after-dot"),0),2));
	}
	
	public int getAmount() {
		return getAmount(this.type);
	}
	
	public String getAmountStr() {
		return getAmountStr(this.type,getAmount());
	}
	
	private static String getAmountStr(ShopType type, int amount) {
		return type == ShopType.ADMINSHOP ? "∞" : "" + amount;
	}
	
	private int getAmount(ShopType type) {
		int amount = 0;
		if (type == null || type.isIndependent()) amount = /*type == ShopType.ADMINSHOP ? Integer.MAX_VALUE : */this.amount;
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
			Main.ItemFrameShopManager.spawnShopArmorStands(this);
		} else {
			if (!isOpen()) return;
			this.open = false;
			Main.ItemFrameShopManager.despawnShopArmorStands(this);
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
//		if (amount <= 0) return old;
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
			for (int i = 0; i < inv.getSize(); i++) {
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
		if (amount > getAmount() || (this.type.requireInventory() && getInventory() == null)) return false;
		return true;
	}
	
	public Block getAttachedBlock() {
		return frame.getLocation().getBlock().getRelative(frame.getAttachedFace());
	}
	
	public BlockState getAttachedBlockState() {
		Block block = getAttachedBlock();
		if (block == null || block.isEmpty()) return null;
		return block.getState();
	}
	
	public Inventory getInventory() {
		if (this.type == null || this.type.isIndependent()) return null;
		BlockState state = getAttachedBlockState();
		if (state == null || !(state instanceof InventoryHolder)) return null;
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
	
	public boolean isOwner(Player player) {
		if (player == null) return false;
		return player.getUniqueId().equals(ownerID);
	}
	
	public boolean sameFrame(Entity entity) {
		return entity != null && (entity instanceof ItemFrame) && entity.getUniqueId().equals(this.frame.getUniqueId());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof ItemFrameShop)) return false;
		ItemFrameShop shop = (ItemFrameShop) obj;
		if (this == shop) return true;
		return sameFrame(shop.frame);
	}
	
	private boolean buy(Player player, int amount) {
		if (isOpen() && testRemove(amount)) {
			EconomyResponse response = Main.EconomyManager.playerPay(player,amount * this.price);
			if (response.type == ResponseType.SUCCESS) {
				for (int i = 0 ; i < amount; i++) Utils.givePlayer(player,getSellItem(),false);
				removeAmount(amount);
				Utils.chatColors(player,"&fPurchased &6" + amount + " &fitems!"); // From config
				return true;
			} else player.sendMessage(response.errorMessage);
		} else Utils.chatColors(player,"&cInsufficient amount of items in shop!"); // From config
		canBeOpen();
		return false;
	}
	
	private double minimum() {
		double min = Main.config().getDouble("price-minimum");
		if (min < 0) return 0;
		return min;
	}
	
	private double maximum() {
		double max = Main.config().getDouble("price-maximum");
		if (max == 0 || max <= minimum()) return Double.POSITIVE_INFINITY;
		return max;
	}
	
	//Events
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onItemFrameClickOff(PlayerInteractEntityEvent event) {
		if (!sameFrame(event.getRightClicked())) return;
		Player player = event.getPlayer();
		if (player == null) {
			event.setCancelled(true);
			return;
		}
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
		if (event.isCancelled() || !sameFrame(event.getEntity()) || !isShop()) return;
		event.setCancelled(true);
		if (!(event instanceof EntityDamageByEntityEvent)) return;
		if (!(((EntityDamageByEntityEvent) event).getDamager() instanceof Player)) return;
		Player player = (Player) ((EntityDamageByEntityEvent) event).getDamager();
		if (player == null || this.type.requireInventory()) return;
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
		if (event.isCancelled() || !(event.getEntity() instanceof ItemFrame) || !sameFrame(event.getEntity())) return;
		if (isShop()) event.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) return;
		try {
			if (event.getBlock().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(Main.getInstance());
		} catch (Exception e) {}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
		if (event.isCancelled()) return;
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
		}.runTask(Main.getInstance());
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
		if (event.isCancelled()) return;
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
		}.runTask(Main.getInstance());
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockBurnEvent(BlockBurnEvent event) {
		if (event.isCancelled()) return;
		try {
			if (event.getBlock().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(Main.getInstance());
		} catch (Exception e) {}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockExplodeEvent(BlockExplodeEvent event) {
		if (event.isCancelled()) return;
		try {
			if (event.getBlock().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(Main.getInstance());
		} catch (Exception e) {}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		try {
			if (event.getBlock().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(Main.getInstance());
		} catch (Exception e) {}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockInventoryCloseEvent(InventoryCloseEvent event) {
		try {
			if (event.getInventory().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(Main.getInstance());
		} catch (Exception e) {}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAttachedBlockMoveItemEvent(InventoryMoveItemEvent event) {
		try {
			if (event.getSource().getLocation().distance(getInventory().getLocation()) <= 2) new BukkitRunnable() {
				public void run() {
					canBeOpen();
				}
			}.runTask(Main.getInstance());
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
		
		public EditMenu(Player player, boolean isCreate) {
			super(Bukkit.createInventory(Objects.requireNonNull(player),lines * 9,Utils.chatColors("&b" + (isCreate ? "Create" : "Edit") + " &6Shop")));
			this.player = player;
			this.isCreate = isCreate;
			this.shopPrice = getPrice();
			this.shopType = getType();
			for (int i = 0; i < lines * 9; i++) inventory.setItem(i,empty);
			ItemStack item = getSellItem();
			if (shopType != null /*&& shopType.isIndependent()*/) {
				Object name = null;
				if (item.getItemMeta().hasDisplayName()) name = ReflectionUtils.getNameItem(item);
				else name = ReflectionUtils.getItemTranslatable(item);
				/*name = ReflectionUtils.buildIChatBaseComponentStringExtra(name,ReflectionUtils.buildIChatBaseComponentString(" (" +
						(shopType == ShopType.ADMINSHOP ? "∞" : getAmount(shopType)) + ")",false,"white"));*/
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
					msg = "&aShop " + (isCreate ? "created" : "changed") + " successfully!";
					canBeOpen();
				} else msg = "&cProblem " + (isCreate ? "creating" : "changing") + " shop";
				Utils.chatColors(player,msg);
			}
		}
		
		private void setPriceItem() {
			ItemStack item = new ItemStack(Material.EMERALD);
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(Utils.chatColors("&aPrice"));
			meta.setLore(Arrays.asList("",shopPrice >= 0 ? Utils.chatColors("&f") + Main.EconomyManager.currency(shopPrice) : Utils.chatColors("&cNot set"),"",
					Utils.chatColors("&8>&7> &eClick to change &7<&8<"))); // From config
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
				name = ReflectionUtils.buildIChatBaseComponentString("Admin",false,"light_purple");
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
				Utils.chatColors(player,"&ePlease insert the price in the chat.\nThe price must be " + (Double.isInfinite(maximum()) ? "bigger than " + minimum() :
					"between " + minimum() + " and " + maximum()) + ", 0 = closed\nType cancel to return"); // From config
			}
			
			@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
			public void onChatPrice(AsyncPlayerChatEvent event) {
				if (!event.getPlayer().equals(player)) return;
				event.setCancelled(true);
				try {
					String read = event.getMessage().trim();
					if (read.equalsIgnoreCase("cancel"));
					else {
						double readPrice = Double.parseDouble(read);
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
					}.runTask(Main.getInstance());
				} catch (Exception e) {
					Utils.chatColors(player,"&cOnly numbers " + (Double.isInfinite(maximum()) ? "bigger than " + minimum() :
						"between " + minimum() + " and " + maximum()) + " or cancel! 0 = closed"); // From config
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
		
		public BuyMenu(Player player) {
			super(Bukkit.createInventory(Objects.requireNonNull(player),lines * 9,Utils.chatColors("&6Shop")));
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
				else if (slot == slotOption5) buy(player,optionAmount(5));
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
			meta.setDisplayName(Utils.chatColors("&f" + getAmountStr()));
			border.setItemMeta(meta);
			for (int i : Arrays.asList(-10,-9,-8,-1,1,8,9,10)) if (slotItem + i >= 0 && slotItem + i < lines * 9) inventory.setItem(slotItem + i,border);
		}
		
		private ItemStack optionItem(int i) {
			int amount = optionAmount(i);
			if (type != ShopType.ADMINSHOP && amount > getAmount()) return empty.clone();
			ItemStack item = new ItemStack(Material.EMERALD);
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(Utils.chatColors("&f") + Main.EconomyManager.currency(amount * getPrice()));
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