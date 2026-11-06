package net.ruleta.herobrine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.ruleta.Ruleta;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.TickEvent;

/** Con el cambio «herobrine» activo, cada minuto hay un 25 % de que Herobrine aparezca cerca de cada jugador. */
public final class HerobrineAparicion {
	private static final int CADA = 1200;
	private static final float PROBABILIDAD = 0.25F;
	private static int tick;

	private HerobrineAparicion() {
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ServerTickEvent e) {
		if (e.phase != TickEvent.Phase.END || ++tick % CADA != 0)
			return;
		MinecraftServer server = e.getServer();
		if (server == null || !Ruleta.cambioActivoPublico("herobrine"))
			return;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (!p.isAlive() || p.isSpectator() || p.isCreative())
				continue;
			ServerLevel nivel = p.serverLevel();
			if (nivel.getDifficulty() == Difficulty.PEACEFUL || !(p.getRandom().nextFloat() < PROBABILIDAD))
				continue;
			if (!nivel.getEntitiesOfClass(HerobrineEntity.class, new AABB(p.blockPosition()).inflate(96.0D)).isEmpty())
				continue;
			aparecer(nivel, p);
		}
	}

	static boolean aparecer(ServerLevel nivel, ServerPlayer p) {
		for (int i = 0; i < 20; i++) {
			double ang = p.getRandom().nextDouble() * Math.PI * 2.0D;
			double r = 14.0D + p.getRandom().nextDouble() * 16.0D;
			int x = (int) Math.floor(p.getX() + Math.cos(ang) * r);
			int z = (int) Math.floor(p.getZ() + Math.sin(ang) * r);
			BlockPos pos = buscarSuelo(nivel, x, (int) p.getY(), z);
			if (pos == null)
				continue;
			HerobrineEntity h = Herobrine.HEROBRINE.get().create(nivel);
			if (h == null)
				return false;
			h.moveTo((double) pos.getX() + 0.5D, pos.getY(), (double) pos.getZ() + 0.5D, p.getRandom().nextFloat() * 360.0F, 0.0F);
			if (!nivel.noCollision(h))
				continue;
			h.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
			nivel.addFreshEntity(h);
			return true;
		}
		return false;
	}

	private static BlockPos buscarSuelo(ServerLevel nivel, int x, int y, int z) {
		for (int dy = 6; dy >= -8; dy--) {
			BlockPos pos = new BlockPos(x, y + dy, z);
			if (nivel.getBlockState(pos.below()).isSolid() && nivel.getBlockState(pos).isAir() && nivel.getBlockState(pos.above()).isAir())
				return pos;
		}
		int sup = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		BlockPos pos = new BlockPos(x, sup, z);
		return Math.abs(sup - y) < 24 && nivel.getBlockState(pos.below()).isSolid() ? pos : null;
	}
}
