package me.DMan16.ItemFrameShop.Utils;

import me.DMan16.ItemFrameShop.ItemFrameShopMain;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	public static String chatColors(String str) {
		if (str == null) return null;
		return ChatColor.translateAlternateColorCodes('&',chatColorsHex(str));
	}
	
	private static String chatColorsHex(@NotNull String str) {
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
	
	@NotNull
	public static List<String> chatColors(@NotNull List<String> list) {
		List<String> newList = new ArrayList<>();
		for (String str : list) if (str != null)
			if (str.trim().isEmpty()) newList.add("");
			else newList.add(chatColors(str));
		return newList;
	}
	
	public static void chatColors(@NotNull CommandSender sender, @NotNull String str) {
		sender.sendMessage(chatColors(str));
	}
	
	public static void logPlugin(@NotNull String str) {
		str = chatColorsPlugin("") + str;
		if (getVersionInt() < 16) str = chatColorsStrip(str);
		Bukkit.getLogger().info(str);
	}
	
	public static void chatColorsLogPlugin(@NotNull String str) {
		logPlugin(chatColors(str));
	}
	
	@NotNull
	public static String chatColorsPlugin(@NotNull String str) {
		return chatColors("&d[" + ItemFrameShopMain.pluginNameColors + "&d]&r " + str);
	}

	public static void chatColorsPlugin(@NotNull CommandSender sender, @NotNull String str) {
		sender.sendMessage(chatColorsPlugin(str));
	}
	
	public static void chatColorsActionBar(@NotNull Player player, @NotNull String str) {
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Utils.chatColors(str)));
	}
	
	@NotNull
	public static String chatColorsStrip(@NotNull String str) {
		return ChatColor.stripColor(str);
	}
	
	@NotNull
	public static NamespacedKey namespacedKey(@NotNull String name) {
		return new NamespacedKey(ItemFrameShopMain.getInstance(),name);
	}
	
	@Nullable
	public static ItemStack asQuantity(ItemStack item, int amount) {
		return asQuantity(item,amount,false);
	}
	
	@Nullable
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
		return Objects.equals(cmp1,cmp2);
	}
	
	public static boolean isNull(ItemStack item) {
		if (item == null || item.getAmount() <= 0) return true;
		if (getVersionInt() < 15) return item.getType().name().endsWith("AIR");
		return item.getType().isAir();
	}
	
	public static boolean givePlayer(@NotNull Player player, ItemStack item, @Nullable Location drop, boolean glow) {
		if (isNull(item)) return false;
		if (!player.isDead() && player.getInventory().addItem(item).isEmpty()) return true;
		if (drop != null) {
			Item droppedItem = dropItem(drop,item);
			if (droppedItem != null && glow) droppedItem.setGlowing(true);
		}
		return false;
	}
	
	public static Item dropItem(@NotNull Location drop, @NotNull ItemStack item) {
		return drop.getWorld() == null ? null : drop.getWorld().dropItemNaturally(drop,item);
	}
	
	public static boolean givePlayer(@NotNull Player player, @NotNull ItemStack item, boolean glow) {
		return givePlayer(player,item,player.getLocation(),glow);
	}
	
	@NotNull
	public static String getVersion() {
		return Bukkit.getServer().getVersion().split("\\(MC:")[1].split("\\)")[0].trim().split(" ")[0].trim();
	}
	
	public static int getVersionMain() {
		return Integer.parseInt(getVersion().split("\\.")[0]);
	}
	
	public static int getVersionInt() {
		return Integer.parseInt(getVersion().split("\\.")[1]);
	}
	
	public static double roundAfterDot(double num) {
		int digitsAfterDot = Math.min(Math.max(ItemFrameShopMain.config().getInt("price-round-after-dot"),0),2);
		if (digitsAfterDot < 0) return num;
		if (digitsAfterDot == 0) return Math.round(num);
		StringBuilder format = new StringBuilder("0.");
		for (int i = 0; i < digitsAfterDot; i++) format.append("0");
		return Double.parseDouble((new DecimalFormat(format.toString())).format(num));
	}
	
	@Nullable
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
	
    @Nullable
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