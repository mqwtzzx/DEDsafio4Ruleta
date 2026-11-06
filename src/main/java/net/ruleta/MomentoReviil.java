package net.ruleta;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Estado global del Momento Reviil (activo, hasta cuándo, con cielo rojo o no). */
public final class MomentoReviil {
	private static long tick;
	private static long desde = -1L;
	private static long hasta = -1L;
	private static boolean cielo;
	private static long desdeCielo = -1L;
	static final float GRIETA_ABIERTA = 16.623932F;

	private MomentoReviil() {
	}

	static void empezar(MinecraftServer server, int segundos) {
		empezar(server, segundos, true);
	}

	static void empezar(MinecraftServer server, int segundos, boolean conCielo) {
		empezar(server, segundos, conCielo, true);
	}

	static void empezar(MinecraftServer server, int segundos, boolean conCielo, boolean conMusica) {
		if (desde >= 0L) {
			if (segundos > 0)
				hasta = tick + (long) segundos * 20L;
			if (conCielo && !cielo) {
				cielo = true;
				desdeCielo = tick;
				Red.enviarATodos(server, mensaje(0));
			}
			return;
		}
		desde = tick;
		hasta = segundos > 0 ? tick + (long) segundos * 20L : -1L;
		Red.enviarATodos(server, new ReviilPayload("reviil_activo", 1, Component.empty(), Component.empty()));
		cielo = conCielo;
		desdeCielo = tick;
		if (cielo)
			Red.enviarATodos(server, mensaje(0));
		else if (conMusica)
			Red.enviarATodos(server, new ReviilPayload("musica_reviil", 1, Component.empty(), Component.empty()));
	}

	/** Ticks que faltan para que la grieta del cielo esté abierta (el anuncio espera a ese momento). */
	static int ticksHastaGrieta() {
		if (desde < 0L || !cielo)
			return 0;
		long falta = desdeCielo + (long) Math.round(GRIETA_ABIERTA * 20.0F) + 5L - tick;
		return (int) Math.max(0L, falta);
	}

	static void alargar(int ticks) {
		if (desde >= 0L && hasta >= 0L)
			hasta += ticks;
	}

	public static boolean enMarchaConCielo() {
		return desde >= 0L && cielo;
	}

	public static boolean enMarcha() {
		return desde >= 0L;
	}

	static void terminar(MinecraftServer server) {
		if (desde < 0L)
			return;
		desde = -1L;
		hasta = -1L;
		Red.enviarATodos(server, new ReviilPayload("reviil_activo", 0, Component.empty(), Component.empty()));
		if (cielo)
			Red.enviarATodos(server, mensaje(-1));
		cielo = false;
		MomentosReviil.alTerminar(server);
		if (RuletaColor.ROJO.id.equals(Pantalla.aviso))
			Pantalla.sacarAviso(server, null);
	}

	static void tick(MinecraftServer server) {
		tick++;
		if (hasta >= 0L && tick >= hasta)
			terminar(server);
	}

	static void alEntrar(ServerPlayer jugador) {
		Red.enviar(jugador, new ReviilPayload("reviil_activo", desde >= 0L ? 1 : 0, Component.empty(), Component.empty()));
		if (desde >= 0L && cielo)
			Red.enviar(jugador, mensaje((int) ((tick - desdeCielo) / 20L)));
	}

	static void limpiar() {
		desde = -1L;
		hasta = -1L;
		cielo = false;
		desdeCielo = -1L;
	}

	private static ReviilPayload mensaje(int valor) {
		return new ReviilPayload("cielo", valor, Component.empty(), Component.empty());
	}
}
