package me.DMan16.ItemFrameShop.Utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ReflectionUtils {
	public static final String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	public static final Class<?> ClassCraftItemStack = getClassCraftBukkit("inventory.CraftItemStack");
	public static final Class<?> ClassItemStackNMS = getClassNMS("ItemStack","world.item");
	public static final Class<?> ClassCraftMetaItem = getClassCraftBukkit("inventory.CraftMetaItem");
	
	
	private static String removeUnnecessaryDots(String str) {
		return Arrays.stream(str.split("\\.")).filter(s -> !s.isEmpty()).collect(Collectors.joining("."));
	}
	
	public static Class<?> getClassNMS(String name, String subPackageNameNewNMS) {
		try {
			return Class.forName(removeUnnecessaryDots("net.minecraft.server." + version + "." + name));
		} catch (Exception e) {
			try {
				return Class.forName(removeUnnecessaryDots("net.minecraft." + subPackageNameNewNMS + "." + name));
			} catch (Exception e2) {}
		}
		return null;
	}
	
	public static Class<?> getClassCraftBukkit(String name) {
		try {
			return Class.forName(removeUnnecessaryDots("org.bukkit.craftbukkit." + version + "." + name));
		} catch (Exception e) {}
		return null;
	}
	
	public static String buildIChatBaseComponentString(String text, boolean translate, boolean italic, @Nullable String color, boolean bold, Object ... with) {
		if (text == null) return null;
		String str = "{\"";
		str += (translate ? "translate" : "text");
		str += "\":\"" + text + "\"";
		str += ",\"italic\":" + italic;
		if (color != null && !color.isEmpty()) str += ",\"color\":\"" + color + "\"";
		if (translate && with.length > 0) {
			List<String> extras = new ArrayList<>();
			for (Object obj : with) {
				String extra = IChatBaseComponentToString(obj);
				if (extra != null) extras.add(extra);
			}
			if (!extras.isEmpty()) {
				str += ",\"with\":[";
				str += String.join(",",extras);
				str += "]";
			}
		}
		str += "}";
		return str;
	}
	
	public static String buildIChatBaseComponentString(String text, boolean translate, @Nullable String color) {
		return buildIChatBaseComponentString(text,translate,false,color,false);
	}
	
	public static Object buildIChatBaseComponentStringExtra(List<Object> comps) {
		if (comps == null || comps.isEmpty()) return null;
		String str = "{\"extra\":[";
		List<String> components = new ArrayList<>();
		for (Object obj : comps) components.add(IChatBaseComponentToString(obj));
		str += String.join(",",components);
		str += "],\"text\":\"\"}";
		return StringToIChatBaseComponent(str);
	}
	
	public static Object buildIChatBaseComponentStringExtra(Object ... components) {
		return buildIChatBaseComponentStringExtra(Arrays.asList(components));
	}
	
	public static String getItemTranslatable(ItemStack item) {
		try {
			Method methodNMSCopy = ClassCraftItemStack.getMethod("asNMSCopy",ItemStack.class);
			Method methodGetItem = ClassItemStackNMS.getMethod("getItem");
			Class<?> nmsItemClass = getClassNMS("Item","world.item");
			Method methodGetName = nmsItemClass.getMethod("getName");
			Object ItemStackNMS = methodNMSCopy.invoke(null,item);
			Object ItemNMS = methodGetItem.invoke(ItemStackNMS);
			String name = (String) methodGetName.invoke(ItemNMS);
			String color = null;
			try {
				Method methodGetRarity = null;
				for (Method method : ClassItemStackNMS.getMethods()) if (method.getReturnType().getName().endsWith("EnumItemRarity") && method.getParameterCount() == 0) {
					methodGetRarity = method;
					break;
				}
				Class<?> itemRarityEnum = getClassNMS("EnumItemRarity","world.item");
				Field itemRarityE = itemRarityEnum.getDeclaredField("e");
				Class<?> chatFormatEnum = getClassNMS("EnumChatFormat","");
				Method methodChatFormatName = chatFormatEnum.getMethod("name");
				Object itemRarity = methodGetRarity.invoke(ItemStackNMS);
				Object itemRarityChatFormat = itemRarityE.get(itemRarity);
				color = ((String) methodChatFormatName.invoke(itemRarityChatFormat)).toLowerCase();
			} catch (Exception e) {e.printStackTrace();}
			return buildIChatBaseComponentString(name,true,color);
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public static String getNameItem(ItemStack item) {
		try {
			ItemMeta meta = item.getItemMeta();
			Field displayNameField = ClassCraftMetaItem.getDeclaredField("displayName");
			displayNameField.setAccessible(true);
			Object MetaItemNMS = ClassCraftMetaItem.cast(meta);
			Object name = displayNameField.get(MetaItemNMS);
			return IChatBaseComponentToString(name);
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public static ItemStack setNameItem(ItemStack item, @Nullable Object name) {
		try {
			ItemMeta meta = item.getItemMeta();
			if (name == null) meta.setDisplayName(null);
			else {
				Field displayNameField = ClassCraftMetaItem.getDeclaredField("displayName");
				displayNameField.setAccessible(true);
				Object MetaItemNMS = ClassCraftMetaItem.cast(meta);
				try {
					displayNameField.set(MetaItemNMS,IChatBaseComponentToString(name));
				} catch (Exception e1) {
					try {
						displayNameField.set(MetaItemNMS,StringToIChatBaseComponent(name));
					} catch (Exception e2) {e2.printStackTrace();}
				}
				meta = (ItemMeta) MetaItemNMS;
			}
			item.setItemMeta(meta);
		} catch (Exception e) {e.printStackTrace();}
		return item;
	}

	public static String ChatColorsToIChatBaseComponent(String str) {
		try {
			Class<?> nmsCraftChatMessage = getClassCraftBukkit("util.CraftChatMessage");
			Method methodfromStringOrNull = nmsCraftChatMessage.getDeclaredMethod("fromStringOrNull",String.class);
			return IChatBaseComponentToString(methodfromStringOrNull.invoke(null,str));
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public static Object StringToIChatBaseComponent(Object obj) {
		try {
			Class<?> nmsIChatBaseComponent = getClassNMS("IChatBaseComponent","network.chat");
			return nmsIChatBaseComponent.cast(obj);
		} catch (Exception e1) {
			try {
				String str = (String) obj;
				if (str.trim().isEmpty()) return str;
				if (!str.startsWith("{") || !(str.toLowerCase().contains("\"text\":\"") || str.toLowerCase().contains("\"translate\":\"")))
					str = ChatColorsToIChatBaseComponent(str);
				Class<?> nmsChatSerializer = getClassNMS("IChatBaseComponent$ChatSerializer","network.chat");
				Method methodA = nmsChatSerializer.getDeclaredMethod("a",String.class);
				return methodA.invoke(null,str);
			} catch (Exception e2) {e2.printStackTrace();}
		}
		return null;
	}
	
	public static String IChatBaseComponentToString(Object obj) {
		try {
			String str = (String) obj;
			if (str.trim().isEmpty()) return str;
			if (str.startsWith("{") && (str.toLowerCase().contains("\"text\":\"") || str.toLowerCase().contains("\"translate\":\""))) return str;
			return ChatColorsToIChatBaseComponent(str);
		} catch (Exception e1) {
			try {
				Class<?> nmsIChatBaseComponent = getClassNMS("IChatBaseComponent","network.chat");
				Class<?> nmsChatSerializer = getClassNMS("IChatBaseComponent$ChatSerializer","network.chat");
				Method methodA = nmsChatSerializer.getDeclaredMethod("a",nmsIChatBaseComponent);
				return (String) methodA.invoke(null,obj);
			} catch (Exception e2) {e2.printStackTrace();}
		}
		return null;
	}
	
	public static Map<String, Object> getStaticFields(Class<?> clazz) {
		Map<String,Object> fields = new HashMap<String,Object>();
		if (clazz == null) return fields;
		for (Field field : getFields(clazz,null)) {
			field.setAccessible(true);
			if (Modifier.isStatic(field.getModifiers())) try {
				fields.put(field.getName(),field.get((Object) null));
			} catch (Exception e) {e.printStackTrace();}
		}
		return fields;
	}
	
	public static List<Field> getFields(Class<?> clazz, Class<?> type) {
		List<Field> list = new ArrayList<Field>();
		if (clazz == null) return list;
		Field[] arr = clazz.getDeclaredFields();
		for (Field field : arr) {
			field.setAccessible(true);
			if (type == null || field.getType() == type) list.add(field);
		}
		return list;
	}
	
	public static int getNextEntityID() {
		int ID = -1;
		try {
			Class<?> entityClass = getClassNMS("Entity","world.entity");
			Field entityCount;
			try {
				entityCount = entityClass.getDeclaredField("entityCount");
			} catch (Exception e) {
				entityCount = entityClass.getDeclaredField("b");
			}
			entityCount.setAccessible(true);
			if (Utils.getVersionInt() < 14) {
				ID = (int) entityCount.get(null);
				entityCount.set(null,ID + 1);
			} else ID = (int) AtomicInteger.class.getDeclaredMethod("incrementAndGet").invoke(entityCount.get(null));
		} catch (Exception e) {e.printStackTrace();}
		return ID;
	}
}