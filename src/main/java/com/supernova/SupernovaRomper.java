package com.supernova;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.ruleta.bunker.Bunkers;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.ServerLifecycle;
import net.ruleta.compat.event.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** /supernova romper on: la onda vacía todo su círculo (columna a columna, del centro hacia fuera) hasta el vacío. */
public final class SupernovaRomper {
	public static boolean activo;
	private static final int BLOQUES_POR_TICK = 40000;
	private static final int COLUMNAS_POR_TICK = 600;
	private static final BlockState AIRE = Blocks.AIR.defaultBlockState();
	private static final List<Trabajo> TRABAJOS = new ArrayList<>();

	private SupernovaRomper() {
	}

	static Trabajo empezar(ServerLevel nivel, double x, double z, float radioMax) {
		Trabajo t = new Trabajo(nivel, x, z, radioMax);
		TRABAJOS.add(t);
		return t;
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ServerTickEvent e) {
		if (e.phase != TickEvent.Phase.END || TRABAJOS.isEmpty())
			return;
		int bloques = BLOQUES_POR_TICK;
		int columnas = COLUMNAS_POR_TICK;
		Iterator<Trabajo> it = TRABAJOS.iterator();
		while (it.hasNext() && bloques > 0 && columnas > 0) {
			Trabajo t = it.next();
			ServerLevel nivel = t.nivel;
			int arriba = nivel.getMaxBuildHeight() - 1;
			int abajo = nivel.getMinBuildHeight();
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			while (!t.terminado() && t.dist[t.siguiente] <= t.limite && bloques > 0 && columnas > 0) {
				int x = t.cx + t.dx[t.siguiente];
				int z = t.cz + t.dz[t.siguiente];
				int y = t.yPendiente == Integer.MIN_VALUE ? arriba : t.yPendiente;
				for (; y >= abajo && bloques > 0; y--) {
					pos.set(x, y, z);
					BlockState estado = nivel.getBlockState(pos);
					if (!estado.isAir() && !Bunkers.protegido(nivel, pos, estado)) {
						nivel.setBlock(pos, AIRE, 18);
						bloques--;
					}
				}
				if (y < abajo) {
					t.yPendiente = Integer.MIN_VALUE;
					t.siguiente++;
					columnas--;
				} else {
					t.yPendiente = y;
				}
			}
			if (t.terminado())
				it.remove();
		}
	}

	@SubscribeEvent
	public static void alApagar(ServerLifecycle.ServerStoppedEvent e) {
		TRABAJOS.clear();
		activo = false;
	}

	/** Columnas del círculo ordenadas por distancia al centro; limite = radio que ya ha alcanzado la onda. */
	static final class Trabajo {
		final ServerLevel nivel;
		final int cx;
		final int cz;
		final int[] dx;
		final int[] dz;
		final float[] dist;
		int siguiente;
		float limite;
		int yPendiente = Integer.MIN_VALUE;

		Trabajo(ServerLevel nivel, double x, double z, float radioMax) {
			this.nivel = nivel;
			this.cx = (int) Math.floor(x);
			this.cz = (int) Math.floor(z);
			int r = (int) Math.ceil(radioMax);
			int lado = 2 * r + 1;
			long[] tmp = new long[lado * lado];
			int n = 0;
			for (int a = -r; a <= r; a++)
				for (int b = -r; b <= r; b++) {
					int d2 = a * a + b * b;
					if ((float) d2 <= radioMax * radioMax)
						tmp[n++] = (long) d2 << 32 | (long) (a + 32768) << 16 | (long) (b + 32768);
				}
			Arrays.sort(tmp, 0, n);
			this.dx = new int[n];
			this.dz = new int[n];
			this.dist = new float[n];
			for (int i = 0; i < n; i++) {
				this.dx[i] = (int) (tmp[i] >> 16 & 65535L) - 32768;
				this.dz[i] = (int) (tmp[i] & 65535L) - 32768;
				this.dist[i] = (float) Math.sqrt((double) (tmp[i] >>> 32));
			}
		}

		boolean terminado() {
			return this.siguiente >= this.dx.length;
		}
	}
}
