package net.ruleta.client.agujero;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

/** Hornea (una cara por fotograma) los mapas cúbicos de nebulosa y constelaciones del cielo del agujero negro. */
public final class CieloBake {
	private static final Logger LOG = LoggerFactory.getLogger("ruleta");
	private static final int RES_NEBULOSA = 768;
	private static final int RES_CONSTELACIONES = 1024;
	private static final int CARAS = 12;
	private static ShaderInstance shader;
	private static RenderTarget nebulosa;
	private static RenderTarget constelaciones;
	private static int hechas;
	private static boolean fallo;

	private CieloBake() {
	}

	public static void shader(ShaderInstance s) {
		shader = s;
		fallo = false;
		liberar();
	}

	public static boolean listo() {
		return hechas >= CARAS && nebulosa != null && constelaciones != null;
	}

	public static boolean fallo() {
		return fallo;
	}

	public static RenderTarget nebulosa() {
		return nebulosa;
	}

	public static RenderTarget constelaciones() {
		return constelaciones;
	}

	public static boolean avanzar() {
		if (listo())
			return true;
		if (shader == null || fallo)
			return false;
		try {
			if (nebulosa == null) {
				nebulosa = crear(RES_NEBULOSA * 3, RES_NEBULOSA * 2);
				constelaciones = crear(RES_CONSTELACIONES * 3, RES_CONSTELACIONES * 2);
				hechas = 0;
			}
			boolean esNebulosa = hechas < 6;
			int cara = hechas % 6;
			int res = esNebulosa ? RES_NEBULOSA : RES_CONSTELACIONES;
			dibujarCara(esNebulosa ? nebulosa : constelaciones, res, cara, esNebulosa ? 0.0F : 1.0F);
			hechas++;
		} catch (RuntimeException ex) {
			LOG.warn("No se pudo hornear el cielo del agujero negro", ex);
			fallo = true;
			liberar();
		}
		return listo();
	}

	public static void liberar() {
		if (nebulosa != null) {
			nebulosa.destroyBuffers();
			nebulosa = null;
		}
		if (constelaciones != null) {
			constelaciones.destroyBuffers();
			constelaciones = null;
		}
		hechas = 0;
	}

	private static RenderTarget crear(int ancho, int alto) {
		RenderTarget t = new TextureTarget(ancho, alto, false, Minecraft.ON_OSX);
		t.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		t.clear(Minecraft.ON_OSX);
		t.setFilterMode(9729);
		return t;
	}

	private static void dibujarCara(RenderTarget destino, int res, int cara, float modo) {
		Minecraft mc = Minecraft.getInstance();
		ShaderInstance sh = shader;
		sh.safeGetUniform("Cara").set((float) cara);
		sh.safeGetUniform("Modo").set(modo);
		destino.bindWrite(false);
		RenderSystem.viewport(cara % 3 * res, cara / 3 * res, res, res);
		try {
			RenderSystem.disableBlend();
			RenderSystem.disableDepthTest();
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			RenderSystem.setShader(() -> sh);
			BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
			b.addVertex(-1.0F, -1.0F, 0.0F);
			b.addVertex(1.0F, -1.0F, 0.0F);
			b.addVertex(1.0F, 1.0F, 0.0F);
			b.addVertex(-1.0F, 1.0F, 0.0F);
			BufferUploader.drawWithShader(b.buildOrThrow());
		} finally {
			mc.getMainRenderTarget().bindWrite(true);
			RenderSystem.enableCull();
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
		}
	}
}
