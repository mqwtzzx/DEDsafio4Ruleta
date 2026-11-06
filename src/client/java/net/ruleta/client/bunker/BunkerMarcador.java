package net.ruleta.client.bunker;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.ruleta.bunker.BunkerPayload;
import net.ruleta.bunker.Bunkers;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.ClientPlayerNetworkEvent;
import net.ruleta.compat.client.RenderLevelStageEvent;

/** Marcadores de los bunkers (en el mundo, a través de las paredes, y en el mapa). */
public final class BunkerMarcador {
	private static final float PX = 1.0F;
	private static final float TEXTO = 0.75F;
	private static final double DIST_DIBUJO = 48.0D;
	private static List<Bunkers.Bunker> bunkers = new ArrayList<>();
	private static List<Bunkers.Bunker> todos = new ArrayList<>();

	private BunkerMarcador() {
	}

	public static void recibir(BunkerPayload m) {
		if (m.todos())
			todos = new ArrayList<>(m.lista());
		else
			bunkers = new ArrayList<>(m.lista());
	}

	public static List<Bunkers.Bunker> todos() {
		return todos;
	}

	public static List<Bunkers.Bunker> deAqui() {
		Minecraft mc = Minecraft.getInstance();
		List<Bunkers.Bunker> r = new ArrayList<>();
		if (mc.level == null)
			return r;
		String dim = mc.level.dimension().location().toString();
		for (Bunkers.Bunker b : bunkers) {
			if (b.dimension().equals(dim))
				r.add(b);
		}
		return r;
	}

	@SubscribeEvent
	public static void alSalir(ClientPlayerNetworkEvent.LoggingOut e) {
		bunkers = new ArrayList<>();
		todos = new ArrayList<>();
	}

