package me.DMan16.ItemFrameShop.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PacketUtils {
	
	public static void sendPacket(Object packet, Player player) {
		if (packet == null) return;
		try {
			Method sendPacket = ReflectionUtils.getClassNMS("PlayerConnection","server.network").getMethod("sendPacket",
					ReflectionUtils.getClassNMS("Packet","network.protocol"));
			Field playerConnection = ReflectionUtils.getClassNMS("EntityPlayer","server.level").getDeclaredField(Utils.getVersionInt() < 17 ? "playerConnection" : "b");
			Method getHandle = ReflectionUtils.getClassCraftBukkit("entity.CraftPlayer").getMethod("getHandle");
			Object playerHandle = getHandle.invoke(player);
			Object playerPlayerConnection = playerConnection.get(Objects.requireNonNull(playerHandle));
			Objects.requireNonNull(sendPacket).invoke(Objects.requireNonNull(playerPlayerConnection),packet);
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public static Object packetCreateArmorStand(int ID, Location loc, Object entityDataWatcher) {
		try {
			Class<?> PacketPlayOutSpawnEntityLiving = ReflectionUtils.getClassNMS("PacketPlayOutSpawnEntityLiving","network.protocol.game");
			Constructor<?> newPacketPlayOutSpawnEntityLiving = PacketPlayOutSpawnEntityLiving.getConstructor(ReflectionUtils.getClassNMS("EntityLiving",
					"world.entity"));
			
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
			
			Object packet = newPacketPlayOutSpawnEntityLiving.newInstance(ReflectionUtils.getClassCraftBukkit("entity.CraftPlayer").getMethod("getHandle").
					invoke(Bukkit.getOnlinePlayers().iterator().next()));
			entityID.set(packet,ID);
			entityType.set(packet,1);
			entityYaw.set(packet,(byte) loc.getYaw());
			entityPitch.set(packet,(byte) loc.getPitch());
			entityUUID.set(packet,UUID.randomUUID());
			entityX.set(packet,loc.getX());
			entityY.set(packet,loc.getY());
			entityZ.set(packet,loc.getZ());
			if (Utils.getVersionInt() <= 14) {
				Field DataWatcher = PacketPlayOutSpawnEntityLiving.getDeclaredField("m");
				DataWatcher.setAccessible(true);
				DataWatcher.set(packet,entityDataWatcher);
			}
			return packet;
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public static Object packetDataWatcherArmorStand(Object name) {
		try {
			Class<?> DataWatcher = ReflectionUtils.getClassNMS("DataWatcher","network.syncher");
			Constructor<?> DataWatcherConstructor = DataWatcher.getConstructor(ReflectionUtils.getClassNMS("Entity","world.entity"));
			Class<?> DataWatcherObject = ReflectionUtils.getClassNMS("DataWatcherObject","network.syncher");
			Constructor<?> DataWatcherObjectConstructor = DataWatcherObject.getConstructors()[0];
			Method register = DataWatcher.getMethod("register",DataWatcherObject,Object.class);
			Class<?> DataWatcherRegistry = ReflectionUtils.getClassNMS("DataWatcherRegistry","network.syncher");
			Map<String,Object> fields = ReflectionUtils.getStaticFields(DataWatcherRegistry);
			Object nmsWatcher = DataWatcherConstructor.newInstance((Object) null);
			register.invoke(nmsWatcher,DataWatcherObjectConstructor.newInstance(0,fields.get("a")),(byte) 32);
			register.invoke(nmsWatcher,DataWatcherObjectConstructor.newInstance(2,fields.get("f")),Optional.ofNullable(ReflectionUtils.StringToIChatBaseComponent(name)));
			register.invoke(nmsWatcher,DataWatcherObjectConstructor.newInstance(3,fields.get("i")),true);
			int version = Utils.getVersionInt();
			int flagPosition;
			if (version >= 17) flagPosition = 15;
			else if (version >= 15) flagPosition = 14;
			else if (version >= 14) flagPosition = 13;
			else flagPosition = 11;
			register.invoke(nmsWatcher,DataWatcherObjectConstructor.newInstance(flagPosition,fields.get("a")),(byte) 16);
			return nmsWatcher;
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public static Object packetUpdateArmorStand(int ID, Object entityDataWatcher) {
		try {
			Class<?> PacketPlayOutEntityMetadata = ReflectionUtils.getClassNMS("PacketPlayOutEntityMetadata","network.protocol.game");
			Class<?> DataWatcher = ReflectionUtils.getClassNMS("DataWatcher","network.syncher");
			Constructor<?> PacketPlayOutEntityMetadataConstructor = PacketPlayOutEntityMetadata.getConstructor(Integer.TYPE,DataWatcher,Boolean.TYPE);
			return PacketPlayOutEntityMetadataConstructor.newInstance(ID,entityDataWatcher,true);
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public static Object packetDestroyEntity(int ID) {
		try {
			Class<?> PacketPlayOutEntityDestroy = ReflectionUtils.getClassNMS("PacketPlayOutEntityDestroy","network.protocol.game");
			Constructor<?> PacketPlayOutEntityDestroyConstructor;
			try {
				PacketPlayOutEntityDestroyConstructor = PacketPlayOutEntityDestroy.getConstructor(int[].class);
			} catch (Exception e) {
				PacketPlayOutEntityDestroyConstructor = PacketPlayOutEntityDestroy.getConstructor(int.class);
			}
			return PacketPlayOutEntityDestroyConstructor.newInstance(ID);
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
}