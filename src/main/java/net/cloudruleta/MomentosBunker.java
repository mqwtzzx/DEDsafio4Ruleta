package net.cloudruleta;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.agujero.ComandoAgujero;
import net.cloudruleta.bunker.Bunkers;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.EntityEvents.EntityTravelToDimensionEvent;
import net.cloudruleta.compat.event.ServerLifecycle;
import net.cloudruleta.puerta.PuertaBunkerBloque;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Momentos Reviil que usan el búnker (rayo, agujeros negros) y el eclipse por pasos que acaba en el búnker. */
public final class MomentosBunker {
	private static final long ENCERRADOS = 3000L;
	private static long encerrados = ENCERRADOS;
	private static final float DANO = 2.0F;
	private static final double SEPARACION = 150.0D;
	private static final double CAPA_MIN = 100.0D;
	private static final int MIN_AGUJEROS = 3;
	private static final int MAX_AGUJEROS = 12;
	private static final double LEJOS_BUNKER = 1000.0D;
	private static final long ESPERA_ECLIPSE = 600L;
	private static final int TOTALIDAD_ECLIPSE = Math.max(5, Math.round(61.0F));
	static final String TEXTO_ECLIPSE_2 = "¡Ha llegado el Eclipse! Deben utilizar linternas para abrirse paso por el mundo. CUIDADO: Herobrine está entre nosotros. NO lo mires a los ojos o te atacará y te hará mucho daño. Puedes apuntarle con la Linterna para que desaparezca.";
	static final String TITULO_ECLIPSE_2 = "Momento Reviil";
	@Nullable
	private static Bunkers.Bunker bunker;
	private static int puerta = -1;
	@Nullable
	private static String activo;
	private static long cierra = -1L;
	private static long abre = -1L;
	private static boolean cerrada;
	private static boolean netherFuera;
	private static long eclipseEn = -1L;
	private static long cierreFinal = -1L;
	@Nullable
	private static Bunkers.Bunker bunkerFinal;
	private static int puertaFinal = -1;
	@Nullable
	private static Boolean puertaPedida;
	private static final long UN_MINUTO = 1200L;
	private static String eclipseQuien = "";
	private static long tick;
	private static int pasoEclipse;
	private static boolean eclipseElegido;
	private static boolean eclipseHastaBunker;

	private MomentosBunker() {
	}

	private static boolean esDelBunker(String id) {
		return "bunker_rayo".equals(id) || "bunker_agujeros".equals(id);
	}

	private static boolean usaBunker(String id) {
		return esDelBunker(id) || "eclipse".equals(id);
	}

	private static String bunkerElegido(String id) {
		String e = MomentosReviil.extra(id);
		if ("eclipse".equals(id)) {
			String[] p = e.split(";", -1);
			return p.length >= 6 ? p[5] : "";
		}
		return e;
	}

	private static int[] tiemposEclipse() {
		int[] r = {60, 60, 60, 300, 150};
		String[] p = MomentosReviil.extra("eclipse").split(";", -1);
		for (int i = 0; i < 5 && i < p.length; i++) {
			try {
				int v = Integer.parseInt(p[i].trim());
				if (v > 0)
					r[i] = Math.min(86400, v);
			} catch (NumberFormatException ignored) {
			}
		}
		return r;
	}

	@Nullable
	static String preparar(ServerPlayer admin, Iterable<String> elegidos) {
		eclipseQuien = admin.getGameProfile().getName();
		String quien = null;
		for (String id : elegidos)
			if (usaBunker(id))
				quien = id;
		if (quien == null)
			return null;
		if (bunker == null || activo == null) {
			Bunkers.Bunker elegido = Bunkers.porClave(admin.getServer(), bunkerElegido(quien));
			bunker = elegido != null ? elegido : Bunkers.elegirOConstruir(admin);
			puerta = admin.getRandom().nextInt(Bunkers.PUERTAS.length);
		}
		if (bunker == null) {
			admin.sendSystemMessage(Component.literal("No se pudo preparar el bunker."));
			return "?";
		}
		return "X: " + (int) Math.floor(bunker.mx()) + " // Z: " + (int) Math.floor(bunker.mz());
	}

