package net.ruleta.compat.event;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.ruleta.compat.Event;

/** MobSpawnEvent de Forge (solo la parte que usa el mod). */
public abstract class MobSpawnEvent extends Event {
	private final Mob entity;
	private final ServerLevelAccessor level;
	private final double x;
	private final double y;
	private final double z;

	protected MobSpawnEvent(Mob entity, ServerLevelAccessor level, double x, double y, double z) {
		this.entity = entity;
		this.level = level;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Mob getEntity() {
		return entity;
	}

	public ServerLevelAccessor getLevel() {
		return level;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	/**
	 * Al empezar Mob.finalizeSpawn. Cancelarlo salta finalizeSpawn; setSpawnCancelled(true) además impide que la
	 * criatura se añada al mundo (lo comprueba el mixin de ServerLevel/WorldGenRegion, como hacía Forge).
	 */
	public static final class FinalizeSpawn extends MobSpawnEvent {
		private DifficultyInstance difficulty;
		private final MobSpawnType spawnType;
		private boolean spawnCancelled;

		public FinalizeSpawn(Mob entity, ServerLevelAccessor level, double x, double y, double z, DifficultyInstance difficulty, MobSpawnType spawnType) {
			super(entity, level, x, y, z);
			this.difficulty = difficulty;
			this.spawnType = spawnType;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public DifficultyInstance getDifficulty() {
			return difficulty;
		}

		public void setDifficulty(DifficultyInstance difficulty) {
			this.difficulty = difficulty;
		}

		public MobSpawnType getSpawnType() {
			return spawnType;
		}

		public boolean isSpawnCancelled() {
			return spawnCancelled;
		}

		public void setSpawnCancelled(boolean cancel) {
			this.spawnCancelled = cancel;
		}
	}
}
