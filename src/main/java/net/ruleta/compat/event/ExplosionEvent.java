package net.ruleta.compat.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.ruleta.compat.Event;

import java.util.List;

/** ExplosionEvent de Forge: Start (antes de calcular, cancelable) y Detonate (bloques y entidades afectados). */
public abstract class ExplosionEvent extends Event {
	private final Level level;
	private final Explosion explosion;

	protected ExplosionEvent(Level level, Explosion explosion) {
		this.level = level;
		this.explosion = explosion;
	}

	public Level getLevel() {
		return level;
	}

	public Explosion getExplosion() {
		return explosion;
	}

	public static final class Start extends ExplosionEvent {
		public Start(Level level, Explosion explosion) {
			super(level, explosion);
		}

		@Override
		public boolean isCancelable() {
			return true;
		}
	}

	public static final class Detonate extends ExplosionEvent {
		private final List<Entity> entities;

		public Detonate(Level level, Explosion explosion, List<Entity> entities) {
			super(level, explosion);
			this.entities = entities;
		}

		/** Lista de bloques que se van a romper (se puede modificar). */
		public List<BlockPos> getAffectedBlocks() {
			return getExplosion().getToBlow();
		}

		/** Entidades que la explosión va a dañar (se puede modificar). */
		public List<Entity> getAffectedEntities() {
			return entities;
		}
	}
}
