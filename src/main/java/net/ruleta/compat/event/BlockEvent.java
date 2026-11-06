package net.ruleta.compat.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.ruleta.compat.Event;

import java.util.List;

/** BlockEvent de Forge: romper (BreakEvent), colocar (EntityPlaceEvent/EntityMultiPlaceEvent) y fluidos. */
public abstract class BlockEvent extends Event {
	private final LevelAccessor level;
	private final BlockPos pos;
	private final BlockState state;

	protected BlockEvent(LevelAccessor level, BlockPos pos, BlockState state) {
		this.level = level;
		this.pos = pos;
		this.state = state;
	}

	public LevelAccessor getLevel() {
		return level;
	}

	public BlockPos getPos() {
		return pos;
	}

	public BlockState getState() {
		return state;
	}

	@Override
	public boolean isCancelable() {
		return true;
	}

	/** Un jugador rompe un bloque (PlayerBlockBreakEvents.BEFORE de Fabric API). */
	public static final class BreakEvent extends BlockEvent {
		private final Player player;

		public BreakEvent(LevelAccessor level, BlockPos pos, BlockState state, Player player) {
			super(level, pos, state);
			this.player = player;
		}

		public Player getPlayer() {
			return player;
		}
	}

	/** Copia de un bloque antes de cambiarlo (BlockSnapshot de Forge, solo lo que usa el mod). */
	public record BlockSnapshot(BlockPos pos, BlockState replaced) {
		public BlockState getReplacedBlock() {
			return replaced;
		}

		public BlockPos getPos() {
			return pos;
		}
	}

	/** Una entidad coloca un bloque. */
	public static class EntityPlaceEvent extends BlockEvent {
		private final Entity entity;

		public EntityPlaceEvent(LevelAccessor level, BlockPos pos, BlockState placed, Entity entity) {
			super(level, pos, placed);
			this.entity = entity;
		}

		public Entity getEntity() {
			return entity;
		}

		public BlockState getPlacedBlock() {
			return getState();
		}
	}

	/** Colocar un bloque cambia varios a la vez (en este mod: una calabaza que forma un gólem). */
	public static final class EntityMultiPlaceEvent extends EntityPlaceEvent {
		private final List<BlockSnapshot> snapshots;

		public EntityMultiPlaceEvent(LevelAccessor level, BlockPos pos, BlockState placed, Entity entity, List<BlockSnapshot> snapshots) {
			super(level, pos, placed, entity);
			this.snapshots = snapshots;
		}

		public List<BlockSnapshot> getReplacedBlockSnapshots() {
			return snapshots;
		}
	}

	/** Un fluido coloca un bloque (lava + agua → piedra, obsidiana…). */
	public static final class FluidPlaceBlockEvent extends BlockEvent {
		private final BlockPos liquidPos;
		private BlockState newState;

		public FluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
			super(level, pos, state);
			this.liquidPos = liquidPos;
			this.newState = state;
		}

		public BlockPos getLiquidPos() {
			return liquidPos;
		}

		public BlockState getNewState() {
			return newState;
		}

		public void setNewState(BlockState state) {
			this.newState = state;
		}
	}
}
