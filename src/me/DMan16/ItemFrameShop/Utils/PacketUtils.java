package me.DMan16.ItemFrameShop.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PacketUtils {
	private static final String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	
	public static void sendPacket(Object packet, Player player) {
		if (packet == null) return;
		try {
			Method sendPacket = Class.forName("net.minecraft.server." + version + ".PlayerConnection").getMethod("sendPacket",
					Class.forName("net.minecraft.server." + version + ".Packet"));
			Field playerConnection = Class.forName("net.minecraft.server." + version + ".EntityPlayer").getDeclaredField("playerConnection");
			Method getHandle = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer").getMethod("getHandle");
			Object playerHandle = getHandle.invoke(player);
			Object playerPlayerConnection = playerConnection.get(playerHandle);
			Objects.requireNonNull(playerHandle);
			Objects.requireNonNull(playerPlayerConnection);
			Objects.requireNonNull(sendPacket);
			sendPacket.invoke(playerPlayerConnection,packet);
		} catch (Exception e) {}
	}
	
	public static Object packetCreateArmorStand(int ID, Location loc, Object entityDataWatcher) {
		try {
			Class<?> PacketPlayOutSpawnEntityLiving = Class.forName("net.minecraft.server." + version + ".PacketPlayOutSpawnEntityLiving");
			Constructor<?> newPacketPlayOutSpawnEntityLiving = PacketPlayOutSpawnEntityLiving.getConstructor();
			Field entityID = PacketPlayOutSpawnEntityLiving.getDeclaredField("a");
			Field entityUUID = PacketPlayOutSpawnEntityLiving.getDeclaredField("b");
			Field entityType = PacketPlayOutSpawnEntityLiving.getDeclaredField("c");
			Field entityX = PacketPlayOutSpawnEntityLiving.getDeclaredField("d");
			Field entityY = PacketPlayOutSpawnEntityLiving.getDeclaredField("e");
			Field entityZ = PacketPlayOutSpawnEntityLiving.getDeclaredField("f");
			Field entityYaw = PacketPlayOutSpawnEntityLiving.getDeclaredField("j");
			Field entityPitch = PacketPlayOutSpawnEntityLiving.getDeclaredField("k");
			entityID.setAccessible(true);
			entityUUID.setAccessible(true);
			entityType.setAccessible(true);
			entityX.setAccessible(true);
			entityY.setAccessible(true);
			entityZ.setAccessible(true);
			entityYaw.setAccessible(true);
			entityPitch.setAccessible(true);
			
			Object packet = newPacketPlayOutSpawnEntityLiving.newInstance();
			entityID.set(packet,Integer.valueOf(ID));
			entityType.set(packet,Integer.valueOf(1));
			entityYaw.set(packet,Byte.valueOf((byte) loc.getYaw()));
			entityPitch.set(packet,Byte.valueOf((byte) loc.getPitch()));
			entityUUID.set(packet,UUID.randomUUID());
			entityX.set(packet,Double.valueOf(loc.getX()));
			entityY.set(packet,Double.valueOf(loc.getY()));
			entityZ.set(packet,Double.valueOf(loc.getZ()));
			if (Utils.getVersionInt() <= 14) {
				Field DataWatcher = PacketPlayOutSpawnEntityLiving.getDeclaredField("m");
				DataWatcher.setAccessible(true);
				DataWatcher.set(packet,entityDataWatcher);
			}
			return packet;
		} catch (Exception e) {}
		return null;
	}
	
	public static Object packetDataWatcherArmorStand(Object name) {
		try {
			Class<?> DataWatcher = Class.forName("net.minecraft.server." + ReflectionUtils.version + ".DataWatcher");
			Constructor<?> DataWatcherConstructor = DataWatcher.getConstructor(Class.forName("net.minecraft.server." + ReflectionUtils.version + ".Entity"));
			Class<?> DataWatcherObject = Class.forName("net.minecraft.server." + ReflectionUtils.version + ".DataWatcherObject");
			Constructor<?> DataWatcherObjectConstructor = DataWatcherObject.getConstructors()[0];
			Method register = DataWatcher.getMethod("register",DataWatcherObject,Object.class);
			Class<?> DataWatcherRegistry = Class.forName("net.minecraft.server." + ReflectionUtils.version + ".DataWatcherRegistry");

			Map<String,Object> fields = ReflectionUtils.getStaticFields(DataWatcherRegistry);
			Object nmsWatcher = DataWatcherConstructor.newInstance((Object) null);
			register.invoke(nmsWatcher,DataWatcherObjectConstructor.newInstance(0,fields.get("a")),(byte) 32);
			register.invoke(nmsWatcher,DataWatcherObjectConstructor.newInstance(2,fields.get("f")),
					Optional.ofNullable(ReflectionUtils.StringToIChatBaseComponent(name)));
			register.invoke(nmsWatcher,DataWatcherObjectConstructor.newInstance(3,fields.get("i")),true);
			int flagPosition = Utils.getVersionInt() >= 15 ? 14 : (Utils.getVersionInt() >= 14 ? 13 : 11);
			register.invoke(nmsWatcher,DataWatcherObjectConstructor.newInstance(flagPosition,fields.get("a")),(byte) 16);
			return nmsWatcher;
		} catch (Exception e) {}
		return null;
	}
	
	public static Object packetUpdateArmorStand(int ID, Object entityDataWatcher) {
		try {
			Class<?> PacketPlayOutEntityMetadata = Class.forName("net.minecraft.server." + ReflectionUtils.version + ".PacketPlayOutEntityMetadata");
			Class<?> DataWatcher = Class.forName("net.minecraft.server." + ReflectionUtils.version + ".DataWatcher");
			Constructor<?> PacketPlayOutEntityMetadataConstructor = PacketPlayOutEntityMetadata.getConstructor(Integer.TYPE,DataWatcher,Boolean.TYPE);
			return PacketPlayOutEntityMetadataConstructor.newInstance(ID,entityDataWatcher,true);
		} catch (Exception e) {}
		return null;
	}
	
	public static Object packetDestroyEntity(int ... IDs) {
		try {
			Class<?> PacketPlayOutEntityDestroy = Class.forName("net.minecraft.server." + ReflectionUtils.version + ".PacketPlayOutEntityDestroy");
			Constructor<?> PacketPlayOutEntityDestroyConstructor = PacketPlayOutEntityDestroy.getConstructor(new Class[]{int[].class});
			return PacketPlayOutEntityDestroyConstructor.newInstance(IDs);
		} catch (Exception e) {}
		return null;
	}
}