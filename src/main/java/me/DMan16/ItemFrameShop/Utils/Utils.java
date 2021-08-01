package me.DMan16.ItemFrameShop.Utils;

import me.DMan16.ItemFrameShop.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	
	public static String chatColors(String str) {
		str = chatColorsStrip(str);
		return ChatColor.translateAlternateColorCodes('&',chatColorsHex(str));
	}
	
	public static String chatColorsHex(String str) {
		Pattern unicode = Pattern.compile("\\\\u\\+[a-fA-F0-9]{4}");
		Matcher match = unicode.matcher(str);
		while (match.find()) {
			String code = str.substring(match.start(),match.end());
			str = str.replace(code,Character.toString((char) Integer.parseInt(code.replace("\\u+",""),16)));
			match = unicode.matcher(str);
		}
		if (getVersionInt() < 16) return str;
		Pattern pattern = Pattern.compile("&#[a-fA-F0-9]{6}");
		match = pattern.matcher(str);
		while (match.find()) {
			String color = str.substring(match.start(),match.end());
			str = str.replace(color,ChatColor.of(color.replace("&","")) + "");
			match = pattern.matcher(str);
		}
		return str;
	}
	
	public static List<String> chatColors(List<String> list) {
		List<String> newList = new ArrayList<String>();
		for (String str : list) if (str != null)
			if (str.trim().isEmpty()) newList.add("");
			else newList.add(chatColors(str));
		return newList;
	}
	
	public static void chatColors(CommandSender sender, String str) {
		sender.sendMessage(chatColors(str));
	}
	
	public static void chatColorsLogPlugin(String str) {
		str = chatColorsPlugin(str);
		if (getVersionInt() < 16) str = chatColorsStrip(str);
		Bukkit.getLogger().info(str);
	}
	
	public static String chatColorsPlugin(String str) {
		return chatColors("&d[" + Main.pluginNameColors + "&d]&r " + str);
	}

	public static void chatColorsPlugin(CommandSender sender, String str) {
		sender.sendMessage(chatColorsPlugin(str));
	}
	
	public static void chatColorsActionBar(Player player, String str) {
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Utils.chatColors(str)));
	}
	
	public static String chatColorsStrip(String str) {
		return ChatColor.stripColor(str);
	}
	
	public static NamespacedKey namespacedKey(String name) {
		return new NamespacedKey(Main.getInstance(),name);
	}
	
	public static ItemStack asQuantity(ItemStack item, int amount) {
		return asQuantity(item,amount,false);
	}
	
	public static ItemStack asQuantity(ItemStack item, int amount, boolean allowIllegal) {
		if (isNull(item)) return null;
		ItemStack clone = item.clone();
		int newAmount = Math.max(0,amount);
		if (!allowIllegal) newAmount = Math.min(newAmount,clone.getMaxStackSize());
		if (amount == 0) return null;
		clone.setAmount(newAmount);
		return clone;
	}
	
	public static boolean isSame(ItemStack item1, ItemStack item2) {
		if (item1 == null || item2 == null) return item1 == item2;
		if (item1 == item2 || item1.equals(item2)) return true;
		ItemStack cmp1 = asQuantity(item1,1);
		ItemStack cmp2 = asQuantity(item2,1);
		return cmp1 == cmp2 || cmp1.equals(cmp2);
	}
	
	public static boolean isNull(@Nullable ItemStack item) {
		if (item == null || item.getAmount() <= 0) return true;
		if (getVersionInt() < 15) return item.getType().name().endsWith("AIR");
		return item.getType().isAir();
	}
	
	public static boolean givePlayer(Player player, ItemStack item, @Nullable Location drop, boolean glow) {
		if (player == null || isNull(item)) return false;
		if (!player.isDead() && player.getInventory().addItem(item).isEmpty()) return true;
		if (drop != null) {
			Item droppedItem = dropItem(drop,item);
			droppedItem.setGlowing(glow);
		}
		return false;
	}
	
	public static Item dropItem(Location drop, ItemStack item) {
		if (drop == null || item == null) return null;
		return drop.getWorld().dropItemNaturally(drop,item);
	}
	
	public static boolean givePlayer(Player player, ItemStack item, boolean glow) {
		if (player == null) return false;
		return givePlayer(player,item,player.getLocation(),glow);
	}
	
	public static String getVersion() {
		return Bukkit.getServer().getVersion().split("\\(MC:")[1].split("\\)")[0].trim().split(" ")[0].trim();
	}
	
	public static int getVersionMain() {
		return Integer.parseInt(getVersion().split("\\.")[0]);
	}
	
	public static int getVersionInt() {
		return Integer.parseInt(getVersion().split("\\.")[1]);
	}
	
	/**
	 * @param digitsAfterDot >= 0
	 */
	public static double roundAfterDot(double num, int digitsAfterDot) {
		if (digitsAfterDot < 0) return num;
		if (digitsAfterDot == 0) return Math.round(num);
		String format = "0.";
		for (int i = 0; i < digitsAfterDot; i++) format += "0";
		return Double.parseDouble((new DecimalFormat(format)).format(num));
	}
	
	public static String ObjectToBase64(Object obj) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
			dataOutput.writeInt(1);
			dataOutput.writeObject(obj);
			dataOutput.close();
			return Base64Coder.encodeLines(outputStream.toByteArray()).replace("\n","").replace("\r","");
        } catch (Exception e) {}
		return null;
    }
	
	public static Object ObjectFromBase64(String data) {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
			BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
			dataInput.readInt();
			Object obj = dataInput.readObject();
			dataInput.close();
			return obj;
		} catch (Exception e) {}
		return null;
    }
}