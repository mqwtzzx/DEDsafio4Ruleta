package net.cloudruleta;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.PlayerEvent;
import net.cloudruleta.compat.event.ServerLifecycle.ServerStoppedEvent;
import net.cloudruleta.compat.event.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/** Momentos Reviil elegidos en el panel (armados) y en efecto: gas, creepers, luna, regeneración… */
public final class MomentosReviil {
	private static final int CAPA_ARRIBA = 250;
	private static final int CAPA_ABAJO = 0;
	private static final float DANO_GAS = 2.0F;
	private static final float PROB_CREEPER = 0.013333334F;
	private static final int TICKS_CEGUERA = 300;
	private static final Set<String> ACTIVOS = new LinkedHashSet<>();
	private static final Map<String, Integer> SEGUNDOS = new HashMap<>();
	private static final Map<String, Integer> SEGUNDOS2 = new HashMap<>();
	private static final Map<String, String> EXTRA = new HashMap<>();
	private static final Map<String, Long> DESDE = new HashMap<>();
	private static Boolean regeneracionAntes;
	private static long tick;
	private static final List<String> SINREGEN_IDS = List.of(Momentos.SIN_REGENERACION);
	private static final long TICKS_RULETA_LUNA = 280L;
	private static long lunaEn = -1L;
	private static String lunaQuien;

	static String extra(String id) {
		return EXTRA.getOrDefault(id, "");
	}

	private MomentosReviil() {
	}

	public static boolean activo(String id) {
		return ACTIVOS.contains(id);
	}

	private static boolean enEfecto(String id) {
		return DESDE.containsKey(id);
	}

	static void recibir(ServerPlayer jugador, MomentoPayload m) {
		if (jugador == null || m == null || jugador.getServer() == null)
			return;
		if (!jugador.hasPermissions(2)) {
			jugador.sendSystemMessage(Component.literal("Solo los operadores pueden cambiar los Momentos Reviil."));
			return;
		}
		Momentos.Momento momento = Momentos.porId(m.id());
		if (momento == null)
			return;
		MinecraftServer server = jugador.getServer();
		if (m.activar()) {
			if (Momentos.CREEPERS_NEGATIVAS.equals(m.id()) && !enEfecto(Momentos.GAS_ABAJO)) {
				jugador.sendSystemMessage(Component.literal("Bloqueado: primero tiene que estar en curso el Momento Reviil del Gas tóxico (bajar a capas negativas).").withStyle(ChatFormatting.RED));
				sincronizar(server);
				return;
			}
			if (Momentos.ECLIPSE.equals(m.id()) && !Ruleta.cambioActivo("herobrine")) {
				jugador.sendSystemMessage(Component.literal("Bloqueado: primero activa el cambio de dificultad Herobrine.").withStyle(ChatFormatting.RED));
				sincronizar(server);
				return;
			}
			if (Momentos.ECLIPSE.equals(m.id()) ? !ACTIVOS.isEmpty() : ACTIVOS.contains(Momentos.ECLIPSE) || ComandoEclipse.enMarcha()) {
				jugador.sendSystemMessage(Component.literal("El Eclipse va solo: no se puede juntar con otros Momentos Reviil, ni elegido ni en marcha.").withStyle(ChatFormatting.RED));
				sincronizar(server);
				return;
			}
			for (String otro : ACTIVOS) {
				if (Momentos.chocan(otro, m.id())) {
					jugador.sendSystemMessage(Component.literal("No se puede: se contradice con «" + Momentos.porId(otro).titulo() + "».").withStyle(ChatFormatting.RED));
					sincronizar(server);
					return;
				}
			}
			ACTIVOS.add(m.id());
			SEGUNDOS.put(m.id(), Math.max(0, Math.min(86400, m.segundos())));
			SEGUNDOS2.put(m.id(), Math.max(0, Math.min(86400, m.segundos2())));
			EXTRA.put(m.id(), m.extra() == null ? "" : m.extra());
		} else {
			if (Mensajes.ocupado() && ACTIVOS.contains(m.id())) {
				jugador.sendSystemMessage(Component.literal("La cloudruleta ya está lanzada: ese Momento Reviil solo se quita con DETENER RULETA o cuando termine.").withStyle(ChatFormatting.RED));
				sincronizar(server);
				return;
			}
			if (Momentos.ECLIPSE.equals(m.id()) && enEfecto(Momentos.ECLIPSE)) {
				jugador.sendSystemMessage(Component.literal("El Eclipse Solar ya está lanzado: se libera cuando acaben sus 4 Momentos Reviil (o con DETENER RULETA).").withStyle(ChatFormatting.RED));
				sincronizar(server);
				return;
			}
			if (!Momentos.LUNA.equals(m.id()) && enEfecto(m.id())) {
				jugador.sendSystemMessage(Component.literal("Ese Momento Reviil ya está lanzado: queda seleccionado hasta que termine.").withStyle(ChatFormatting.RED));
				sincronizar(server);
				return;
			}
			if (Momentos.LUNA.equals(m.id()) && enEfecto(Momentos.LUNA)) {
				jugador.sendSystemMessage(Component.literal("La Maldición de la Luna ya está lanzada: se quita sola cuando alguien mire la Luna.").withStyle(ChatFormatting.RED));
				sincronizar(server);
				return;
			}
			desactivar(server, m.id());
		}
		sincronizar(server);
	}

