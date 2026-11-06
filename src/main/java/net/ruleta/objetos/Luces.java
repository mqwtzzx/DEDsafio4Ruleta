package net.ruleta.objetos;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.PlayerEvent;
import net.ruleta.compat.event.ServerLifecycle;
import net.ruleta.compat.event.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bloques de luz invisibles delante del jugador con la linterna prendida (se quitan al soltarla). */
public final class Luces {
	private static final double[] DISTANCIAS = {3.0D, 7.0D};
	private static final Map<UUID, Object[]> PUESTAS = new HashMap<>();

	private Luces() {
	}

	static void alumbrar(ServerPlayer p, float intensidad) {
		ServerLevel nivel = p.serverLevel();
		float yaw = p.getYRot() * 0.017453292F;
		Vec3 frente = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
		Vec3 ojos = p.getEyePosition();
		int fuerte = Math.max(6, Math.min(15, Math.round(15.0F * intensidad)));
		List<Object[]> nuevas = new ArrayList<>();
		for (double dist : DISTANCIAS) {
			Vec3 fin = ojos.add(frente.scale(dist));
			BlockHitResult hit = nivel.clip(new ClipContext(ojos, fin, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
			BlockPos pos = hit.getType() == HitResult.Type.MISS ? BlockPos.containing(fin) : hit.getBlockPos().relative(hit.getDirection());
			int luz = dist > DISTANCIAS[0] ? Math.max(4, fuerte - 3) : fuerte;
			nuevas.add(new Object[]{pos.immutable(), luz});
			if (hit.getType() != HitResult.Type.MISS)
				break;
		}
		Object[] antes = PUESTAS.get(p.getUUID());
		if (antes != null && antes[0] == nivel.dimension() && iguales(antes[1], nuevas))
			return;
		List<Object[]> puestas = new ArrayList<>();
		for (Object[] n : nuevas) {
			BlockPos pos = (BlockPos) n[0];
			int luz = (Integer) n[1];
			BlockState aqui = nivel.getBlockState(pos);
			if (aqui.isAir() || aqui.is(Blocks.LIGHT)) {
				if (!aqui.is(Blocks.LIGHT) || aqui.getValue(LightBlock.LEVEL) != luz)
					nivel.setBlock(pos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, luz), 3);
				puestas.add(n);
			}
		}
		if (antes != null) {
			@SuppressWarnings("unchecked")
			List<Object[]> viejas = (List<Object[]>) antes[1];
			@SuppressWarnings("unchecked")
			ServerLevel nivelViejo = p.getServer().getLevel((ResourceKey<Level>) antes[0]);
			for (Object[] v : viejas) {
				BlockPos pos = (BlockPos) v[0];
				boolean sigue = antes[0] == nivel.dimension() && puestas.stream().anyMatch(n -> n[0].equals(pos));
				if (!sigue && nivelViejo != null && nivelViejo.getBlockState(pos).is(Blocks.LIGHT))
					nivelViejo.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			}
		}
		PUESTAS.put(p.getUUID(), new Object[]{nivel.dimension(), puestas});
	}

	private static boolean iguales(Object viejo, List<Object[]> nuevas) {
		@SuppressWarnings("unchecked")
		List<Object[]> v = (List<Object[]>) viejo;
		if (v.size() != nuevas.size())
			return false;
		for (int i = 0; i < v.size(); i++)
			if (!v.get(i)[0].equals(nuevas.get(i)[0]) || !v.get(i)[1].equals(nuevas.get(i)[1]))
				return false;
		return true;
	}

	static void quitar(MinecraftServer server, UUID id) {
		Object[] o = PUESTAS.remove(id);
		if (o == null || server == null)
			return;
		@SuppressWarnings("unchecked")
		ServerLevel nivel = server.getLevel((ResourceKey<Level>) o[0]);
		@SuppressWarnings("unchecked")
		List<Object[]> lista = (List<Object[]>) o[1];
		for (Object[] l : lista) {
			BlockPos pos = (BlockPos) l[0];
			if (nivel != null && nivel.getBlockState(pos).is(Blocks.LIGHT))
				nivel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
		}
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ServerTickEvent e) {
		if (e.phase != TickEvent.Phase.END || PUESTAS.isEmpty())
			return;
		MinecraftServer server = e.getServer();
		Iterator<UUID> it = new ArrayList<>(PUESTAS.keySet()).iterator();
		while (it.hasNext()) {
			UUID id = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null || !p.isAlive() || !prendidaEnMano(p))
				quitar(server, id);
		}
	}

	private static boolean prendidaEnMano(ServerPlayer p) {
		return luce(p.getMainHandItem()) || luce(p.getOffhandItem());
	}

	private static boolean luce(ItemStack s) {
		return s.is(Objetos.LINTERNA.get()) && Linterna.prendida(s);
	}

	@SubscribeEvent
	public static void alSalir(PlayerEvent.PlayerLoggedOutEvent e) {
		if (e.getEntity() instanceof ServerPlayer p)
			quitar(p.getServer(), p.getUUID());
	}

	@SubscribeEvent
	public static void alCambiarDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
		if (e.getEntity() instanceof ServerPlayer p)
			quitar(p.getServer(), p.getUUID());
	}

	@SubscribeEvent
	public static void alParar(ServerLifecycle.ServerStoppingEvent e) {
		for (UUID id : new ArrayList<>(PUESTAS.keySet()))
			quitar(e.getServer(), id);
	}
}
