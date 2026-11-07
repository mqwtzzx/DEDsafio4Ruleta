package net.cloudruleta.puerta;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Estado de la puerta (alimentada por redstone) y animación de apertura en el cliente. */
public class PuertaBunkerBE extends BlockEntity {
	public static final int DURACION = 60;
	private boolean alimentada;
	public float progreso;
	public float anterior;
	private boolean iniciada;

	public PuertaBunkerBE(BlockPos pos, BlockState state) {
		super(PuertaBunker.BE.get(), pos, state);
	}

	public static void tickCliente(Level level, BlockPos pos, BlockState state, PuertaBunkerBE be) {
		float meta = state.getValue(PuertaBunkerBloque.OPEN) ? 1.0F : 0.0F;
		if (!be.iniciada) {
			be.iniciada = true;
			be.progreso = meta;
			be.anterior = meta;
			return;
		}
		be.anterior = be.progreso;
		float paso = 0.016666668F;
		if (be.progreso < meta)
			be.progreso = Math.min(meta, be.progreso + paso);
		else if (be.progreso > meta)
			be.progreso = Math.max(meta, be.progreso - paso);
	}

	public boolean estaAlimentada() {
		return this.alimentada;
	}

	public void setAlimentada(boolean valor) {
		this.alimentada = valor;
		this.setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registros) {
		super.saveAdditional(tag, registros);
		tag.putBoolean("Alimentada", this.alimentada);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registros) {
		super.loadAdditional(tag, registros);
		this.alimentada = tag.getBoolean("Alimentada");
	}

	/**
	 * Caja de dibujo (en Forge, getRenderBoundingBox). Vanilla no la consulta: el renderizador de la puerta
	 * usa shouldRenderOffScreen para que no desaparezca al mirar de lado.
	 */
	public AABB getRenderBoundingBox() {
		BlockState s = this.getBlockState();
		if (!s.is(PuertaBunker.BLOQUE.get()))
			return new AABB(this.worldPosition);
		Direction derecha = s.getValue(PuertaBunkerBloque.FACING).getCounterClockWise();
		BlockPos a = this.worldPosition;
		BlockPos b = a.relative(derecha, 7);
		return new AABB(Math.min(a.getX(), b.getX()) - 6, a.getY() - 1, Math.min(a.getZ(), b.getZ()) - 6,
				Math.max(a.getX(), b.getX()) + 7, a.getY() + 11 + 1, Math.max(a.getZ(), b.getZ()) + 7);
	}
}
