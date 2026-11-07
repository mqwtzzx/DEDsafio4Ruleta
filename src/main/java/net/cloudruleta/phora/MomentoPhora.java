package net.cloudruleta.phora;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.ComandoEclipse;
import net.cloudruleta.MomentoReviil;
import net.cloudruleta.Red;
import net.cloudruleta.ReviilPayload;
import net.cloudruleta.compat.Puente;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.PlayerEvent;
import net.cloudruleta.compat.event.RegisterCommandsEvent;
import net.cloudruleta.compat.event.ServerLifecycle;
import net.cloudruleta.compat.event.TickEvent;

import java.util.ArrayList;
import java.util.List;

/** Momento Phora: lluvia de meteoritos cerca de los jugadores (/phora lluvia | detener | palo). */
public final class MomentoPhora {
	private static final int MIN_ESPERA = 120;
	private static final int MAX_ESPERA = 320;
	private static final double SEPARACION = 110.0D;
	private static final List<double[]> RECIENTES = new ArrayList<>();
	private static long siguiente;
	private static final long ESPERA_VOZ = 100L;
	private static long tick;
	private static long desde = -1L;
	private static long hasta = -1L;

	private MomentoPhora() {
	}

	public static boolean enMarcha() {
		return desde >= 0L;
	}

	public static void empezar(MinecraftServer server, int segundos) {
		if (desde < 0L) {
			desde = tick;
			siguiente = 0L;
			RECIENTES.clear();
			Red.enviarATodos(server, mensaje(0));
		}
		hasta = segundos > 0 ? tick + (long) segundos * 20L : -1L;
	}

	public static void terminar(MinecraftServer server) {
		if (desde < 0L)
			return;
		desde = -1L;
		hasta = -1L;
		Red.enviarATodos(server, mensaje(-1));
	}

	private static ReviilPayload mensaje(int valor) {
		return new ReviilPayload("phora", valor, Component.empty(), Component.empty());
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ServerTickEvent e) {
		if (e.phase != TickEvent.Phase.END)
			return;
		tick++;
		Crater.avanzar();
		MinecraftServer server = Puente.servidor(); // ServerLifecycleHooks.getCurrentServer()
		if (server == null || desde < 0L)
			return;
		if (hasta >= 0L && tick >= hasta) {
			terminar(server);
			return;
		}
		if (tick - desde < ESPERA_VOZ || tick < siguiente)
			return;
		List<ServerPlayer> jugadores = new ArrayList<>();
		for (ServerPlayer p : server.getPlayerList().getPlayers())
			if (!p.isSpectator() && p.level().dimensionType().hasSkyLight())
				jugadores.add(p);
		if (jugadores.isEmpty())
			return;
		ServerPlayer p = jugadores.get(server.overworld().random.nextInt(jugadores.size()));
		RandomSource r = p.getRandom();
		siguiente = tick + MIN_ESPERA + (long) r.nextInt(MAX_ESPERA - MIN_ESPERA);
		ServerLevel nivel = p.serverLevel();
		int alcance = Math.max(64, server.getPlayerList().getSimulationDistance() * 16 - 20);
		RECIENTES.removeIf(q -> tick - (long) q[3] > 3600L);
		for (int intento = 0; intento < 12; intento++) {
			double ang = r.nextDouble() * Math.PI * 2.0D;
			double dist = 45.0D + r.nextDouble() * ((double) alcance - 45.0D);
			int x = (int) Math.floor(p.getX() + Math.cos(ang) * dist);
			int z = (int) Math.floor(p.getZ() + Math.sin(ang) * dist);
			BlockPos donde = new BlockPos(x, p.getBlockY(), z);
			if (!nivel.isPositionEntityTicking(donde))
				continue;
			boolean malo = false;
			for (ServerPlayer otro : jugadores) {
				if (otro.level() == nivel && otro.position().distanceToSqr(x, otro.getY(), z) < 2025.0D) {
					malo = true;
					break;
				}
			}
			for (double[] q : RECIENTES) {
				double dx = q[0] - (double) x;
				double dz = q[2] - (double) z;
				if (dx * dx + dz * dz < SEPARACION * SEPARACION) {
					malo = true;
					break;
				}
			}
			if (malo)
				continue;
			int y = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
			MeteoritoEntity.lanzar(nivel, new Vec3((double) x + 0.5D, y, (double) z + 0.5D), r, p.position());
			RECIENTES.add(new double[]{x, y, z, (double) tick});
			return;
		}
	}

	@SubscribeEvent
	public static void alRegistrar(RegisterCommandsEvent e) {
		e.getDispatcher().register(Commands.literal("phora").requires(s -> s.hasPermission(2))
				.then(Commands.literal("lluvia").executes(ctx -> {
					if (ComandoEclipse.enMarcha()) {
						ctx.getSource().sendFailure(Component.literal("No se puede lanzar el Momento Phora con un eclipse en curso."));
						return 0;
					}
					if (MomentoReviil.enMarcha()) {
						ctx.getSource().sendFailure(Component.literal("No se puede lanzar el Momento Phora con un Momento Reviil en curso."));
						return 0;
					}
					empezar(ctx.getSource().getServer(), -1);
					ctx.getSource().sendSuccess(() -> Component.literal("Momento Phora: empieza la lluvia de meteoritos (/phora detener para pararla)."), true);
					return 1;
				}))
				.then(Commands.literal("detener").executes(ctx -> {
					if (!enMarcha()) {
						ctx.getSource().sendFailure(Component.literal("No hay ningún Momento Phora en marcha."));
						return 0;
					}
					terminar(ctx.getSource().getServer());
					ctx.getSource().sendSuccess(() -> Component.literal("Momento Phora detenido."), true);
					return 1;
				}))
				.then(Commands.literal("palo").executes(ctx -> {
					ServerPlayer p = ctx.getSource().getPlayerOrException();
					ItemStack palo = new ItemStack(PhoraMod.PALO.get());
					if (!p.getInventory().add(palo))
						p.drop(palo, false);
					ctx.getSource().sendSuccess(() -> Component.literal("Palo de los Phora: clic derecho para hacer caer un meteorito donde miras."), false);
					return 1;
				})));
	}

	@SubscribeEvent
	public static void alEntrar(PlayerEvent.PlayerLoggedInEvent e) {
		if (desde >= 0L && e.getEntity() instanceof ServerPlayer p)
			Red.enviar(p, mensaje((int) ((tick - desde) / 20L)));
	}

	@SubscribeEvent
	public static void alApagar(ServerLifecycle.ServerStoppedEvent e) {
		desde = -1L;
		hasta = -1L;
		Crater.limpiar();
	}

	static List<String> nada() {
		return List.of();
	}
}
