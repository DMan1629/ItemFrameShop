package me.DMan16.ItemFrameShop.Shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.DMan16.ItemFrameShop.Main;
import me.DMan16.ItemFrameShop.Utils.PacketUtils;
import me.DMan16.ItemFrameShop.Utils.ReflectionUtils;
import me.DMan16.ItemFrameShop.Utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.Map.Entry;

public class ItemFrameShopManager {
	private final double yNameDisplay = 0.5;
	private final double yPriceDisplay = 0.2;
	private final double yFaceDown = -0.5;
	
	private final HashMap<String,List<ItemFrameShop>> ItemFrameShops = new HashMap<String,List<ItemFrameShop>>();
	private final List<ItemFrame> ItemFrameShopsFrames = new ArrayList<ItemFrame>();
	private final String pluginDir = "plugins/" + Main.pluginName;
	private final Path dir = Paths.get(pluginDir);
	
	public ItemFrameShopManager() throws IOException {
		if (!Files.exists(dir, new LinkOption[0])) Files.createDirectories(dir, new FileAttribute[0]);
		else for (World world : Bukkit.getWorlds()) loadInfoFromPath(world);
	}
	
	public void loadInfoFromPath(World world) {
		if (world == null) return;
		File path = null;
		for (final File file : (new File(pluginDir)).listFiles()) if (file.isDirectory() && file.getName().equals(world.getName())) {
			path = file;
			break;
		}
		if (path == null) return;
		List<File> remove = new ArrayList<File>();
		JSONParser jsonParser = new JSONParser();
		for (final File file : path.listFiles()) {
			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(pluginDir + "/" + path.getName() + "/" + file.getName()),StandardCharsets.UTF_8)) {
				Object obj = jsonParser.parse(reader);
				JSONArray arr = (JSONArray) obj;
				int count = 0;
				for (Object info : arr) {
					try {
						double x = Double.parseDouble(((JSONObject) info).get("x").toString());
						double y = Double.parseDouble(((JSONObject) info).get("y").toString());
						double z = Double.parseDouble(((JSONObject) info).get("z").toString());
						ShopType type = ShopType.getType(((JSONObject) info).get("type").toString());
						ItemStack item = (ItemStack) Utils.ObjectFromBase64(((JSONObject) info).get("item").toString());
						double price = Double.parseDouble(((JSONObject) info).get("price").toString());
						int amount = 0;
						try {
							amount = Integer.parseInt(((JSONObject) info).get("amount").toString());
						} catch (Exception e) {};
						Location location = natLocation(world,x,y,z);
						ItemFrame frame = null;
						for (Entity entity : world.getChunkAt(location).getEntities())
							if (entity instanceof ItemFrame && entity.getLocation().distance(location) <= 0.01) frame = (ItemFrame) entity;
						ItemFrameShop shop = new ItemFrameShop(frame,UUID.fromString(file.getName().split("\\.")[0]),type,item,price,amount,true);
						if (shop.isShop()) {
							count++;
							newShop(shop);
						}
					} catch (Exception e) {}
				}
				if (count == 0) remove.add(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for (File file : remove) file.delete();
	}
	
	public void unload(World world) {
		write(world);
		if (ItemFrameShops.containsKey(world.getName())) {
			List<ItemFrameShop> shops = new ArrayList<ItemFrameShop>(ItemFrameShops.get(world.getName()));
			for (ItemFrameShop shop : shops) remove(shop,null);
		}
	}
	
	public void newShop(ItemFrameShop shop) {
		if (!Objects.requireNonNull(shop).isShop()) return;
		ItemFrame frame = shop.getFrame();
		if (isShop(frame)) return;
		World world = frame.getWorld();
		if (!ItemFrameShops.containsKey(world.getName())) ItemFrameShops.put(world.getName(), new ArrayList<ItemFrameShop>());
		ItemFrameShops.get(world.getName()).add(shop);
		ItemFrameShopsFrames.add(frame);
	}
	
	public ItemFrameShop getShop(ItemFrame frame) {
		if (!ItemFrameShopsFrames.contains(Objects.requireNonNull(frame)) || !ItemFrameShops.containsKey(frame.getWorld().getName())) return null;
		for (ItemFrameShop shop : ItemFrameShops.get(frame.getWorld().getName())) if (shop.getFrame().equals(frame)) return shop;
		return null;
	}
	
	public boolean isShop(ItemFrame frame) {
		return getShop(Objects.requireNonNull(frame)) != null;
	}
	
	@SuppressWarnings("unchecked")
	public void write(World world) {
		String worldName = world.getName();
		if (!ItemFrameShops.containsKey(worldName)) return;
		Path path = dir.resolve(worldName);
		try {
			if (!Files.exists(path)) Files.createDirectories(path);
			Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
			JsonParser jp = new JsonParser();
			List<ItemFrameShop> remove = new ArrayList<ItemFrameShop>();
			HashMap<UUID,JSONArray> ItemFrameShopsPlayer = new HashMap<UUID,JSONArray>();
			for (ItemFrameShop shop : ItemFrameShops.get(worldName)) {
				if (!shop.isShop()) {
					remove.add(shop);
					continue;
				}
				JSONArray arr;
				if (ItemFrameShopsPlayer.containsKey(shop.getOwnerID())) arr = ItemFrameShopsPlayer.get(shop.getOwnerID());
				else arr = new JSONArray();
				JSONObject entry = new JSONObject();
				ItemFrame frame = shop.getFrame();
				Location loc = natLocation(frame.getLocation());
				entry.put("x",loc.getX());
				entry.put("y",loc.getY());
				entry.put("z",loc.getZ());
				entry.put("type",shop.getType().name);
				entry.put("item",Utils.ObjectToBase64(shop.getSellItem()));
				entry.put("price",shop.getPrice());
				int amount = shop.getAmount();
				if (amount > 0) entry.put("amount",shop.getAmount());
				arr.add(entry);
				ItemFrameShopsPlayer.put(shop.getOwnerID(),arr);
			}
			for (Entry<UUID,JSONArray> playerShops : ItemFrameShopsPlayer.entrySet()) {
				Path playerPath = path.resolve(playerShops.getKey() + ".json");
				Files.deleteIfExists(playerPath);
				if (!playerShops.getValue().isEmpty()) {
					Files.createFile(playerPath);
					JsonElement je = jp.parse(playerShops.getValue().toJSONString());
					String prettyJsonString = gson.toJson(je);
					PrintWriter pw = new PrintWriter(pluginDir + "/" + worldName + "/" + playerShops.getKey() + ".json");
					pw.write(prettyJsonString);
					pw.flush();
					pw.close();
				}
			}
			for (ItemFrameShop shop : remove) remove(shop,null);
		} catch (Exception e) {
			Utils.chatColorsLogPlugin("&cError saving shops in world &e" + world.getName() + "&c!");
		}
		if (Bukkit.getWorlds().get(Bukkit.getWorlds().size() - 1).getName().equals(worldName))
			Utils.chatColorsLogPlugin("&aAll shops have been saved!");
	}
	
	public String toString() {
		List<String> worldStrs = new ArrayList<String>();
		Bukkit.broadcastMessage("size: " + ItemFrameShops.entrySet().size());
		for (Entry<String,List<ItemFrameShop>> shops : ItemFrameShops.entrySet()) {
			List<String> worldStr = new ArrayList<String>();
			for (ItemFrameShop shop : shops.getValue()) {
				String str = "&6(&c&6) ";
				Location loc = natLocation(shop.getFrame().getLocation());
				str += "&f(" + loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + ")";
				str += " ";
				str += "&ftype: &e" + shop.getType().name;
				str += "&f, ";
				str += "owner ID: &b" + shop.getOwnerID();
				str += "&f, ";
				str += "price: &a" + shop.getPrice();
				str += "&f, ";
				str += "open: &d" + shop.isOpen();
				str += "&f, ";
				str += "amount: &9" + shop.getAmountStr();
				
				worldStr.add(str);
			}
			worldStrs.add("&b&n&l" + shops.getKey() + ":&r\n" + String.join("\n",worldStr));
		}
		return Utils.chatColors(String.join("\n",worldStrs));
	}
	
	private void spawnShopArmorStands(Object name, int ID, Location loc, double yOffset) {
		Object DataWatcher = PacketUtils.packetDataWatcherArmorStand(name);
		Location location = loc.clone().add(0,yOffset,0);
		for (Player player : loc.getWorld().getPlayers()) {
			PacketUtils.sendPacket(PacketUtils.packetCreateArmorStand(ID,location,DataWatcher),player);
			if (Utils.getVersionInt() > 14) PacketUtils.sendPacket(PacketUtils.packetUpdateArmorStand(ID,DataWatcher),player);
		}
	}
	
	public void spawnShopArmorStands(ItemFrameShop shop) {
		if (shop == null || !shop.isShop() || !shop.isOpen()) return;
		Object nameStr;
		if (shop.getSellItem().getItemMeta().hasDisplayName()) nameStr = ReflectionUtils.getNameItem(shop.getSellItem());
		else nameStr = ReflectionUtils.getItemTranslateable(shop.getSellItem());
		Object priceStr = ReflectionUtils.ChatColorsToIChatBaseComponent(Main.EconomyManager.currency(shop.getPrice()));
		int nameID = shop.getNameDisplay();
		int priceID = shop.getPriceDisplay();
		Location loc = natLocation(shop.getFrame().getLocation());
		double nameOffset = shop.getFrame().getAttachedFace() == BlockFace.UP ? yFaceDown - yPriceDisplay : yNameDisplay;
		double priceOffset = shop.getFrame().getAttachedFace() == BlockFace.UP ? yFaceDown - yNameDisplay : yPriceDisplay;
		spawnShopArmorStands(nameStr,nameID,loc,nameOffset);
		spawnShopArmorStands(priceStr,priceID,loc,priceOffset);
	}
	
	private void spawnShopArmorStands(Object name, int ID, Location loc, double yOffset, Player player) {
		Object DataWatcher = PacketUtils.packetDataWatcherArmorStand(name);
		Location location = loc.clone().add(0,yOffset,0);
		PacketUtils.sendPacket(PacketUtils.packetCreateArmorStand(ID,location,DataWatcher),player);
		if (Utils.getVersionInt() > 14) PacketUtils.sendPacket(PacketUtils.packetUpdateArmorStand(ID,DataWatcher),player);
	}
	
	public void spawnShopArmorStands(Player player) {
		if (player == null || !ItemFrameShops.containsKey(player.getWorld().getName())) return;
		for (ItemFrameShop shop : ItemFrameShops.get(player.getWorld().getName())) {
			if (!shop.isOpen()) continue;
			Object nameStr;
			if (shop.getSellItem().getItemMeta().hasDisplayName()) nameStr = ReflectionUtils.getNameItem(shop.getSellItem());
			else nameStr = ReflectionUtils.getItemTranslateable(shop.getSellItem());
			Object priceStr = ReflectionUtils.ChatColorsToIChatBaseComponent(Main.EconomyManager.currency(shop.getPrice()));
			int nameID = shop.getNameDisplay();
			int priceID = shop.getPriceDisplay();
			Location loc = natLocation(shop.getFrame().getLocation());
			double nameOffset = shop.getFrame().getAttachedFace() == BlockFace.UP ? yFaceDown - yPriceDisplay : yNameDisplay;
			double priceOffset = shop.getFrame().getAttachedFace() == BlockFace.UP ? yFaceDown - yNameDisplay : yPriceDisplay;
			spawnShopArmorStands(nameStr,nameID,loc,nameOffset,player);
			spawnShopArmorStands(priceStr,priceID,loc,priceOffset,player);
		}
	}
	
	public void despawnShopArmorStands(ItemFrameShop shop) {
		if (shop == null || shop.isOpen()) return;
		int nameID = shop.getNameDisplay();
		int priceID = shop.getPriceDisplay();
		for (Player player : shop.getFrame().getWorld().getPlayers()) PacketUtils.sendPacket(PacketUtils.packetDestroyEntity(nameID,priceID),player);
	}
	
	private Location natLocation(Location loc) {
		return natLocation(loc.getWorld(),loc.getX(),loc.getY(),loc.getZ());
	}
	
	private Location natLocation(World world, double x, double y, double z) {
		return new Location(world,x,y,z);
	}
	
	void remove(ItemFrameShop shop, Player player) {
		if (shop == null) return;
		if (shop.isShop()) shop.delete(player);
		else {
			String world = shop.getFrame().getWorld().getName();
			try {
				ItemFrameShopsFrames.remove(shop.getFrame());
			} catch (Exception e) {}
			try {
				ItemFrameShops.get(world).remove(shop);
			} catch (Exception e) {}
			if (ItemFrameShops.get(world).isEmpty()) ItemFrameShops.remove(world);
		}
	}
}