	static void terminarUno(MinecraftServer server, String id) {
		desactivar(server, id);
		sincronizar(server);
	}

	static void antesDeLanzar(ServerPlayer admin) {
		MomentosBunker.preparar(admin, new ArrayList<>(ACTIVOS));
	}

	private static void desactivar(MinecraftServer server, String id) {
		ACTIVOS.remove(id);
		SEGUNDOS.remove(id);
		SEGUNDOS2.remove(id);
		EXTRA.remove(id);
		boolean estaba = DESDE.remove(id) != null;
		if (estaba && (Momentos.BUNKER_RAYO.equals(id) || Momentos.BUNKER_AGUJEROS.equals(id)))
			MomentosBunker.terminar(server);
		if (SINREGEN_IDS.stream().noneMatch(MomentosReviil::enEfecto))
			devolverRegeneracion(server);
	}

	static void alTerminar(MinecraftServer server) {
		MomentosBunker.alTerminarReviil(server);
		ACTIVOS.clear();
		SEGUNDOS.clear();
		SEGUNDOS2.clear();
		EXTRA.clear();
		DESDE.clear();
		devolverRegeneracion(server);
		sincronizar(server);
	}

	/** Título y texto de los momentos armados (para el anuncio), o null si no hay ninguno. */
	static String[] textoArmados() {
		StringBuilder t = new StringBuilder();
		StringBuilder x = new StringBuilder();
		for (String id : ACTIVOS) {
			Momentos.Momento m = Momentos.porId(id);
			if (m == null)
				continue;
			if (x.length() > 0)
				x.append(" ");
			x.append(m.texto());
			t = new StringBuilder(Momentos.TITULO);
		}
		return x.length() == 0 ? null : new String[]{t.toString(), MomentosBunker.conCoordenadas(x.toString())};
	}

	static void lanzarElegidos(MinecraftServer server) {
		if (!MomentoReviil.enMarcha())
			return;
		boolean cambio = false;
		for (String id : new ArrayList<>(ACTIVOS)) {
			if (!DESDE.containsKey(id)) {
				DESDE.put(id, tick);
				empezar(server, id);
				cambio = true;
			}
		}
		if (cambio)
			sincronizar(server);
	}

	static boolean nadaEnEfecto() {
		return DESDE.isEmpty();
	}

	static boolean hayArmados() {
		return !ACTIVOS.isEmpty();
	}

	static boolean soloLuna() {
		if (!DESDE.containsKey(Momentos.LUNA))
			return false;
		for (String id : DESDE.keySet())
			if (!Momentos.LUNA.equals(id))
				return false;
		return true;
	}

	static boolean sinCielo() {
		return ACTIVOS.contains(Momentos.LUNA) || ACTIVOS.contains(Momentos.ECLIPSE) || ACTIVOS.contains(Momentos.BUNKER_AGUJEROS);
	}

	static void sincronizar(MinecraftServer server) {
		Red.enviarATodos(server, payload());
	}

	private static ReviilPayload payload() {
		StringJoiner armados = new StringJoiner(",");
		Objects.requireNonNull(armados);
		ACTIVOS.forEach(armados::add);
		StringJoiner efecto = new StringJoiner(",");
		Objects.requireNonNull(efecto);
		DESDE.keySet().forEach(efecto::add);
		return new ReviilPayload("momentos", 0, Component.literal(armados.toString()), Component.literal(efecto.toString()));
	}

	@SubscribeEvent
	public static void alEntrar(PlayerEvent.PlayerLoggedInEvent e) {
		if (e.getEntity() instanceof ServerPlayer p)
			Red.enviar(p, payload());
	}

