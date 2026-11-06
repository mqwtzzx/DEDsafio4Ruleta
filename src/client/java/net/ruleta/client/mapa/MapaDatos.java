package net.ruleta.client.mapa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.ChunkEvent;
import net.ruleta.compat.event.TickEvent;

/** Caché de colores de los chunks cargados para el mapa (un color y una textura por columna). */
public final class MapaDatos {
	static final Map<Long, Trozo> CACHE = new HashMap<>();
	static final Set<Long> CAMBIADOS = new HashSet<>();
	private static final LinkedHashSet<Long> COLA = new LinkedHashSet<>();
	private static final int POR_TICK = 6;
	private static final int POR_TICK_ABIERTO = 40;
	private static final Map<TextureAtlasSprite, Integer> MEDIOS = new HashMap<>();
	private static final Map<BlockState, Object[]> CARAS = new HashMap<>();
	private static final RandomSource AZAR = RandomSource.create(42L);
	private static NativeImage atlas;
	private static int atlasId = -1;
	private static Object mundo;
	private static int ticks;
	private static int sinUsarAtlas;

	public static final class Trozo {
		final int[] abgr = new int[256];
		final TextureAtlasSprite[] sprite = new TextureAtlasSprite[256];
		final int[] mult = new int[256];
		final int[] alto = new int[256];
	}

	private MapaDatos() {
	}

	static void pedir(long chunk) {
		COLA.add(chunk);
	}

	@SubscribeEvent
	public static void alCargarChunk(ChunkEvent.Load e) {
		if (e.getLevel() instanceof ClientLevel)
			COLA.add(e.getChunk().getPos().toLong());
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ClientTickEvent e) {
		if (e.phase != TickEvent.Phase.END)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			if (mundo != null)
				vaciar();
			return;
		}

		if (mundo != mc.level) {
			vaciar();
			mundo = mc.level;
		}

		int id = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId();
		if (id != atlasId) {
			atlasId = id;
			MEDIOS.clear();
			CARAS.clear();
			if (atlas != null) {
				atlas.close();
				atlas = null;
			}
			COLA.addAll(CACHE.keySet());
		}

		if (++ticks % 40 == 0 && mc.player != null) {
			int pcx = mc.player.getBlockX() >> 4;
			int pcz = mc.player.getBlockZ() >> 4;
			for (int dz = -2; dz <= 2; dz++) {
				for (int dx = -2; dx <= 2; dx++)
					COLA.add(ChunkPos.asLong(pcx + dx, pcz + dz));
			}
		}

		boolean abierto = mc.screen instanceof PantallaMapa;
		if (!abierto)
			CAMBIADOS.clear();

		int presupuesto = abierto ? POR_TICK_ABIERTO : POR_TICK;
		Iterator<Long> it = COLA.iterator();
		while (presupuesto > 0 && it.hasNext()) {
			long c = it.next();
			it.remove();
			LevelChunk ch = mc.level.getChunkSource().getChunk(ChunkPos.getX(c), ChunkPos.getZ(c), ChunkStatus.FULL, false);
			if (ch == null)
				continue;
			CACHE.put(c, convertir(mc, ch, ChunkPos.getX(c), ChunkPos.getZ(c)));
			if (abierto)
				CAMBIADOS.add(c);
			presupuesto--;
		}

