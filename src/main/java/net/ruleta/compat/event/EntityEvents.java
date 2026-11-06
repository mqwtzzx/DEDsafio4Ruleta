package net.ruleta.compat.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.ruleta.compat.Event;

/** Eventos de entidades de Forge: entrar y salir del mundo, cambiar de dimensión y perla de Ender. */
public final class EntityEvents {
	private EntityEvents() {}

	public static final class EntityJoinLevelEvent extends Event {
		private final Entity entity;
		private final Level level;
		private final boolean loadedFromDisk;

		public EntityJoinLevelEvent(Entity entity, Level level, boolean loadedFromDisk) {
			this.entity = entity;
			this.level = level;
			this.loadedFromDisk = loadedFromDisk;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public Entity getEntity() {
			return entity;
		}

		public Level getLevel() {
			return level;
		}

		public boolean loadedFromDisk() {
			return loadedFromDisk;
		}
	}

	public static final class EntityLeaveLevelEvent extends Event {
		private final Entity entity;
		private final Level level;

		public EntityLeaveLevelEvent(Entity entity, Level level) {
			this.entity = entity;
			this.level = level;
		}

		public Entity getEntity() {
			return entity;
		}

		public Level getLevel() {
			return level;
		}
	}

	public static final class EntityTravelToDimensionEvent extends Event {
		private final Entity entity;
		private final ResourceKey<Level> dimension;

		public EntityTravelToDimensionEvent(Entity entity, ResourceKey<Level> dimension) {
			this.entity = entity;
			this.dimension = dimension;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public Entity getEntity() {
			return entity;
		}

		public ResourceKey<Level> getDimension() {
			return dimension;
		}
	}

	/** EntityTeleportEvent.EnderPearl: el daño de la perla se puede cambiar o cancelar el viaje. */
	public static final class EnderPearlEvent extends Event {
		private final ServerPlayer player;
		private final ThrownEnderpearl pearl;
		private final double targetX, targetY, targetZ;
		private float attackDamage;

		public EnderPearlEvent(ServerPlayer player, ThrownEnderpearl pearl, double x, double y, double z, float attackDamage) {
			this.player = player;
			this.pearl = pearl;
			this.targetX = x;
			this.targetY = y;
			this.targetZ = z;
			this.attackDamage = attackDamage;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public ServerPlayer getPlayer() {
			return player;
		}

		public ThrownEnderpearl getPearlEntity() {
			return pearl;
		}

		public double getTargetX() {
			return targetX;
		}

		public double getTargetY() {
			return targetY;
		}

		public double getTargetZ() {
			return targetZ;
		}

		public float getAttackDamage() {
			return attackDamage;
		}

		public void setAttackDamage(float attackDamage) {
			this.attackDamage = attackDamage;
		}
	}
}