	@SubscribeEvent
	public static void alApagar(ServerStoppedEvent e) {
		ACTIVOS.clear();
		SEGUNDOS.clear();
		SEGUNDOS2.clear();
		EXTRA.clear();
		DESDE.clear();
		regeneracionAntes = null;
		lunaEn = -1L;
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ServerTickEvent e) {
		if (e.phase != TickEvent.Phase.END)
			return;
		MinecraftServer server = e.getServer();
		if (server == null)
			return;
		tick++;
		cloudruletaLuna(server);
		MomentosBunker.tick(server);
		if (!MomentoReviil.enMarcha()) {
			if (!DESDE.isEmpty()) {
				DESDE.clear();
				devolverRegeneracion(server);
				sincronizar(server);
			}
			return;
		}
		if (DESDE.isEmpty())
			return;
		for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
			if (!p.isAlive() || p.isCreative() || p.isSpectator())
				continue;
			if (tick % 20L == 0L) {
				gas(p);
				creepers(p);
			}
			if (tick % 5L == 0L && enEfecto(Momentos.LUNA))
				luna(server, p);
		}
	}

	private static void empezar(MinecraftServer server, String id) {
		Momentos.Momento m = Momentos.porId(id);
		if (m == null)
			return;
		int segundos = SEGUNDOS.getOrDefault(id, 0);
		if (m.temporizador() && segundos > 0)
			Red.enviarATodos(server, new ReviilPayload("temporizador", segundos, Component.empty(), Component.empty()));
		MomentosBunker.empezar(server, id, segundos, SEGUNDOS2.getOrDefault(id, 0));
		if (Momentos.CEGUERA_CREEPERS.equals(id)) {
			for (ServerPlayer p : server.getPlayerList().getPlayers())
				p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, TICKS_CEGUERA, 0, false, false, true));
		}
		if (Momentos.SIN_REGENERACION.equals(id)) {
			GameRules.BooleanValue regla = server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION);
			if (regeneracionAntes == null)
				regeneracionAntes = regla.get();
			regla.set(false, server);
		}
	}

	private static void devolverRegeneracion(MinecraftServer server) {
		if (regeneracionAntes != null && server != null) {
			server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(regeneracionAntes, server);
			regeneracionAntes = null;
		}
	}

	private static boolean gasListo(String id) {
		Long desde = DESDE.get(id);
		return desde != null && tick >= desde + (long) SEGUNDOS.getOrDefault(id, 0) * 20L;
	}

	private static void gas(ServerPlayer p) {
		boolean arriba = gasListo(Momentos.GAS_ARRIBA);
		boolean abajo = gasListo(Momentos.GAS_ABAJO) || gasListo(Momentos.CREEPERS_NEGATIVAS);
		double y = p.getY();
		if (arriba && y < CAPA_ARRIBA || abajo && y >= CAPA_ABAJO) {
			p.invulnerableTime = 0;
			p.hurt(Ruleta.fuenteLetal(p, "cloudruleta.gas", " murió por el Gas Tóxico de Reviil"), DANO_GAS);
		}
	}

	private static void creepers(ServerPlayer p) {
		boolean hay = false;
		for (String id : DESDE.keySet()) {
			if (Momentos.conCreepers(id)) {
				hay = true;
				break;
			}
		}
		ServerLevel nivel = p.serverLevel();
		if (!hay || nivel.getDifficulty() == Difficulty.PEACEFUL || !(p.getRandom().nextFloat() < PROB_CREEPER))
			return;
		Creeper c = EntityType.CREEPER.create(nivel);
		if (c == null)
			return;
		c.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0.0F);
		c.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.EVENT, null);
		// creeper eléctrico con 1 de vida
		CompoundTag t = new CompoundTag();
		c.addAdditionalSaveData(t);
		t.putBoolean("powered", true);
		c.readAdditionalSaveData(t);
		c.setHealth(1.0F);
		nivel.addFreshEntity(c);
	}

	private static void luna(MinecraftServer server, ServerPlayer p) {
		ServerLevel nivel = p.serverLevel();
		if (nivel.dimension() != Level.OVERWORLD || p.hasEffect(MobEffects.BLINDNESS))
			return;
		float angulo = nivel.getTimeOfDay(1.0F) * 6.2831855F;
		Vec3 luna = new Vec3(Mth.sin(angulo), -Mth.cos(angulo), 0.0D);
		if (luna.y < 0.05D || !(p.getViewVector(1.0F).dot(luna) >= 0.985D))
			return;
		Vec3 ojos = p.getEyePosition();
		BlockHitResult golpe = nivel.clip(new ClipContext(ojos, ojos.add(luna.scale(128.0D)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p));
		if (golpe.getType() != HitResult.Type.MISS)
			return;
		p.hurt(Ruleta.fuenteLetal(p, "cloudruleta.luna", " miró la Luna maldita de Reviil"), Float.MAX_VALUE);
		desactivar(server, Momentos.LUNA);
		sincronizar(server);
		lunaQuien = p.getGameProfile().getName();
		lunaEn = tick + TICKS_RULETA_LUNA;
	}

	/** Tras morir alguien por la luna, una cloudruleta verde avisa de que ya es seguro mirarla. */
	private static void cloudruletaLuna(MinecraftServer server) {
		if (lunaEn < 0L || tick < lunaEn || Mensajes.ocupado())
			return;
		lunaEn = -1L;
		String nombre = lunaQuien;
		Ruleta.girar(server, RuletaColor.VERDE, nombre, false);
		Mensajes.programar(RuletaColor.VERDE, nombre, null, "El jugador {jugador} ha mirado la luna, ahora ya es seguro volver a mirar la luna.", -1, null, null, () -> {
			if (MomentoReviil.enMarcha() && !MomentoReviil.enMarchaConCielo() && DESDE.isEmpty() && ACTIVOS.isEmpty())
				MomentoReviil.terminar(server);
		}, false);
	}
}