		if (!COLA.isEmpty() || abierto) {
			sinUsarAtlas = 0;
		} else if (atlas != null && ++sinUsarAtlas > 1200) {
			atlas.close();
			atlas = null;
			sinUsarAtlas = 0;
		}
	}

	private static void vaciar() {
		CACHE.clear();
		COLA.clear();
		CAMBIADOS.clear();
		mundo = null;
	}

	private static Object[] caraArriba(Minecraft mc, BlockState st) {
		return CARAS.computeIfAbsent(st, s -> {
			BakedModel modelo = mc.getBlockRenderer().getBlockModel(s);
			List<BakedQuad> qs = modelo.getQuads(s, Direction.UP, AZAR);
			if (qs.isEmpty())
				qs = modelo.getQuads(s, null, AZAR);
			if (!qs.isEmpty()) {
				BakedQuad q = qs.get(0);
				return new Object[]{q.getSprite(), q.isTinted() ? q.getTintIndex() : -1};
			}
			return new Object[]{modelo.getParticleIcon(), -1};
		});
	}

	private static int colorMedio(Minecraft mc, TextureAtlasSprite sp) {
		Integer c = MEDIOS.get(sp);
		if (c != null)
			return c;

		if (atlas == null) {
			AbstractTexture t = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
			RenderSystem.bindTexture(t.getId());
			int w = GlStateManager._getTexLevelParameter(3553, 0, 4096);
			int h = GlStateManager._getTexLevelParameter(3553, 0, 4097);
			atlas = new NativeImage(w, h, false);
			atlas.downloadTexture(0, false);
		}

		long r = 0L;
		long g = 0L;
		long b = 0L;
		long n = 0L;
		int x0 = sp.getX();
		int y0 = sp.getY();
		int w = sp.contents().width();
		int h = sp.contents().height();
		for (int y = y0; y < y0 + h && y < atlas.getHeight(); y++) {
			for (int x = x0; x < x0 + w && x < atlas.getWidth(); x++) {
				int abgr = atlas.getPixelRGBA(x, y);
				if ((abgr >>> 24 & 255) < 16)
					continue;
				r += abgr & 255;
				g += abgr >> 8 & 255;
				b += abgr >> 16 & 255;
				n++;
			}
		}

		int rgb = n == 0L ? 8421504 : (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
		MEDIOS.put(sp, rgb);
		return rgb;
	}

	private static int multiplicar(int rgb, int tinte, float luz) {
		int r = Mth.clamp((int) ((float) ((rgb >> 16 & 255) * (tinte >> 16 & 255) / 255) * luz), 0, 255);
		int g = Mth.clamp((int) ((float) ((rgb >> 8 & 255) * (tinte >> 8 & 255) / 255) * luz), 0, 255);
		int b = Mth.clamp((int) ((float) ((rgb & 255) * (tinte & 255) / 255) * luz), 0, 255);
		return r << 16 | g << 8 | b;
	}

	private static Trozo convertir(Minecraft mc, LevelChunk ch, int cx, int cz) {
		ClientLevel nivel = mc.level;
		Trozo t = new Trozo();
		Trozo norte = CACHE.get(ChunkPos.asLong(cx, cz - 1));
		int suelo = nivel.getMinBuildHeight();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int y = ch.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
				pos.set((cx << 4) + x, y, (cz << 4) + z);
				BlockState st = ch.getBlockState(pos);
				int bajado = 0;
				while (y > suelo && bajado < 64 && (st.isAir() || (st.getRenderShape() == RenderShape.INVISIBLE && st.getFluidState().isEmpty()))) {
					y--;
					bajado++;
					pos.setY(y);
					st = ch.getBlockState(pos);
				}

				int i0 = z * 16 + x;
				if (st.isAir() || (st.getRenderShape() == RenderShape.INVISIBLE && st.getFluidState().isEmpty())) {
					t.alto[i0] = suelo;
					t.sprite[i0] = null;
					t.mult[i0] = 0;
					t.abgr[i0] = -16777216;
					continue;
				}

				TextureAtlasSprite sp;
				int tinte;
				float luz;
				FluidState fl = st.getFluidState();
				// Forge usaba IClientFluidTypeExtensions (textura quieta y tinte); en Fabric lo da FluidRenderHandler.
				FluidRenderHandler manejador = !fl.isEmpty() && st.getBlock() instanceof LiquidBlock ? FluidRenderHandlerRegistry.INSTANCE.get(fl.getType()) : null;
				if (manejador != null) {
					sp = manejador.getFluidSprites(nivel, pos, fl)[0];
					tinte = manejador.getFluidColor(nivel, pos, fl) & 16777215;
					int hondo = 0;
					BlockPos.MutableBlockPos q = pos.mutable();
					while (hondo < 12 && q.getY() > suelo && !ch.getBlockState(q).getFluidState().isEmpty()) {
						q.move(0, -1, 0);
						hondo++;
					}
					luz = hondo > 7 ? 0.72F : hondo > 2 ? 0.86F : 1.0F;
				} else {
					Object[] cara = caraArriba(mc, st);
					sp = (TextureAtlasSprite) cara[0];
					int indice = (Integer) cara[1];
					tinte = indice < 0 ? 16777215 : mc.getBlockColors().getColor(st, nivel, pos, indice);
					if (tinte == -1)
						tinte = 16777215;
					int alNorte = z > 0 ? t.alto[(z - 1) * 16 + x] : norte != null ? norte.alto[240 + x] : y;
					luz = y == alNorte ? 0.88F : y > alNorte ? 1.0F : 0.72F;
				}

				int i = z * 16 + x;
				t.alto[i] = y;
				t.sprite[i] = sp;
				t.mult[i] = multiplicar(16777215, tinte, luz);
				int rgb = multiplicar(colorMedio(mc, sp), tinte, luz);
				t.abgr[i] = -16777216 | (rgb & 255) << 16 | (rgb >> 8 & 255) << 8 | rgb >> 16 & 255;
			}
		}
		return t;
	}
}
