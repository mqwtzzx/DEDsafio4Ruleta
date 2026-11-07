package net.cloudruleta;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.cloudruleta.compat.Puente;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.PlayerEvent;
import net.cloudruleta.compat.event.RegisterCommandsEvent;
import net.cloudruleta.compat.event.ServerLifecycle;
import net.cloudruleta.compat.event.TickEvent;
import net.cloudruleta.phora.MomentoPhora;

/** /eclipse [segundos] | detener: eclipse solar; lleva la hora a la medianoche de luna nueva y la devuelve después. */
public final class ComandoEclipse {
	private static final int TOTALIDAD = 30;
	public static final float RESTO = 59.0F;
	private static long tick;
	private static long desde = -1L;
	private static int totalidad;
	private static final float HASTA_TOTAL = 29.5F;
	private static final float SALIDA = 29.5F;
	private static final long MEDIANOCHE = 18000L;
	private static long horaInicio = -1L;
	private static long noche;
	private static long cortadoEn = -1L;
	private static long horaCortada;
	private static long desdeHora;

	private ComandoEclipse() {
	}

	public static boolean enMarcha() {
		return desde >= 0L;
	}

	@SubscribeEvent
	public static void alRegistrar(RegisterCommandsEvent e) {
		e.getDispatcher().register(Commands.literal("eclipse").requires(s -> s.hasPermission(2))
				.executes(ctx -> empezar(ctx.getSource().getServer(), TOTALIDAD, ctx.getSource()))
				.then(Commands.literal("detener").executes(ctx -> {
					if (desde < 0L) {
						ctx.getSource().sendFailure(Component.literal("No hay ningún eclipse en marcha."));
						return 0;
					}
					terminar(ctx.getSource().getServer());
					ctx.getSource().sendSuccess(() -> Component.literal("Eclipse detenido."), true);
					return 1;
				}))
				.then(Commands.argument("segundos", IntegerArgumentType.integer(5, 600))
						.executes(ctx -> empezar(ctx.getSource().getServer(), IntegerArgumentType.getInteger(ctx, "segundos"), ctx.getSource()))));
	}

	private static int empezar(MinecraftServer server, int segundos, CommandSourceStack fuente) {
		if (MomentoReviil.enMarcha() || MomentoPhora.enMarcha()) {
			fuente.sendFailure(Component.literal("No se puede lanzar el eclipse con un Momento Reviil o un Momento Phora en curso."));
			return 0;
		}
		ServerLevel mundo = server.overworld();
		long base = horaInicio >= 0L ? horaNormal() : mundo.getDayTime();
		desde = tick;
		totalidad = segundos;
		cortadoEn = -1L;
		horaInicio = base;
		desdeHora = tick;
		// la próxima noche de luna nueva (fase 4 del ciclo de 8 días) a medianoche
		long dia = Math.floorDiv(base, 24000L);
		long hasta = (4L - Math.floorMod(dia, 8L) + 8L) % 8L;
		noche = (dia + hasta) * 24000L + MEDIANOCHE;
		if (noche <= base)
			noche += 192000L;
		Red.enviarATodos(server, mensaje(segundos, 0));
		fuente.sendSuccess(() -> Component.literal("Eclipse solar: " + segundos + " s de totalidad."), true);
		return 1;
	}

	public static void empezarMomento(MinecraftServer server, int segundosTotalidad) {
		ServerLevel mundo = server.overworld();
		long base = horaInicio >= 0L ? horaNormal() : mundo.getDayTime();
		desde = tick;
		totalidad = segundosTotalidad;
		cortadoEn = -1L;
		horaInicio = base;
		desdeHora = tick;
		long dia = Math.floorDiv(base, 24000L);
		long hasta = (4L - Math.floorMod(dia, 8L) + 8L) % 8L;
		noche = (dia + hasta) * 24000L + MEDIANOCHE;
		if (noche <= base)
			noche += 192000L;
		Red.enviarATodos(server, mensaje(segundosTotalidad, 0));
	}

	public static void terminarMomento(MinecraftServer server) {
		if (desde >= 0L)
			terminar(server);
	}

	private static void terminar(MinecraftServer server) {
		desde = -1L;
		Red.enviarATodos(server, mensaje(-1, 0));
		if (horaInicio >= 0L) {
			cortadoEn = tick;
			horaCortada = server.overworld().getDayTime();
		}
	}

	private static long corto(long de, long a) {
		long d = Math.floorMod(a - de, 24000L);
		return d > 12000L ? d - 24000L : d;
	}

	private static long horaNormal() {
		return horaInicio + (tick - desdeHora);
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
			float u = suave((float) (tick - cortadoEn) / 80.0F);
			long destino = horaNormal();
			termina = tick - cortadoEn >= 80L;
			hora = termina ? destino : horaCortada + (long) Math.round((float) corto(horaCortada, destino) * u);
		} else {
			float s = (float) (tick - desdeHora) / 20.0F;
			float totalFin = HASTA_TOTAL + (float) totalidad;
			if (s < HASTA_TOTAL) {
				long recorrido = corto(horaInicio, noche);
				long saltoDias = noche - horaInicio - recorrido;
				hora = horaInicio + saltoDias + (long) Math.round((float) recorrido * suave(s / HASTA_TOTAL));
			} else if (s < totalFin) {
				hora = noche;
			} else {
				long destino = horaNormal();
				termina = s >= totalFin + SALIDA;
				hora = termina ? destino : noche + (long) Math.round((float) corto(noche, destino) * suave((s - totalFin) / SALIDA));
			}
		}
		mundo.setDayTime(hora);
		if (termina) {
			horaInicio = -1L;
			cortadoEn = -1L;
		}
		server.getPlayerList().broadcastAll(new ClientboundSetTimePacket(mundo.getGameTime(), hora, ciclo && termina), Level.OVERWORLD);
	}

	private static ReviilPayload mensaje(int segundos, int lleva) {
		return new ReviilPayload("eclipse", segundos, Component.literal(Integer.toString(lleva)), Component.empty());
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ServerTickEvent e) {
		if (e.phase != TickEvent.Phase.END)
			return;
		tick++;
		MinecraftServer server = Puente.servidor();
		if (server != null)
			hora(server);
		if (desde >= 0L && tick - desde > (long) (((float) totalidad + RESTO) * 20.0F) + 20L)
			desde = -1L;
	}

	@SubscribeEvent
	public static void alEntrar(PlayerEvent.PlayerLoggedInEvent e) {
		if (desde >= 0L && e.getEntity() instanceof ServerPlayer p)
			Red.enviar(p, mensaje(totalidad, (int) ((tick - desde) / 20L)));
	}

	@SubscribeEvent
	public static void alApagar(ServerLifecycle.ServerStoppedEvent e) {
		desde = -1L;
		horaInicio = -1L;
		cortadoEn = -1L;
	}
}
