package net.ruleta.client.mapa;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.ruleta.bunker.Bunkers;
import net.ruleta.client.bunker.BunkerMarcador;

/** Mapa de los chunks cargados (tecla M): arrastrar para mover, rueda para zoom, espacio para centrar. */
public class PantallaMapa extends Screen {
	private static final int CHUNKS_MAX = 32;
	private static final float ZOOM_TEXTURAS = 3.0F;
	private static float zoom = 2.0F;
	private DynamicTexture textura;
	private ResourceLocation idTextura;
	private int lado;
	private int minX;
	private int minZ;
	private double centroX;
	private double centroZ;

	public PantallaMapa() {
		super(Component.literal("Mapa"));
	}

	@Override
	protected void init() {
		if (this.textura == null && this.minecraft != null && this.minecraft.player != null) {
			this.centroX = this.minecraft.player.getX();
			this.centroZ = this.minecraft.player.getZ();
			this.preparar();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void preparar() {
		Player p = this.minecraft.player;
		int radio = Math.min(CHUNKS_MAX, this.minecraft.options.getEffectiveRenderDistance() + 1);
		int c0x = (p.getBlockX() >> 4) - radio;
		int c0z = (p.getBlockZ() >> 4) - radio;
		int n = radio * 2 + 1;
		this.minX = c0x << 4;
		this.minZ = c0z << 4;
		this.lado = n * 16;
		this.textura = new DynamicTexture(this.lado, this.lado, true);
		this.idTextura = this.minecraft.getTextureManager().register("ruleta_mapa", this.textura);
		NativeImage img = this.textura.getPixels();
		if (img == null)
			return;

		img.fillRect(0, 0, this.lado, this.lado, -16777216);
		for (int cz = 0; cz < n; cz++) {
			for (int cx = 0; cx < n; cx++) {
				long c = ChunkPos.asLong(c0x + cx, c0z + cz);
				MapaDatos.Trozo t = MapaDatos.CACHE.get(c);
				if (t != null)
					this.pintar(img, c0x + cx, c0z + cz, t);
				else if (this.minecraft.level.getChunkSource().getChunk(c0x + cx, c0z + cz, ChunkStatus.FULL, false) != null)
					MapaDatos.pedir(c);
			}
		}
		this.textura.upload();
	}

	@Override
	public void tick() {
		if (this.textura == null || MapaDatos.CAMBIADOS.isEmpty())
			return;
		NativeImage img = this.textura.getPixels();
		if (img == null)
			return;
		for (long c : MapaDatos.CAMBIADOS) {
			MapaDatos.Trozo t = MapaDatos.CACHE.get(c);
			if (t != null)
				this.pintar(img, ChunkPos.getX(c), ChunkPos.getZ(c), t);
		}
		MapaDatos.CAMBIADOS.clear();
		this.textura.upload();
	}

	private void pintar(NativeImage img, int cx, int cz, MapaDatos.Trozo t) {
		int ox = (cx << 4) - this.minX;
		int oz = (cz << 4) - this.minZ;
		if (ox < 0 || oz < 0 || ox + 16 > this.lado || oz + 16 > this.lado)
			return;
		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++)
				img.setPixelRGBA(ox + x, oz + z, t.abgr[z * 16 + x]);
		}
	}

	@Override
	public void removed() {
		if (this.idTextura != null && this.minecraft != null)
			this.minecraft.getTextureManager().release(this.idTextura);
		this.textura = null;
		this.idTextura = null;
		super.removed();
	}

