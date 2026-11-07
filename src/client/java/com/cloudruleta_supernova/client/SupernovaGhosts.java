package com.cloudruleta_supernova.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cloudruleta_supernova.SupernovaNet;
import com.cloudruleta_supernova.SupernovaState;

import net.minecraft.client.Minecraft;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.client.ClientPlayerNetworkEvent;

public class SupernovaGhosts {
	private static final Map<Integer, SupernovaState> MAP = new ConcurrentHashMap<>();

	public static void apply(SupernovaNet.SyncMessage m) {
		if (m.remove)
			MAP.remove(m.id);
		else
			MAP.put(m.id, m.toState());
	}

	public static SupernovaState get(int id) {
		return MAP.get(id);
	}

	public static List<SupernovaState> active(float now) {
		List<SupernovaState> out = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return out;
		String dim = mc.level.dimension().location().toString();
		for (SupernovaState s : MAP.values()) {
			if (s.expired(now))
				MAP.remove(s.id);
			else if (s.dim.equals(dim))
				out.add(s);
		}
		return out;
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
		MAP.clear();
	}
}
