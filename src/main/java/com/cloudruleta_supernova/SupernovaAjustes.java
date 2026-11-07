package com.cloudruleta_supernova;

import net.minecraft.util.Mth;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.ServerLifecycle;

/** Tamaño y tiempo de las estrellas nuevas (/cloudruleta_supernova tamaño … tiempo …). */
public final class SupernovaAjustes {
	public static final float TAMANO_MIN = 1.0F;
	public static final float TAMANO_MAX = 500.0F;
	public static final int TIEMPO_MAX_SEGUNDOS = 86400;
	public static float tamano = 250.0F;
	public static int tiempoTicks = 0;

	private SupernovaAjustes() {
	}

	public static float limitarTamano(float t) {
		return Mth.clamp(t, TAMANO_MIN, TAMANO_MAX);
	}

	public static int limitarTicks(int ticks) {
		return Mth.clamp(ticks, 0, TIEMPO_MAX_SEGUNDOS * 20);
	}

	@SubscribeEvent
	public static void alApagar(ServerLifecycle.ServerStoppedEvent e) {
		tamano = 250.0F;
		tiempoTicks = 0;
	}
}
