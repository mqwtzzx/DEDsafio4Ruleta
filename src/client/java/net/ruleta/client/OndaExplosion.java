package net.ruleta.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.ruleta.OndaPayload;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.ClientPlayerNetworkEvent;
import net.ruleta.compat.client.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Onda pixelada en el suelo que se expande desde cada explosión. */
public final class OndaExplosion {
	private static final float DURACION = 1.1F;
	private static final float PIXEL = 0.5F;
	private static final int MAXIMO = 24;
	private static final List<Onda> ONDAS = new ArrayList<>();

	private record Onda(double x, double y, double z, float radio, long inicio) {
	}

	private OndaExplosion() {
	}

	static void agregar(OndaPayload p) {
		if (ONDAS.size() >= MAXIMO)
			ONDAS.remove(0);
		ONDAS.add(new Onda(p.x(), p.y(), p.z(), p.radio(), System.nanoTime()));
	}

	@SubscribeEvent
	public static void alSalir(ClientPlayerNetworkEvent.LoggingOut evento) {
		ONDAS.clear();
	}

	@SubscribeEvent
	public static void alDibujar(RenderLevelStageEvent evento) {
		if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ONDAS.isEmpty())
			return;
		long ahora = System.nanoTime();
		Vec3 cam = evento.getCamera().getPosition();
		Matrix4f m = evento.getPoseStack().last().pose();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		// 1.21.1: Tesselator.begin devuelve el BufferBuilder; build() da null si no se añadió ningún vértice
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Iterator<Onda> it = ONDAS.iterator();
		while (it.hasNext()) {
			Onda o = it.next();
			float t = (float) (ahora - o.inicio) / 1.0E9F / DURACION;
			if (t >= 1.0F) {
				it.remove();
				continue;
			}
			dibujar(b, m, o, t, cam);
		}
		MeshData malla = b.build();
		if (malla != null)
			BufferUploader.drawWithShader(malla);
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static boolean dibujar(BufferBuilder b, Matrix4f m, Onda o, float t, Vec3 cam) {
		float sale = 1.0F - t;
		float r = o.radio * (1.0F - sale * sale * sale);
		float ancho = Math.max(PIXEL, r * (0.95F * (float) Math.pow(sale, 1.600000023841858D) + 0.04F));
		float dentro = r - ancho;
		float alfa = t < 0.55F ? 1.0F : 1.0F - (t - 0.55F) / 0.45F;
		if (r < 0.25F || alfa <= 0.0F)
			return false;
		float y = (float) (o.y + 0.12D - cam.y);
		int n = Mth.ceil(r / PIXEL);
		boolean algo = false;
		for (int gx = -n; gx < n; gx++)
			for (int gz = -n; gz < n; gz++) {
				float cx = ((float) gx + 0.5F) * PIXEL;
				float cz = ((float) gz + 0.5F) * PIXEL;
				float d = Mth.sqrt(cx * cx + cz * cz);
				if (d > r || d < dentro)
					continue;
				float u = (d - dentro) / ancho;
				int color = color(u, t);
				int a = (int) (alfa * (float) (color >>> 24 & 255));
				if (a <= 2)
					continue;
				int rr = color >> 16 & 255;
				int gg = color >> 8 & 255;
				int bb = color & 255;
				float x0 = (float) (o.x - cam.x) + (float) gx * PIXEL;
				float z0 = (float) (o.z - cam.z) + (float) gz * PIXEL;
				float x1 = x0 + PIXEL;
				float z1 = z0 + PIXEL;
				b.addVertex(m, x0, y, z0).setColor(rr, gg, bb, a);
				b.addVertex(m, x0, y, z1).setColor(rr, gg, bb, a);
				b.addVertex(m, x1, y, z1).setColor(rr, gg, bb, a);
				b.addVertex(m, x1, y, z0).setColor(rr, gg, bb, a);
				algo = true;
			}
		return algo;
	}

	private static int color(float u, float t) {
		if (t > 0.6F)
			return u > 0.5F ? (t > 0.8F ? -523753444 : -252681196) : -385890284;
		if (u > 0.82F)
			return -117474816;
		if (u > 0.55F)
			return -184564736;
		if (u > 0.3F)
			return -419435892;
		return -1258293032;
	}
}
