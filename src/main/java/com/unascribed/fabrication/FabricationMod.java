package com.unascribed.fabrication;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.unascribed.fabrication.support.Feature;
import com.unascribed.fabrication.support.MixinConfigPlugin;
import com.unascribed.fabrication.support.MixinConfigPlugin.RuntimeChecks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;

public class FabricationMod implements ModInitializer {
	
	private static final Map<String, Feature> features = Maps.newHashMap();
	private static final List<Feature> unconfigurableFeatures = Lists.newArrayList();
	private static final Set<String> enabledFeatures = Sets.newHashSet();
	
	@Override
	public void onInitialize() {
		for (String s : MixinConfigPlugin.discoverClassesInPackage("com.unascribed.fabrication.features", false)) {
			try {
				Feature r = (Feature)Class.forName(s).newInstance();
				String key = MixinConfigPlugin.remap(r.getConfigKey());
				if (key == null || RuntimeChecks.check(key)) {
					r.apply();
					if (key != null) {
						enabledFeatures.add(key);
					}
				}
				if (key != null) {
					features.put(key, r);
				} else {
					unconfigurableFeatures.add(r);
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to initialize feature "+s, e);
			}
		}
	}
	
	public static boolean isAvailableFeature(String configKey) {
		return features.containsKey(MixinConfigPlugin.remap(configKey));
	}
	
	public static boolean updateFeature(String configKey) {
		configKey = MixinConfigPlugin.remap(configKey);
		boolean enabled = MixinConfigPlugin.isEnabled(configKey);
		if (enabledFeatures.contains(configKey) == enabled) return true;
		if (enabled) {
			features.get(configKey).apply();
			enabledFeatures.add(configKey);
			return true;
		} else {
			boolean b = features.get(configKey).undo();
			if (b) {
				enabledFeatures.remove(configKey);
			}
			return b;
		}
	}
	
	public static <T> T snag(Class<?> clazz, Object inst, String intermediateName, String yarnName) {
		try {
			return (T)snagField(clazz, intermediateName, yarnName).get(inst);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Field snagField(Class<?> clazz, String intermediateName, String yarnName) {
		try {
			Field f;
			try {
				f = clazz.getDeclaredField(intermediateName);
			} catch (NoSuchFieldException e) {
				f = clazz.getDeclaredField(yarnName);
			}
			f.setAccessible(true);
			return f;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T> T twiddle(Class<?> clazz, Object inst, String intermediateName, String yarnName, Class<?>[] argTypes, Object... args) {
		try {
			Method m;
			try {
				m = clazz.getDeclaredMethod(intermediateName, argTypes);
			} catch (NoSuchMethodException e) {
				m = clazz.getDeclaredMethod(yarnName, argTypes);
			}
			m.setAccessible(true);
			return (T)m.invoke(inst, args);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static void sendToTrackersMatching(Entity entity, CustomPayloadS2CPacket pkt, Predicate<ServerPlayerEntity> predicate) {
		if (entity.world.isClient) return;
		ServerChunkManager cm = ((ServerWorld)entity.world).getChunkManager();
		ThreadedAnvilChunkStorage tacs = cm.threadedAnvilChunkStorage;
		Int2ObjectMap<?> entityTrackers = FabricationMod.snag(ThreadedAnvilChunkStorage.class, tacs, "field_18242", "entityTrackers");
		Object tracker = entityTrackers.get(entity.getEntityId());
		if (tracker == null) return;
		Set<ServerPlayerEntity> playersTracking = FabricationMod.snag(tracker.getClass(), tracker, "field_18250", "playersTracking");
		if (entity instanceof ServerPlayerEntity) {
			ServerPlayerEntity spe = (ServerPlayerEntity)entity;
			if (predicate.test(spe)) {
				spe.networkHandler.sendPacket(pkt);
			}
		}
		for (ServerPlayerEntity spe : playersTracking) {
			if (predicate.test(spe)) {
				spe.networkHandler.sendPacket(pkt);
			}
		}
	}
	
}
