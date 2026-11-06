package net.ruleta.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.ruleta.bunker.Bunkers;
import net.ruleta.nuclear.entity.NuclearCreeperEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Explosión nuclear: 30 de daño a 40 bloques y un cráter de radio 7 que se excava poco a poco (2500 bloques/tick). */
public final class NuclearExplosion {
	public static final double DAMAGE_RADIUS = 40.0D;
	public static final float DAMAGE = 30.0F;
	public static final int CRATER_RADIUS = 7;
	private static final int BLOCKS_PER_TICK = 2500;
	private static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
			ResourceLocation.fromNamespaceAndPath("nuclearcreeper", "nuclear_explosion"));
	private static final List<Crater> CRATERS = new ArrayList<>();

	private NuclearExplosion() {
	}

	public static void detonate(ServerLevel level, NuclearCreeperEntity creeper) {
		double x = creeper.getX();
		double y = creeper.getY() + (double) creeper.getBbHeight() / 2.0D;
		double z = creeper.getZ();
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 12.0F, 0.5F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 8, 6.0D, 2.0D, 6.0D, 0.0D);
		DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE), creeper, creeper);
		AABB box = new AABB(x - DAMAGE_RADIUS, y - DAMAGE_RADIUS, z - DAMAGE_RADIUS, x + DAMAGE_RADIUS, y + DAMAGE_RADIUS, z + DAMAGE_RADIUS);
		double maxSq = DAMAGE_RADIUS * DAMAGE_RADIUS;
		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != creeper && e.isAlive() && !e.isSpectator() && e.distanceToSqr(x, y, z) <= maxSq);
		for (LivingEntity victim : victims) {
			if (victim instanceof Player player && player.isBlocking()) {
				breakShield(player);
				continue;
			}
			victim.hurt(source, DAMAGE);
		}
		if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))
			queueCrater(level, BlockPos.containing(x, y, z));
	}

	private static void breakShield(Player player) {
		InteractionHand hand = player.getUsedItemHand();
		ItemStack shield = player.getItemInHand(hand);
		player.stopUsingItem();
		shield.shrink(1);
		player.level().broadcastEntityEvent(player, (byte) 30);
	}

	private static void queueCrater(ServerLevel level, BlockPos center) {
		RandomSource random = level.random;
		int r = CRATER_RADIUS;
		int inner = (r - 2) * (r - 2);
		int outer = r * r;
		List<BlockPos> list = new ArrayList<>();
		for (int dx = -r; dx <= r; dx++)
			for (int dy = -r; dy <= r; dy++)
				for (int dz = -r; dz <= r; dz++) {
					int d2 = dx * dx + dy * dy + dz * dz;
					if (d2 > outer)
						continue;
					if (d2 > inner && random.nextFloat() < 0.4F)
						continue;
					list.add(center.offset(dx, dy, dz));
				}
		list.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
		CRATERS.add(new Crater(level, list));
	}

	public static void tick() {
		if (CRATERS.isEmpty())
			return;
		Iterator<Crater> it = CRATERS.iterator();
		while (it.hasNext()) {
			Crater c = it.next();
			int end = Math.min(c.index + BLOCKS_PER_TICK, c.positions.size());
			while (c.index < end) {
				BlockPos pos = c.positions.get(c.index);
				BlockState state = c.level.getBlockState(pos);
				if (!state.isAir() && !Bunkers.protegido(c.level, pos, state))
					c.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
				c.index++;
			}
			if (c.index >= c.positions.size())
				it.remove();
		}
	}

	public static void clear() {
		CRATERS.clear();
	}

	private static final class Crater {
		final ServerLevel level;
		final List<BlockPos> positions;
		int index = 0;

		Crater(ServerLevel level, List<BlockPos> positions) {
			this.level = level;
			this.positions = positions;
		}
	}
}
