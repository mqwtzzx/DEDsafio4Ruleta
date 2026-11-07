package net.cloudruleta.agujero;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.Red;
import net.cloudruleta.compat.Puente;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.PlayerEvent;
import net.cloudruleta.compat.event.RegisterCommandsEvent;
import net.cloudruleta.compat.event.ServerLifecycle;
import net.cloudruleta.compat.event.TickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** /blackhole run | stop | agujero [pos] [tamaño N]: cielo de agujero negro (lleva la hora a medianoche) y agujeros. */
public final class ComandoAgujero {
	private static boolean activo;
	private static final List<AgujeroPayload.Agujero> AGUJEROS = new ArrayList<>();
	private static final Set<UUID> VIVOS = new HashSet<>();
	private static long tick;
	private static long horaInicio = -1L;
	private static long desdeHora;
	private static long cortadoEn = -1L;
	private static long horaCortada;
	private static long relojHora;
	private static final long MEDIANOCHE = 18000L;
	private static final float ENTRADA = 60.0F;

	public static boolean vivo(UUID id) {
		return VIVOS.contains(id);
	}

	private ComandoAgujero() {
	}

	@SubscribeEvent
	public static void alRegistrar(RegisterCommandsEvent e) {
		e.getDispatcher().register(Commands.literal("blackhole").requires(s -> s.hasPermission(2))
				.then(Commands.literal("run").executes(ctx -> empezar(ctx.getSource())))
				.then(Commands.literal("stop").executes(ctx -> parar(ctx.getSource())))
				.then(Commands.literal("agujero").executes(ctx -> agujero(ctx.getSource(), null, AgujeroNegroEntity.RADIO_DEFECTO))
						.then(tamano("tamaño", null))
						.then(tamano("tamano", null))
						.then(Commands.argument("pos", Vec3Argument.vec3()).executes(ctx -> agujero(ctx.getSource(), Vec3Argument.getVec3(ctx, "pos"), AgujeroNegroEntity.RADIO_DEFECTO))
								.then(tamano("tamaño", "pos"))
								.then(tamano("tamano", "pos")))));
	}

	private static int empezar(CommandSourceStack fuente) {
		if (activo) {
			fuente.sendFailure(Component.literal("El agujero negro ya está en marcha."));
			return 0;
		}
		activo = true;
		horaInicio = fuente.getServer().overworld().getDayTime();
		desdeHora = relojHora;
		cortadoEn = -1L;
		enviarATodos(fuente.getServer());
		fuente.sendSuccess(() -> Component.literal("Agujero negro: el cielo ha cambiado. Usa /blackhole agujero para que aparezca uno."), true);
		return 1;
	}

