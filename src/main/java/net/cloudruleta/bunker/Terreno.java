package net.cloudruleta.bunker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.BitSet;

/** Prepara el terreno del búnker: vacía su caja y hace una explanada con ladera alrededor. */
final class Terreno {
	private static final int EXPLANADA = 6;
	private static final int LADERA_MAX = 80;
	private static final int ALTO_LIBRE = 16;
	private static final int FLAGS = 18;

	private Terreno() {
	}

	private static int sueloDe(ServerLevel nivel, int x, int z, int desde, BlockPos.MutableBlockPos p) {
		int min = nivel.getMinBuildHeight();
		for (int y = Math.min(desde, nivel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1); y > min; y--) {
			p.set(x, y, z);
			BlockState s = nivel.getBlockState(p);
			if (!s.isAir() && s.getFluidState().isEmpty() && !s.is(BlockTags.LEAVES) && !s.canBeReplaced() && s.isSolid())
				return y;
		}
		return min;
	}

	private static BlockState material(ServerLevel nivel, int hondo) {
		if (nivel.dimension() == Level.NETHER)
			return Blocks.NETHERRACK.defaultBlockState();
		if (nivel.dimension() == Level.END)
			return Blocks.END_STONE.defaultBlockState();
		if (hondo == 0)
			return Blocks.GRASS_BLOCK.defaultBlockState();
		return hondo <= 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
	}

	/** Trabajo repartido entre ticks: primero las capas de la caja (2 por tick), luego 1200 columnas por tick. */
	static final class Obra {
		private static final int CAPAS_POR_TICK = 2;
		private static final int COLUMNAS_POR_TICK = 1200;
		private final ServerLevel nivel;
		private final int ex;
		private final int ey;
		private final int ez;
		private final BitSet ocupado;
		private final int alcance = 86;
		private final int ancho;
		private final int total;
		private int capa;
		private int columna;
		private final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

		Obra(ServerLevel nivel, BlockPos esquina, BitSet ocupado) {
			this.nivel = nivel;
			this.ex = esquina.getX();
			this.ey = esquina.getY();
			this.ez = esquina.getZ();
			this.ocupado = ocupado;
			this.ancho = Bunkers.SX + 2 * this.alcance;
			this.total = this.ancho * (Bunkers.SZ + 2 * this.alcance);
		}

		ServerLevel nivel() {
			return this.nivel;
		}

		boolean paso() {
			BlockState aire = Blocks.AIR.defaultBlockState();
			int sx = Bunkers.SX;
			int sz = Bunkers.SZ;
			if (this.capa < Bunkers.SY) {
				for (int n = 0; n < CAPAS_POR_TICK && this.capa < Bunkers.SY; n++, this.capa++) {
					int y = this.capa;
					for (int z = 0; z < sz; z++)
						for (int x = 0; x < sx; x++) {
							if (this.ocupado.get((y * sz + z) * sx + x))
								continue;
							this.p.set(this.ex + x, this.ey + y, this.ez + z);
							if (!this.nivel.getBlockState(this.p).isAir())
								this.nivel.setBlock(this.p, aire, FLAGS);
						}
				}
				return false;
			}
			int hasta = Math.min(this.total, this.columna + COLUMNAS_POR_TICK);
			while (this.columna < hasta) {
				this.columna(this.columna % this.ancho - this.alcance, this.columna / this.ancho - this.alcance, aire);
				this.columna++;
			}
			return this.columna >= this.total;
		}

		private void columna(int x, int z, BlockState aire) {
			int sx = Bunkers.SX;
			int sz = Bunkers.SZ;
			int arriba = this.ey - 1;
			int fx = x < 0 ? -x : x >= sx ? x - sx + 1 : 0;
			int fz = z < 0 ? -z : z >= sz ? z - sz + 1 : 0;
			float d = Mth.sqrt((float) (fx * fx + fz * fz));
			if (d > (float) this.alcance)
				return;
			int wx = this.ex + x;
			int wz = this.ez + z;
			int tope;
			if (d <= (float) EXPLANADA) {
				tope = arriba;
				if (d > 0.0F) {
					for (int y = this.ey; y < this.ey + ALTO_LIBRE; y++) {
						this.p.set(wx, y, wz);
						if (!this.nivel.getBlockState(this.p).isAir())
							this.nivel.setBlock(this.p, aire, FLAGS);
					}
				}
			} else {
				float ruido = (float) (Math.sin((double) wx * 0.21D) * 1.3D + Math.cos((double) wz * 0.17D) * 1.3D);
				tope = arriba - Math.round((d - 6.0F) * 0.95F + ruido);
			}
			int suelo = sueloDe(this.nivel, wx, wz, tope, this.p);
			if (suelo >= tope && d > (float) EXPLANADA)
				return;
			for (int y = suelo + 1; y <= tope; y++) {
				this.p.set(wx, y, wz);
				this.nivel.setBlock(this.p, material(this.nivel, tope - y), FLAGS);
			}
		}
	}
}
