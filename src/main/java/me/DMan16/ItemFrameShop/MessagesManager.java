package me.DMan16.ItemFrameShop;

import com.google.common.base.Charsets;
import me.DMan16.ItemFrameShop.Utils.ReflectionUtils;
import me.DMan16.ItemFrameShop.Utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public final class MessagesManager {
	private static final String name = "messages.yml";
	
	@NotNull private FileConfiguration config;
	@Nullable private String configReloaded;
	@NotNull private String titleCreate;
	@NotNull private String titleEdit;
	@NotNull private String titleBuy;
	@NotNull private String cancel;
	@NotNull private String typeAdmin;
	@Nullable private String typeAdminTranslation;
	@Nullable private boolean suffixesExact;
	@Nullable private List<String> suffixes;
	@Nullable private List<String> saveShopsError;
	@Nullable private List<String> purchase;
	@Nullable private List<String> insufficientAmount;
	@Nullable private List<String> createError;
	@Nullable private List<String> createSuccess;
	@Nullable private List<String> changeError;
	@Nullable private List<String> changeSuccess;
	@Nullable private String priceNotSet;
	@NotNull private String priceBuyItemName;
	@Nullable private List<String> priceBuyItemLore;
	@NotNull private String priceEditItemName;
	@Nullable private List<String> priceEditItemLore;
	@NotNull private String priceBorderName;
	@Nullable private List<String> priceBorderLore;
	@Nullable private List<String> priceMessageWithMaximum;
	@Nullable private List<String> priceMessageNoMaximum;
	@Nullable private List<String> priceErrorWithMaximum;
	@Nullable private List<String> priceErrorNoMaximum;
	
	MessagesManager() {
		reload();
	}
	
	void reload() {
		File file = new File(ItemFrameShopMain.getInstance().getDataFolder(),name);
		if (!file.exists()) ItemFrameShopMain.getInstance().saveResource(name,false);
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		InputStream inputStream = ItemFrameShopMain.getInstance().getResource(name);
		if (inputStream != null) config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream,Charsets.UTF_8)));
		this.config = config;
		configReloaded = getString("config-reloaded");
		titleCreate = getStringNotNull("shop-title-create","&bCreate &6Shop");
		titleEdit = getStringNotNull("shop-title-edit","&bEdit &6Shop");
		titleBuy = getStringNotNull("shop-title-buy","&6Shop");
		cancel = getStringNotNull("price-enter-message-cancel","cancel");
		if (cancel.trim().isEmpty()) cancel = "cancel";
		typeAdmin = getStringNotNull("shop-type-admin","Admin");
		typeAdminTranslation = getString("shop-type-admin-translation");
		if (typeAdminTranslation == null || typeAdminTranslation.trim().isEmpty()) typeAdminTranslation = null;
		suffixesExact = config.getBoolean("currency-suffixes-exact");
		suffixes = createSuffixes();
		saveShopsError = getStringList("save-shops-error");
		purchase = getStringList("player-purchase");
		insufficientAmount = getStringList("shop-insufficient-amount");
		createError = getStringList("shop-create-error");
		createSuccess = getStringList("shop-create-success");
		changeError = getStringList("shop-change-error");
		changeSuccess = getStringList("shop-change-success");
		priceNotSet = getString("price-not-set");
		priceBuyItemName = getStringNotNull("price-buy-item-name","&f<price>");
		priceBuyItemLore = getStringList("price-buy-item-lore");
		priceEditItemName = getStringNotNull("price-edit-item-name","&aPrice");
		priceEditItemLore = getStringList("price-edit-item-lore");
		priceBorderName = getStringNotNull("price-border-name","&f<amount>");
		priceBorderLore = getStringList("price-border-lore");
		priceMessageWithMaximum = getStringList("price-enter-message-with-maximum");
		priceMessageNoMaximum = getStringList("price-enter-message-without-maximum");
		priceErrorWithMaximum = getStringList("price-enter-error-with-maximum");
		priceErrorNoMaximum = getStringList("price-enter-error-without-maximum");
	}
	
	@Nullable
	@Contract("null, _ -> null; !null, _ -> !null")
	private String placeholders(String str, Player player) {
		if (str == null) return null;
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) str = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player,str);
		return Utils.chatColors(str);
	}
	
	@Nullable
	private List<String> placeholders(List<String> list, Player player) {
		if (list == null) return null;
		for (int i = 0; i < list.size(); i++) list.set(i,placeholders(list.get(i),player));
		return list;
	}
	
	@Nullable
	private String replace(String str, String old, String val) {
		return str == null ? null : str.replace(old,val);
	}
	
	@Nullable
	private List<String> replace(List<String> list, String old, String val) {
		if (list == null) return null;
		List<String> newList = new ArrayList<>();
		for (String line : list) newList.add(replace(line,old,val));
		return newList;
	}
	
	@NotNull
	private String getStringNotNull(@NotNull String option, @NotNull String def) {
		String str = getString(option);
		return str == null ? def : str;
	}
	
	@Nullable
	private String getString(@NotNull String option) {
		return config.getString(option);
	}
	
	@Nullable
	private List<String> getStringList(@NotNull String option) {
		if (config.isList(option)) {
			List<String> list = config.getStringList(option);
			return list.isEmpty() ? null : list;
		} else {
			String str = getString(option);
			return str == null ? null : new ArrayList<>(Collections.singletonList(str));
		}
	}
	
	@Nullable
	private List<String> createSuffixes() {
		List<String> configSuffixes = getStringList("currency-suffixes");
		if (configSuffixes == null) return null;
		List<String> suffixes = new ArrayList<>();
		Set<String> suffixesLower = new HashSet<>();
		int count = 0;
		Double a;
		for (String suffix : configSuffixes) {
			if (suffix == null || suffix.trim().isEmpty() || (suffixesExact ? suffixes.contains(suffix.trim()) : suffixesLower.contains(suffix.trim().toLowerCase()))) suffixes.add(null);
			else {
				suffixes.add(suffix.trim());
				suffixesLower.add(suffix.trim().toLowerCase());
				count++;
			}
		}
		if (count == 0) return null;
		return suffixes;
	}
	
	
	@Nullable
	private String join(List<String> list) {
		return list == null ? null : String.join("\n",list);
	}
	
	@NotNull
	private String format(double original) {
		if (original < 1000 || suffixes == null) return "" + original;
		int i = 0, previous = -1;
		double num = original;
		while (i < suffixes.size() && (num /= 1000) >= 1000) {
			if (suffixes.get(i) != null) previous = i;
			i++;
		}
		if (suffixes.get(i) != null) return correctFormat(num,suffixes.get(i));
		if (previous < 0) return "" + original;
		num = original;
		for (i = 0; i <= previous; i++) num /= 1000;
		return correctFormat(num,suffixes.get(previous));
	}
	
	private String correctFormat(double num, @NotNull String suffix) {
		if (num == Math.round(num)) return String.valueOf(num).replace(".0","") + suffix;
		return num + suffix;
	}
	
	@Nullable
	public String configReloaded(Player player) {
		return placeholders(configReloaded,player);
	}
	
	@NotNull
	public String titleCreate(Player player) {
		return placeholders(titleCreate,player);
	}
	
	@NotNull
	public String titleEdit(Player player) {
		return placeholders(titleEdit,player);
	}
	
	@NotNull
	public String titleBuy(Player player) {
		return placeholders(titleBuy,player);
	}
	
	@NotNull
	public String cancel(Player player) {
		return placeholders(cancel,player);
	}
	
	@NotNull
	public String typeAdmin(Player player, String color) {
		if (typeAdminTranslation == null) return ReflectionUtils.buildIChatBaseComponentString(placeholders(typeAdmin,player),false,color);
		return ReflectionUtils.buildIChatBaseComponentString(typeAdminTranslation,true,color);
	}
	
	public boolean suffixesExact() {
		return suffixesExact;
	}
	
	@Nullable
	public List<String> suffixes() {
		return suffixes;
	}
	
	@Nullable
	public String saveShopsError(@NotNull World world) {
		return join(placeholders(replace(saveShopsError,"<world>",world.getName()),null));
	}
	
	@Nullable
	public String purchase(Player player, int amount) {
		return join(placeholders(replace(purchase,"<amount>","" + amount),player));
	}
	
	@Nullable
	public String insufficientAmount(Player player, int amount) {
		return join(placeholders(replace(insufficientAmount,"<amount>","" + amount),player));
	}
	
	@Nullable
	public String createError(Player player) {
		return join(placeholders(createError,player));
	}
	
	@Nullable
	public String createSuccess(Player player) {
		return join(placeholders(createSuccess,player));
	}
	
	@Nullable
	public String changeError(Player player) {
		return join(placeholders(changeError,player));
	}
	
	@Nullable
	public String changeSuccess(Player player) {
		return join(placeholders(changeSuccess,player));
	}
	
	@NotNull
	public String priceBuyItemName(Player player, int amount, double price) {
		double finalPrice = amount * price;
		return placeholders(replace(replace(priceBuyItemName,"<amount>","" + amount),"<price>",ItemFrameShopMain.getEconomyManager().currency(finalPrice)),player);
	}
	
	@Nullable
	public List<String> priceBuyItemLore(Player player, int amount, double price) {
		double finalPrice = amount * price;
		return placeholders(replace(replace(priceBuyItemLore,"<amount>","" + amount),"<price>",ItemFrameShopMain.getEconomyManager().currency(finalPrice)),player);
	}
	
	@NotNull
	public String priceEditItemName(Player player, double price) {
		return placeholders(replace(priceEditItemName,"<price>",price >= 0 ? ItemFrameShopMain.getEconomyManager().currency(price) : priceNotSet),player);
	}
	
	@Nullable
	public List<String> priceEditItemLore(Player player, double price) {
		return placeholders(replace(priceEditItemLore,"<price>",price >= 0 ? ItemFrameShopMain.getEconomyManager().currency(price) : priceNotSet),player);
	}
	
	@NotNull
	public String priceBorderName(Player player, String amount) {
		return placeholders(replace(priceBorderName,"<amount>",amount),player);
	}
	
	@Nullable
	public List<String> priceBorderLore(Player player, String amount) {
		return placeholders(replace(priceBorderLore,"<amount>",amount),player);
	}
	
	@Nullable
	public String priceMessageWithMaximum(Player player, double minimum, double maximum) {
		return join(placeholders(replace(replace(replace(priceMessageWithMaximum,"<minimum>",format(minimum)),"<maximum>","" + format(maximum)),"<cancel>",cancel),player));
	}
	
	@Nullable
	public String priceMessageNoMaximum(Player player, double minimum) {
		return join(placeholders(replace(replace(priceMessageNoMaximum,"<minimum>",format(minimum)),"<cancel>","" + cancel),player));
	}
	
	@Nullable
	public String priceErrorWithMaximum(Player player, double minimum, double maximum) {
		return join(placeholders(replace(replace(replace(priceErrorWithMaximum,"<minimum>",format(minimum)),"<maximum>",format(maximum)),"<cancel>",cancel),player));
	}
	
	@Nullable
	public String priceErrorNoMaximum(Player player, double minimum) {
		return join(placeholders(replace(replace(priceErrorNoMaximum,"<minimum>",format(minimum)),"<cancel>","" + cancel),player));
	}
}