	private static int parar(CommandSourceStack fuente) {
		if (!activo && AGUJEROS.isEmpty()) {
			fuente.sendFailure(Component.literal("No hay ningún agujero negro en marcha."));
			return 0;
		}
		activo = false;
		AGUJEROS.clear();
		quitarAgujeros(fuente.getServer());
		if (horaInicio >= 0L) {
			cortadoEn = relojHora;
			horaCortada = fuente.getServer().overworld().getDayTime();
		}
		enviarATodos(fuente.getServer());
		fuente.sendSuccess(() -> Component.literal("Agujero negro detenido."), true);
		return 1;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> tamano(String palabra, String pos) {
		return Commands.literal(palabra).then(Commands.argument("tam", FloatArgumentType.floatArg(AgujeroNegroEntity.RADIO_MIN, AgujeroNegroEntity.RADIO_MAX))
				.executes(ctx -> agujero(ctx.getSource(), pos == null ? null : Vec3Argument.getVec3(ctx, pos), FloatArgumentType.getFloat(ctx, "tam"))));
	}

	private static int agujero(CommandSourceStack fuente, Vec3 sitio, float radio) {
		if (sitio == null) {
			Vec3 ojos = fuente.getPosition();
			Vec3 mira = new Vec3(0.0D, 0.0D, 1.0D);
			if (fuente.getEntity() != null) {
				ojos = fuente.getEntity().getEyePosition();
				mira = fuente.getEntity().getLookAngle();
			}
			sitio = ojos.add(mira.scale((double) radio * 8.0D));
			double minimo = ojos.y + (double) radio * 2.2D;
			if (sitio.y < minimo)
				sitio = new Vec3(sitio.x, minimo, sitio.z);
		}
		Vec3 donde = sitio;
		ServerLevel mundo = fuente.getLevel();
		AgujeroNegroEntity agujero = AgujeroNegro.TIPO.get().create(mundo);
		if (agujero == null) {
			fuente.sendFailure(Component.literal("No se pudo crear el agujero negro."));
			return 0;
		}
		agujero.fijarRadio(radio);
		agujero.moveTo(donde.x, donde.y, donde.z, 0.0F, 0.0F);
		mundo.addFreshEntity(agujero);
		VIVOS.add(agujero.getUUID());
		AGUJEROS.add(new AgujeroPayload.Agujero(donde, agujero.radio() > 0.0D ? (float) agujero.radio() : radio, mundo.dimension().location().toString()));
		enviarATodos(fuente.getServer());
		fuente.sendSuccess(() -> Component.literal(String.format("Ha aparecido un agujero negro (tamaño %.0f) en %.0f, %.0f, %.0f.", radio, donde.x, donde.y, donde.z)), true);
		return 1;
	}

	public static void cielo(MinecraftServer server, boolean poner) {
		if (poner && !activo) {
			activo = true;
			horaInicio = server.overworld().getDayTime();
			desdeHora = relojHora;
			cortadoEn = -1L;
		} else if (!poner && activo) {
			activo = false;
			if (horaInicio >= 0L) {
				cortadoEn = relojHora;
				horaCortada = server.overworld().getDayTime();
			}
		}
		enviarATodos(server);
	}

	public static void crearAgujero(ServerLevel mundo, Vec3 donde, float radio) {
		AgujeroNegroEntity agujero = AgujeroNegro.TIPO.get().create(mundo);
		if (agujero == null)
			return;
		agujero.fijarRadio(radio);
		agujero.moveTo(donde.x, donde.y, donde.z, 0.0F, 0.0F);
		mundo.addFreshEntity(agujero);
		VIVOS.add(agujero.getUUID());
		AGUJEROS.add(new AgujeroPayload.Agujero(donde, agujero.radio() > 0.0D ? (float) agujero.radio() : radio, mundo.dimension().location().toString()));
		enviarATodos(mundo.getServer());
	}

	public static void quitarTodos(MinecraftServer server) {
		AGUJEROS.clear();
		quitarAgujeros(server);
		enviarATodos(server);
	}

	private static void quitarAgujeros(MinecraftServer server) {
		VIVOS.clear();
		for (ServerLevel mundo : server.getAllLevels()) {
			List<Entity> lista = new ArrayList<>();
			for (Entity e : mundo.getAllEntities())
				if (e instanceof AgujeroNegroEntity)
					lista.add(e);
			for (Entity e : lista)
				e.discard();
		}
	}

	private static long corto(long de, long a) {
		long d = Math.floorMod(a - de, 24000L);
		return d > 12000L ? d - 24000L : d;
	}

	private static float suave(float x) {
		x = Math.max(0.0F, Math.min(1.0F, x));
		return x * x * (3.0F - 2.0F * x);
	}

	private static void hora(MinecraftServer server) {
		if (horaInicio < 0L)
			return;
		ServerLevel mundo = server.overworld();
		boolean ciclo = mundo.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
		boolean termina = false;
		long hora;
		if (cortadoEn >= 0L) {
			long destino = ciclo ? horaInicio + (relojHora - desdeHora) : horaInicio;
			float u = suave((float) (relojHora - cortadoEn) / ENTRADA);
			termina = relojHora - cortadoEn >= 60L;
			hora = termina ? destino : horaCortada + (long) Math.round((float) corto(horaCortada, destino) * u);
		} else {
			long hasta = horaInicio + corto(horaInicio, MEDIANOCHE);
			hora = horaInicio + (long) Math.round((float) (hasta - horaInicio) * suave((float) (relojHora - desdeHora) / ENTRADA));
		}
		mundo.setDayTime(hora);
		if (termina) {
			horaInicio = -1L;
			cortadoEn = -1L;
		}
		server.getPlayerList().broadcastAll(new ClientboundSetTimePacket(mundo.getGameTime(), hora, ciclo && termina), Level.OVERWORLD);
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ServerTickEvent e) {
		if (e.phase != TickEvent.Phase.END)
			return;
		relojHora++;
		MinecraftServer server = Puente.servidor();
		if (server == null)
			return;
		hora(server);
	}

	private static AgujeroPayload estado() {
		return new AgujeroPayload(activo, new ArrayList<>(AGUJEROS));
	}

	private static void enviarATodos(MinecraftServer server) {
		Red.enviarATodos(server, estado());
	}

	@SubscribeEvent
	public static void alEntrar(PlayerEvent.PlayerLoggedInEvent e) {
		if ((activo || !AGUJEROS.isEmpty()) && e.getEntity() instanceof ServerPlayer p)
			Red.enviar(p, estado());
	}

	@SubscribeEvent
	public static void alApagar(ServerLifecycle.ServerStoppedEvent e) {
		activo = false;
		horaInicio = -1L;
		cortadoEn = -1L;
		AGUJEROS.clear();
		VIVOS.clear();
	}
}