	public static String etiqueta(Bunkers.Bunker b) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return b.nombre();
		double d = Math.sqrt(mc.player.distanceToSqr(b.mx(), b.my(), b.mz()));
		return b.nombre() + " (" + Math.round(d) + "m)";
	}

	/** Sin @SubscribeEvent en el original: lo llama AgujeroCliente en el orden que necesita. */
	public static void alDibujarMundo(RenderLevelStageEvent e) {
		Minecraft mc = Minecraft.getInstance();
		if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || mc.player == null || mc.options.hideGui || bunkers.isEmpty())
			return;
		List<Bunkers.Bunker> aqui = deAqui();
		if (aqui.isEmpty())
			return;

		Camera cam = e.getCamera();
		Vec3 c = cam.getPosition();
		float fov = mc.options.fov().get().floatValue();
		float altoPx = (float) mc.getWindow().getHeight();
		float pxPorUnidad = PX * (float) mc.getWindow().getGuiScale();
		Matrix4f proyAntes = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting ordenAntes = RenderSystem.getVertexSorting();
		// 1.21.1: la pila de modelo-vista es un Matrix4fStack de JOML (antes un PoseStack)
		Matrix4fStack mv = RenderSystem.getModelViewStack();
		mv.pushMatrix();
		mv.identity();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setProjectionMatrix(new Matrix4f(e.getProjectionMatrix()), VertexSorting.DISTANCE_TO_ORIGIN);
		mc.getMainRenderTarget().bindWrite(false);
		try {
			for (Bunkers.Bunker b : aqui) {
				Vec3 rel = new Vec3(b.mx() - c.x, b.my() - c.y, b.mz() - c.z);
				double dist = rel.length();
				if (dist < 0.001D)
					continue;
				double dDib = Math.min(dist, DIST_DIBUJO);
				Vec3 pos = rel.scale(dDib / dist);
				float escala = (float) ((double) pxPorUnidad * 2.0D * Math.tan(Math.toRadians((double) fov) / 2.0D) / (double) altoPx * dDib);
				PoseStack ps = new PoseStack();
				ps.mulPose(new Matrix4f().rotateX(cam.getXRot() * 0.017453292F).rotateY((cam.getYRot() + 180.0F) * 0.017453292F));
				ps.translate(pos.x, pos.y, pos.z);
				ps.mulPose(cam.rotation());
				ps.scale(-escala, -escala, escala);
				dibujar(ps.last().pose(), etiqueta(b));
			}
		} finally {
			mv.popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(proyAntes, ordenAntes);
		}
	}

	private static void dibujar(Matrix4f m, String texto) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		geometria(bb, m);
		BufferUploader.drawWithShader(bb.buildOrThrow());

		Minecraft mc = Minecraft.getInstance();
		Font f = mc.font;
		MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
		float w = (float) f.width(texto);
		Matrix4f mt = new Matrix4f(m).translate(0.0F, 3.0F, 0.0F).scale(TEXTO);
		f.drawInBatch(texto, -w / 2.0F, 0.0F, -1, false, mt, buf, Font.DisplayMode.SEE_THROUGH, 1426063360, 15728880);
		buf.endBatch();

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	public static void dibujarPin(GuiGraphics g, float x, float y, Bunkers.Bunker b) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0.0F);
		Matrix4f m = g.pose().last().pose();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		geometria(bb, m);
		BufferUploader.drawWithShader(bb.buildOrThrow());
		RenderSystem.enableCull();

		Font f = Minecraft.getInstance().font;
		String t = etiqueta(b);
		int w = f.width(t);
		g.pose().translate(0.0F, 3.0F, 0.0F);
		g.pose().scale(TEXTO, TEXTO, 1.0F);
		g.fill(-w / 2 - 2, -1, w / 2 + 2, 9, 1996488704);
		g.drawString(f, t, -w / 2, 0, 16777215, false);
		g.pose().popPose();
	}

	private static void geometria(BufferBuilder bb, Matrix4f m) {
		pin(bb, m, 8.2F, -1728053248);
		pin(bb, m, 7.0F, -1);
		icono(bb, m);
	}

	private static void pin(BufferBuilder b, Matrix4f m, float r, int argb) {
		float cy = -r * 2.0F;
		int seg = 24;
		for (int i = 0; i < seg; i++) {
			float a0 = (float) ((double) i * Math.PI * 2.0D / (double) seg);
			float a1 = (float) ((double) (i + 1) * Math.PI * 2.0D / (double) seg);
			v(b, m, 0.0F, cy, argb);
			v(b, m, Mth.cos(a0) * r, cy + Mth.sin(a0) * r, argb);
			v(b, m, Mth.cos(a1) * r, cy + Mth.sin(a1) * r, argb);
		}
		float k = r * 0.72F;
		v(b, m, -k, cy + r * 0.69F, argb);
		v(b, m, k, cy + r * 0.69F, argb);
		v(b, m, 0.0F, 0.0F, argb);
	}

	private static void icono(BufferBuilder b, Matrix4f m) {
		int gris = -13749704;
		float cy = -14.0F;
		v(b, m, -4.5F, cy - 1.0F, gris);
		v(b, m, 4.5F, cy - 1.0F, gris);
		v(b, m, 0.0F, cy - 4.5F, gris);
		rect(b, m, -3.5F, cy - 1.0F, 3.5F, cy + 3.5F, gris);
		rect(b, m, -1.2F, cy + 0.6F, 1.2F, cy + 3.5F, -1);
	}

	private static void rect(BufferBuilder b, Matrix4f m, float x0, float y0, float x1, float y1, int c) {
		v(b, m, x0, y0, c);
		v(b, m, x0, y1, c);
		v(b, m, x1, y1, c);
		v(b, m, x0, y0, c);
		v(b, m, x1, y1, c);
		v(b, m, x1, y0, c);
	}

	private static void v(BufferBuilder b, Matrix4f m, float x, float y, int argb) {
		b.addVertex(m, x, y, 0.0F).setColor(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24);
	}
}
