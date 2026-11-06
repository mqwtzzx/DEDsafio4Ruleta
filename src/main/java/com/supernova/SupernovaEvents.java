package com.supernova;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.EntityEvents.EntityJoinLevelEvent;
import net.ruleta.compat.event.EntityEvents.EntityLeaveLevelEvent;
import net.ruleta.compat.event.PlayerEvent;

/** Mantiene cargado el chunk de cada estrella y sincroniza su estado con los clientes. */
public class SupernovaEvents {
	@SubscribeEvent
	public static void onJoin(EntityJoinLevelEvent e) {
		if (e.getLevel().isClientSide() || !(e.getEntity() instanceof SupernovaEntity n))
			return;
		if (e.getLevel() instanceof ServerLevel sl) {
			try {
				ChunkPos cp = n.chunkPosition();
				sl.setChunkForced(cp.x, cp.z, true);
			} catch (Exception ignored) {
			}
		}
		SupernovaNet.broadcast(n, false);
	}

	@SubscribeEvent
	public static void onLeave(EntityLeaveLevelEvent e) {
		if (e.getLevel().isClientSide() || !(e.getEntity() instanceof SupernovaEntity n))
			return;
		SupernovaNet.broadcast(n, true);
		if (e.getLevel() instanceof ServerLevel sl && sl.getServer().isRunning()) {
			try {
				ChunkPos cp = n.chunkPosition();
				sl.setChunkForced(cp.x, cp.z, false);
			} catch (Exception ignored) {
			}
		}
	}

	private static void sendAll(ServerPlayer p) {
		for (SupernovaEntity n : p.serverLevel().getEntities(SupernovaMod.SUPERNOVA.get(), x -> true))
			SupernovaNet.sendTo(p, n);
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
		if (e.getEntity() instanceof ServerPlayer p)
			sendAll(p);
	}

	@SubscribeEvent
	public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
		if (e.getEntity() instanceof ServerPlayer p)
			sendAll(p);
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
		if (e.getEntity() instanceof ServerPlayer p)
			sendAll(p);
	}
}
