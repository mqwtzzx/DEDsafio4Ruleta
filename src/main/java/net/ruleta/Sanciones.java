package net.ruleta;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.ServerLifecycle.ServerStoppedEvent;
import net.ruleta.compat.event.TickEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Sanciones de la ruleta morada: ban temporal o expulsión, 10 segundos después del anuncio. */
public final class Sanciones {
	public static final int NADA = 0;
	public static final int BAN = 1;
	public static final int KICK = 2;
	private static final int TICKS_LEER = 200;
	private static final List<Pendiente> PENDIENTES = new ArrayList<>();
	private static long tick;

	private Sanciones() {
	}

	/** Operadores de nivel 3 o 4: no se les puede sancionar. */
	static boolean protegido(MinecraftServer server, ServerPlayer p) {
		return server.getProfilePermissions(p.getGameProfile()) >= 3;
	}

	/** "30", "30m", "2h", "1h30m" → segundos (0 si no vale). */
	public static long leerTiempo(String s) {
		if (s == null)
			return 0L;
		String t = s.trim().toLowerCase().replace(" ", "");
		if (t.isEmpty())
			return 0L;
		try {
			if (t.matches("\\d+"))
				return Long.parseLong(t) * 60L;
			if (!t.matches("(\\d+h)?(\\d+m)?"))
				return 0L;
			long total = 0L;
			int h = t.indexOf('h');
			if (h >= 0) {
				total += Long.parseLong(t.substring(0, h)) * 3600L;
				t = t.substring(h + 1);
			}
			if (t.endsWith("m"))
				total += Long.parseLong(t.substring(0, t.length() - 1)) * 60L;
			return total;
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	static String textoTiempo(long segundos) {
		long h = segundos / 3600L;
		long m = segundos % 3600L / 60L;
		StringBuilder b = new StringBuilder();
		if (h > 0L)
			b.append(h).append(h == 1L ? " hora" : " horas");
		if (m > 0L) {
			if (b.length() > 0)
				b.append(" y ");
			b.append(m).append(m == 1L ? " minuto" : " minutos");
		}
		return b.toString();
	}

	static void programar(ServerPlayer objetivo, int tipo, long segundos, String quien) {
		PENDIENTES.add(new Pendiente(tick + TICKS_LEER, objetivo.getUUID(), tipo, segundos, textoTiempo(segundos), quien));
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ServerTickEvent e) {
		if (e.phase != TickEvent.Phase.END)
			return;
		tick++;
		if (PENDIENTES.isEmpty())
			return;
		MinecraftServer server = e.getServer();
		Iterator<Pendiente> it = PENDIENTES.iterator();
		while (it.hasNext()) {
			Pendiente s = it.next();
			if (tick < s.en())
				continue;
			it.remove();
			ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(s.jugador());
			if (p == null || protegido(server, p))
				continue;
			if (s.tipo() == BAN) {
				Date ahora = new Date();
				Date hasta = new Date(ahora.getTime() + s.segundos() * 1000L);
				String razon = s.textoTiempo() + " baneado/a por la Ruleta Morada.";
				server.getPlayerList().getBans().add(new UserBanListEntry(p.getGameProfile(), ahora, s.quien(), hasta, razon));
				p.connection.disconnect(Component.literal("Desconectado/a.\n" + s.textoTiempo() + " baneado/a."));
			} else if (s.tipo() == KICK) {
				p.connection.disconnect(Component.literal("GG"));
			}
		}
	}

	@SubscribeEvent
	public static void alApagar(ServerStoppedEvent e) {
		PENDIENTES.clear();
	}

	private record Pendiente(long en, UUID jugador, int tipo, long segundos, String textoTiempo, String quien) {
	}
}