	static String conCoordenadas(String texto) {
		if (texto == null || !texto.contains("{bunker}"))
			return texto;
		String c = bunker == null ? "?" : "X: " + (int) Math.floor(bunker.mx()) + " // Z: " + (int) Math.floor(bunker.mz());
		return texto.replace("{bunker}", c);
	}

	static void empezar(MinecraftServer server, String id, int segundos, int segundosCerrada) {
		if ("eclipse".equals(id)) {
			int[] t = tiemposEclipse();
			pasoEclipse = 2;
			eclipseEn = tick + (long) t[0] * 20L;
			return;
		}
		if (!esDelBunker(id) || bunker == null)
			return;
		activo = id;
		encerrados = segundosCerrada > 0 ? (long) segundosCerrada * 20L : ENCERRADOS;
		cierreFinal = -1L;
		Bunkers.mostrarMarcador(server, bunker);
		cierra = tick + (long) Math.max(1, segundos) * 20L;
		abre = -1L;
		cerrada = false;
		ponerPuerta(server, true);
		if ("bunker_agujeros".equals(id)) {
			netherFuera = true;
			ComandoAgujero.cielo(server, true);
			agujeros(server);
		}
	}

	private static void ponerPuerta(MinecraftServer server, boolean abierta) {
		puertaPedida = abierta;
		intentarPuerta(server, bunker, puerta);
	}

	/** La puerta se abre/cierra cuando su chunk está cargado; si no, se reintenta cada tick. */
	private static void intentarPuerta(MinecraftServer server, Bunkers.Bunker b, int i) {
		if (puertaPedida == null || b == null || i < 0)
			return;
		ServerLevel nivel = Bunkers.nivel(server, b);
		if (nivel != null && PuertaBunkerBloque.ponerPuerta(nivel, Bunkers.puerta(b, i), puertaPedida))
			puertaPedida = null;
	}

	private static void agujeros(MinecraftServer server) {
		ServerLevel mundo = server.overworld();
		List<ServerPlayer> jugadores = new ArrayList<>();
		for (ServerPlayer p : mundo.players())
			if (!p.isSpectator())
				jugadores.add(p);
		Vec3 centro = bunker == null ? null : new Vec3(bunker.mx(), bunker.ey(), bunker.mz());
		float radio = 15.0F;
		int cuantos = Math.max(MIN_AGUJEROS, Math.min(MAX_AGUJEROS, jugadores.size() * 2));
		List<Vec3> puestos = new ArrayList<>();
		RandomSource rnd = mundo.getRandom();
		for (int intento = 0; intento < cuantos * 40 && puestos.size() < cuantos; intento++) {
			Vec3 base;
			if (!jugadores.isEmpty()) {
				ServerPlayer p = jugadores.get(rnd.nextInt(jugadores.size()));
				base = p.position();
			} else {
				base = Vec3.atCenterOf(mundo.getSharedSpawnPos());
			}
			double ang = rnd.nextDouble() * Math.PI * 2.0D;
			double d = 80.0D + rnd.nextDouble() * 320.0D;
			double x = base.x + Math.cos(ang) * d;
			double z = base.z + Math.sin(ang) * d;
			if (centro != null && Math.hypot(x - centro.x, z - centro.z) < LEJOS_BUNKER)
				continue;
			boolean junto = false;
			for (Vec3 otro : puestos)
				junto |= Math.hypot(x - otro.x, z - otro.z) < SEPARACION;
			if (junto)
				continue;
			int suelo = superficie(mundo, (int) Math.floor(x), (int) Math.floor(z));
			Vec3 sitio = new Vec3(x, Math.max((double) suelo + (double) radio * 2.2D + 6.0D, CAPA_MIN + (double) radio * 2.2D), z);
			ComandoAgujero.crearAgujero(mundo, sitio, radio);
			puestos.add(sitio);
		}
	}

