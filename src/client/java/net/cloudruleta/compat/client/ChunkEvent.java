package net.cloudruleta.compat.client;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.cloudruleta.compat.Event;

/** ChunkEvent.Load de Forge, solo en el cliente (ClientChunkEvents.CHUNK_LOAD de Fabric API). */
public abstract class ChunkEvent extends Event {
	private final LevelAccessor level;
	private final ChunkAccess chunk;

	protected ChunkEvent(LevelAccessor level, ChunkAccess chunk) {
		this.level = level;
		this.chunk = chunk;
	}

	public LevelAccessor getLevel() {
		return this.level;
	}

	public ChunkAccess getChunk() {
		return this.chunk;
	}

	public static final class Load extends ChunkEvent {
		public Load(LevelAccessor level, ChunkAccess chunk) {
			super(level, chunk);
		}
	}
}