	/** En 1.20.1 esta pantalla no tenía fondo (pinta negro ella misma); Screen.render de 1.21.1 lo pintaría: se anula. */
	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
	}

	@Override
	public void render(GuiGraphics g, int ratonX, int ratonY, float parcial) {
		g.fill(0, 0, this.width, this.height, -16777216);
		float cx = (float) this.width / 2.0F;
		float cy = (float) this.height / 2.0F;
		if (this.idTextura != null) {
			float x0 = cx + (float) (((double) this.minX - this.centroX) * (double) zoom);
			float y0 = cy + (float) (((double) this.minZ - this.centroZ) * (double) zoom);
			g.pose().pushPose();
			g.pose().translate(x0, y0, 0.0F);
			g.pose().scale(zoom, zoom, 1.0F);
			g.blit(this.idTextura, 0, 0, 0.0F, 0.0F, this.lado, this.lado, this.lado, this.lado);
			g.pose().popPose();
			if (zoom >= ZOOM_TEXTURAS)
				this.texturas(g, cx, cy);
		}

		this.otrosJugadores(g, parcial, cx, cy);
		this.bunkers(g, cx, cy);
		this.jugador(g, parcial, cx, cy);
		this.brujula(g);
		super.render(g, ratonX, ratonY, parcial);
	}

	private void texturas(GuiGraphics g, float cx, float cy) {
		int bx0 = Mth.floor(this.centroX - (double) (cx / zoom));
		int bx1 = Mth.ceil(this.centroX + (double) (cx / zoom));
		int bz0 = Mth.floor(this.centroZ - (double) (cy / zoom));
		int bz1 = Mth.ceil(this.centroZ + (double) (cy / zoom));
		Matrix4f m = g.pose().last().pose();
		// 1.21.1 ya no tiene POSITION_COLOR_TEX: se usa POSITION_TEX_COLOR (mismo resultado: textura × color)
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

		for (int ccz = bz0 >> 4; ccz <= bz1 >> 4; ccz++) {
			for (int ccx = bx0 >> 4; ccx <= bx1 >> 4; ccx++) {
				MapaDatos.Trozo t = MapaDatos.CACHE.get(ChunkPos.asLong(ccx, ccz));
				if (t == null)
					continue;
				for (int z = 0; z < 16; z++) {
					int wz = (ccz << 4) + z;
					if (wz < bz0 || wz > bz1)
						continue;
					float y0 = cy + (float) (((double) wz - this.centroZ) * (double) zoom);
					float y1 = y0 + zoom;
					for (int x = 0; x < 16; x++) {
						int wx = (ccx << 4) + x;
						if (wx < bx0 || wx > bx1)
							continue;
						TextureAtlasSprite sp = t.sprite[z * 16 + x];
						if (sp == null)
							continue;
						int c = t.mult[z * 16 + x];
						float r = (float) (c >> 16 & 255) / 255.0F;
						float v = (float) (c >> 8 & 255) / 255.0F;
						float a = (float) (c & 255) / 255.0F;
						float x0 = cx + (float) (((double) wx - this.centroX) * (double) zoom);
						float x1 = x0 + zoom;
						b.addVertex(m, x0, y0, 0.0F).setColor(r, v, a, 1.0F).setUv(sp.getU0(), sp.getV0());
						b.addVertex(m, x0, y1, 0.0F).setColor(r, v, a, 1.0F).setUv(sp.getU0(), sp.getV1());
						b.addVertex(m, x1, y1, 0.0F).setColor(r, v, a, 1.0F).setUv(sp.getU1(), sp.getV1());
						b.addVertex(m, x1, y0, 0.0F).setColor(r, v, a, 1.0F).setUv(sp.getU1(), sp.getV0());
					}
				}
			}
		}

		// 1.21.1: build() devuelve null si no se añadió ningún vértice
		MeshData malla = b.build();
		if (malla != null)
			BufferUploader.drawWithShader(malla);
	}

	private void bunkers(GuiGraphics g, float cx, float cy) {
		for (Bunkers.Bunker b : BunkerMarcador.deAqui()) {
			float sx = cx + (float) ((b.mx() - this.centroX) * (double) zoom);
			float sy = cy + (float) ((b.mz() - this.centroZ) * (double) zoom);
			BunkerMarcador.dibujarPin(g, sx, sy, b);
		}
	}

	private void otrosJugadores(GuiGraphics g, float parcial, float cx, float cy) {
		if (this.minecraft == null || this.minecraft.level == null)
			return;
		for (Player otro : this.minecraft.level.players()) {
			if (otro == this.minecraft.player || otro.isSpectator())
				continue;
			double px = Mth.lerp((double) parcial, otro.xo, otro.getX());
			double pz = Mth.lerp((double) parcial, otro.zo, otro.getZ());
			int sx = Math.round(cx + (float) ((px - this.centroX) * (double) zoom));
			int sy = Math.round(cy + (float) ((pz - this.centroZ) * (double) zoom));
			g.fill(sx - 3, sy - 3, sx + 3, sy + 3, -14671840);
			g.fill(sx - 2, sy - 2, sx + 2, sy + 2, -1);
		}
	}

	private void jugador(GuiGraphics g, float parcial, float cx, float cy) {
		Player p = this.minecraft == null ? null : this.minecraft.player;
		if (p == null)
			return;
		double px = Mth.lerp((double) parcial, p.xo, p.getX());
		double pz = Mth.lerp((double) parcial, p.zo, p.getZ());
		float sx = cx + (float) ((px - this.centroX) * (double) zoom);
		float sy = cy + (float) ((pz - this.centroZ) * (double) zoom);
		float yaw = p.getViewYRot(parcial);
		g.pose().pushPose();
		g.pose().translate(sx, sy, 0.0F);
		g.pose().mulPose(Axis.ZP.rotationDegrees(yaw + 180.0F));
		Matrix4f m = g.pose().last().pose();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.disableCull();
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		flecha(b, m, 0.62F, 0.35F, 0.0F, 0.0F);
		flecha(b, m, 0.45F, 0.95F, 0.12F, 0.12F);
		BufferUploader.drawWithShader(b.buildOrThrow());
		RenderSystem.enableCull();
		g.pose().popPose();
	}

	private static void flecha(BufferBuilder b, Matrix4f m, float s, float r, float v, float a) {
		float[] punta = {0.0F, -9.0F * s};
		float[] izq = {-6.5F * s, 7.0F * s};
		float[] muesca = {0.0F, 3.0F * s};
		float[] der = {6.5F * s, 7.0F * s};
		vertice(b, m, punta, r, v, a);
		vertice(b, m, izq, r, v, a);
		vertice(b, m, muesca, r, v, a);
		vertice(b, m, punta, r, v, a);
		vertice(b, m, muesca, r, v, a);
		vertice(b, m, der, r, v, a);
	}

	private static void vertice(BufferBuilder b, Matrix4f m, float[] p, float r, float v, float a) {
		b.addVertex(m, p[0], p[1], 0.0F).setColor(r, v, a, 1.0F);
	}

	private void brujula(GuiGraphics g) {
		float cx = (float) this.width - 34.0F;
		float cy = 30.0F;
		float largo = 20.0F;
		g.pose().pushPose();
		g.pose().translate(cx, cy, 0.0F);
		Matrix4f m = g.pose().last().pose();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.disableCull();
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		linea(b, m, -largo, -largo, largo, largo, 3.0F, 0.0F);
		linea(b, m, -largo, largo, largo, -largo, 3.0F, 0.0F);
		linea(b, m, -largo, -largo, largo, largo, 1.2F, 1.0F);
		linea(b, m, -largo, largo, largo, -largo, 1.2F, 1.0F);
		BufferUploader.drawWithShader(b.buildOrThrow());
		RenderSystem.enableCull();
		g.pose().popPose();

		int ix = Math.round(cx);
		int iy = Math.round(cy);
		int d = 11;
		this.letra(g, "N", ix, iy - d);
		this.letra(g, "S", ix, iy + d);
		this.letra(g, "W", ix - d, iy);
		this.letra(g, "E", ix + d, iy);
	}

	private void letra(GuiGraphics g, String t, int x, int y) {
		int w = this.font.width(t);
		int lx = x - w / 2;
		int ly = y - 4;
		for (int ox = -1; ox <= 1; ox++) {
			for (int oy = -1; oy <= 1; oy++) {
				if (ox != 0 || oy != 0)
					g.drawString(this.font, t, lx + ox, ly + oy, 0, false);
			}
		}
		g.drawString(this.font, t, lx, ly, 16777215, false);
	}

	private static void linea(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2, float grosor, float tono) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float l = Mth.sqrt(dx * dx + dy * dy);
		float nx = -dy / l * grosor * 0.5F;
		float ny = dx / l * grosor * 0.5F;
		float[][] q = {{x1 + nx, y1 + ny}, {x1 - nx, y1 - ny}, {x2 - nx, y2 - ny}, {x2 + nx, y2 + ny}};
		int[] orden = {0, 1, 2, 0, 2, 3};
		for (int i : orden)
			b.addVertex(m, q[i][0], q[i][1], 0.0F).setColor(tono, tono, tono, 1.0F);
	}

	/** 1.21.1: la rueda trae desplazamiento horizontal y vertical; se usa el vertical como el delta de 1.20.1. */
	@Override
	public boolean mouseScrolled(double ratonX, double ratonY, double dx, double delta) {
		double bx = this.centroX + (ratonX - (double) this.width / 2.0D) / (double) zoom;
		double bz = this.centroZ + (ratonY - (double) this.height / 2.0D) / (double) zoom;
		zoom = Mth.clamp(zoom * (delta > 0.0D ? 1.25F : 0.8F), 0.25F, 24.0F);
		this.centroX = bx - (ratonX - (double) this.width / 2.0D) / (double) zoom;
		this.centroZ = bz - (ratonY - (double) this.height / 2.0D) / (double) zoom;
		return true;
	}

	@Override
	public boolean mouseDragged(double ratonX, double ratonY, int boton, double dx, double dy) {
		this.centroX -= dx / (double) zoom;
		this.centroZ -= dy / (double) zoom;
		return true;
	}

	@Override
	public boolean keyPressed(int tecla, int codigo, int mods) {
		if (MapaCliente.TECLA.matches(tecla, codigo)) {
			this.onClose();
			return true;
		}
		if (tecla == 32 && this.minecraft != null && this.minecraft.player != null) {
			this.centroX = this.minecraft.player.getX();
			this.centroZ = this.minecraft.player.getZ();
			return true;
		}
		return super.keyPressed(tecla, codigo, mods);
	}
}
