package net.ruleta.creeperes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.ruleta.Ruleta;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.BlockEvent;
import net.ruleta.compat.event.MobSpawnEvent;
import net.ruleta.creeperrojo.CreeperRojo;
import net.ruleta.creeperrojo.CreeperRojoEntity;

/** Creepers de césped al romper hierba y creepers rojos en lugar de algunos creepers naturales. */
public final class MobsNuevos {
	private static final float PROB_PASTO = 0.08F;
	private static final float PROB_ROJO = 0.25F;

	private MobsNuevos() {
	}

	private static boolean esHierba(BlockState s) {
		Block b = s.getBlock();
		// Blocks.GRASS de 1.20.1 se llama SHORT_GRASS en 1.21.1 (es el mismo bloque, minecraft:short_grass)
		return b == Blocks.GRASS_BLOCK || b == Blocks.SHORT_GRASS || b == Blocks.TALL_GRASS || b == Blocks.FERN || b == Blocks.LARGE_FERN
				|| b == Blocks.DEAD_BUSH || b == Blocks.SWEET_BERRY_BUSH || b == Blocks.AZALEA || b == Blocks.FLOWERING_AZALEA || b == Blocks.SEAGRASS
				|| b == Blocks.TALL_SEAGRASS || b == Blocks.NETHER_SPROUTS || b == Blocks.CRIMSON_ROOTS || b == Blocks.WARPED_ROOTS
				|| b == Blocks.HANGING_ROOTS || b == Blocks.MOSS_CARPET;
	}

	@SubscribeEvent
	public static void alRomper(BlockEvent.BreakEvent e) {
		if (e.isCanceled() || e.getPlayer() == null || e.getPlayer().isCreative() || !(e.getLevel() instanceof ServerLevel nivel))
			return;
		if (!esHierba(e.getState()) || !Ruleta.cambioActivoPublico("mob-creeper-pasto") || !(nivel.random.nextFloat() < PROB_PASTO))
			return;
		CreeperCespedEntity c = Creeperes.CESPED.get().create(nivel);
		if (c != null) {
			c.moveTo((double) e.getPos().getX() + 0.5D, e.getPos().getY(), (double) e.getPos().getZ() + 0.5D, nivel.random.nextFloat() * 360.0F, 0.0F);
			c.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(e.getPos()), MobSpawnType.EVENT, null);
			nivel.addFreshEntity(c);
		}
	}

	@SubscribeEvent
	public static void alAparecer(MobSpawnEvent.FinalizeSpawn e) {
		if (e.getEntity().getType() != EntityType.CREEPER || e.isSpawnCancelled())
			return;
		MobSpawnType razon = e.getSpawnType();
		if (razon != MobSpawnType.NATURAL && razon != MobSpawnType.CHUNK_GENERATION)
			return;
		if (!Ruleta.cambioActivoPublico("mob-creeper-rojo") || !(e.getEntity().getRandom().nextFloat() < PROB_ROJO))
			return;
		ServerLevel nivel = e.getLevel().getLevel();
		CreeperRojoEntity rojo = CreeperRojo.TIPO.get().create(nivel);
		if (rojo == null)
			return;
		e.setSpawnCancelled(true);
		rojo.moveTo(e.getX(), e.getY(), e.getZ(), e.getEntity().getYRot(), 0.0F);
		rojo.finalizeSpawn(e.getLevel(), e.getDifficulty(), razon, null);
		e.getLevel().addFreshEntity(rojo);
	}
}