	private static int superficie(ServerLevel mundo, int x, int z) {
		mundo.getChunk(x >> 4, z >> 4);
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, mundo.getMaxBuildHeight() - 1, z);
		while (p.getY() > mundo.getMinBuildHeight()) {
			BlockState s = mundo.getBlockState(p);
			if (!s.isAir() && s.blocksMotion() && !s.is(BlockTags.LEAVES))
				return p.getY() + 1;
			p.move(0, -1, 0);
		}
		return mundo.getSeaLevel();
	}

	static void tick(MinecraftServer server) {
		tick++;
		eclipse(server);
		if (puertaPedida != null) {
			if (activo != null)
				intentarPuerta(server, bunker, puerta);
			else
				intentarPuerta(server, bunkerFinal, puertaFinal);
		}
		if (activo == null) {
			despues(server);
			return;
		}
		if (puertaPedida == null && tick % 20L == 0L)
			ponerPuerta(server, !cerrada);
		if (!cerrada && tick >= cierra) {
			cerrada = true;
			abre = tick + encerrados;
			ponerPuerta(server, false);
		}
		if (cerrada && tick >= abre) {
			terminar(server);
			return;
		}
		if (cerrada && tick % 20L == 0L) {
			for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
				if (!p.isAlive() || p.isCreative() || p.isSpectator() || dentro(p))
					continue;
				p.invulnerableTime = 0;
				if ("bunker_rayo".equals(activo)) {
					p.hurt(Ruleta.fuenteLetal(p, "cloudruleta.rayo", " murió por el Rayo mortal de Reviil"), DANO);
					if (p.getRandom().nextFloat() < 0.35F)
						rayo(p);
				} else {
					p.hurt(Ruleta.fuenteLetal(p, "cloudruleta.agujero", " fue absorbido por los Agujeros Negros de Reviil"), DANO);
				}
			}
		}
	}

	private static boolean dentro(ServerPlayer p) {
		return Bunkers.interiorDe(p.serverLevel(), p.blockPosition()) != null || Bunkers.interiorDe(p.serverLevel(), BlockPos.containing(p.getEyePosition())) != null;
	}

	private static void rayo(ServerPlayer p) {
		ServerLevel nivel = p.serverLevel();
		LightningBolt r = EntityType.LIGHTNING_BOLT.create(nivel);
		if (r != null) {
			double x = p.getX() + (p.getRandom().nextDouble() - 0.5D) * 16.0D;
			double z = p.getZ() + (p.getRandom().nextDouble() - 0.5D) * 16.0D;
			int y = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
			r.moveTo(x, y, z);
			r.setVisualOnly(true);
			nivel.addFreshEntity(r);
		}
	}

	static void terminar(MinecraftServer server) {
		String era = activo;
		if (era != null && cerrada)
			ponerPuerta(server, true);
		acabarEclipse(server);
		if ("bunker_agujeros".equals(era)) {
			ComandoAgujero.quitarTodos(server);
			ComandoAgujero.cielo(server, false);
		}
		if (era != null) {
			bunkerFinal = bunker;
			puertaFinal = puerta;
			cierreFinal = tick + UN_MINUTO;
			Bunkers.mostrarMarcador(server, null);
		}
		activo = null;
		cerrada = false;
		cierra = -1L;
		abre = -1L;
		netherFuera = false;
		bunker = null;
		puerta = -1;
		if (era != null) {
			MomentosReviil.terminarUno(server, era);
			if (!eclipseElegido && MomentosReviil.nadaEnEfecto() && MomentoReviil.enMarcha())
				MomentoReviil.terminar(server);
		}
		if (eclipseElegido) {
			eclipseElegido = false;
			MomentosReviil.terminarUno(server, "eclipse");
			MomentoReviil.terminar(server);
		}
	}

	/** Un minuto después del final se cierra la puerta y se saca a quien siga dentro. */
	private static void despues(MinecraftServer server) {
		if (cierreFinal >= 0L) {
			if (tick < cierreFinal)
				return;
			cierreFinal = -1L;
			puertaPedida = Boolean.FALSE;
			intentarPuerta(server, bunkerFinal, puertaFinal);
		}
		if (tick % 20L != 0L)
			return;
		for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
			if (!p.isAlive() || p.isCreative() || p.isSpectator())
				continue;
			Bunkers.Bunker b = Bunkers.interiorDe(p.serverLevel(), p.blockPosition());
			if (b != null)
				Bunkers.sacar(p, b);
		}
	}

	private static void acabarEclipse(MinecraftServer server) {
		if (eclipseHastaBunker) {
			eclipseHastaBunker = false;
			ComandoEclipse.terminarMomento(server);
		}
	}

	private static void avisarSiguiente(MinecraftServer server, int segundos) {
		Red.enviarATodos(server, new ReviilPayload("temporizador", segundos, Component.empty(), Component.empty()));
	}

	private static void eclipse(MinecraftServer server) {
		if (eclipseEn < 0L || tick < eclipseEn || Mensajes.ocupado())
			return;
		eclipseEn = -1L;
		int paso = pasoEclipse;
		int[] t = tiemposEclipse();
		String quien = eclipseQuien;
		String titulo = TITULO_ECLIPSE_2;
		String texto = Momentos.ECLIPSE_TEXTOS[paso - 1];
		Runnable alSalir;
		if (paso == 2) {
			alSalir = () -> {
				pasoEclipse = 3;
				eclipseEn = tick + (long) t[1] * 20L;
			};
		} else if (paso == 3) {
			alSalir = () -> {
				ComandoEclipse.empezarMomento(server, 86400);
				eclipseHastaBunker = true;
				pasoEclipse = 4;
				eclipseEn = tick + (long) t[2] * 20L;
			};
		} else {
			texto = conCoordenadas(texto);
			alSalir = () -> {
				pasoEclipse = 0;
				if (bunker != null) {
					activo = "bunker_rayo";
					encerrados = (long) t[4] * 20L;
					cierreFinal = -1L;
					Bunkers.mostrarMarcador(server, bunker);
					cierra = tick + (long) t[3] * 20L;
					abre = -1L;
					cerrada = false;
					ponerPuerta(server, true);
					avisarSiguiente(server, t[3]);
					eclipseElegido = true;
				} else {
					acabarEclipse(server);
					MomentosReviil.terminarUno(server, "eclipse");
					MomentoReviil.terminar(server);
				}
			};
		}
		Ruleta.girar(server, RuletaColor.ROJO, quien, false);
		Mensajes.programar(RuletaColor.ROJO, quien, titulo, texto, -1, null, null, alSalir, false);
	}

	static void alTerminarReviil(MinecraftServer server) {
		eclipseEn = -1L;
		pasoEclipse = 0;
		eclipseElegido = false;
		acabarEclipse(server);
		if (activo != null)
			terminar(server);
	}

	@SubscribeEvent
	public static void alViajar(EntityTravelToDimensionEvent e) {
		if (netherFuera && e.getDimension() == Level.NETHER) {
			e.setCanceled(true);
			if (e.getEntity() instanceof ServerPlayer p)
				p.displayClientMessage(Component.literal("El Nether ha sido desactivado."), true);
		}
	}

	@SubscribeEvent
	public static void alApagar(ServerLifecycle.ServerStoppedEvent e) {
		bunkerFinal = null;
		puertaFinal = -1;
		cierreFinal = -1L;
		puertaPedida = null;
		bunker = null;
		puerta = -1;
		activo = null;
		cerrada = false;
		netherFuera = false;
		eclipseEn = -1L;
		pasoEclipse = 0;
		eclipseHastaBunker = false;
		eclipseElegido = false;
	}
}
