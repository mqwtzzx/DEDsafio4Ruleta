package net.cloudruleta.client.cloudruleta;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

/** Dibujo de las texturas HD de la cloudruleta (con el shader cloudruleta_hd, que permite recolorear). */
public final class Pintor {
	private static ShaderInstance shader;
	private static final Map<String, ResourceLocation> TEXTURAS = new HashMap<>();

	private Pintor() {
	}

	static void shader(ShaderInstance s) {
		shader = s;
	}

	static ResourceLocation textura(String nombre) {
		return TEXTURAS.computeIfAbsent(nombre, n -> {
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath("cloudruleta", "hd/" + n);
			Minecraft.getInstance().getTextureManager().register(id, new TexturaHD(ResourceLocation.fromNamespaceAndPath("cloudruleta", "textures/cloudruleta/" + n + ".png")));
			return id;
		});
	}

	static void empezar() {
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShader(() -> shader != null ? shader : GameRenderer.getPositionTexColorShader());
	}

	static void terminar() {
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	static void rectTono(Matrix4f m, String textura, float x0, float y0, float x1, float y1, int argb, float giro, float luz, float saturacion) {
		Uniform u = shader == null ? null : shader.getUniform("Recolor");
		if (u == null) {
			rect(m, textura, x0, y0, x1, y1, argb);
			return;
		}
		u.set(giro / 360.0F, luz, saturacion, 1.0F);
		try {
			rect(m, textura, x0, y0, x1, y1, argb);
		} finally {
			u.set(0.0F, 0.0F, 1.0F, 0.0F);
		}
	}

	static void rectSuma(Matrix4f m, String textura, float x0, float y0, float x1, float y1, int argb) {
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		try {
			rect(m, textura, x0, y0, x1, y1, argb);
		} finally {
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		}
	}

	static void rect(Matrix4f m, String textura, float x0, float y0, float x1, float y1, int argb) {
		if (argb >>> 24 == 0)
			return;
		RenderSystem.setShaderTexture(0, textura(textura));
		float a = (float) (argb >>> 24) / 255.0F;
		float r = (float) (argb >> 16 & 255) / 255.0F;
		float g = (float) (argb >> 8 & 255) / 255.0F;
		float b = (float) (argb & 255) / 255.0F;
		BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		bb.addVertex(m, x0, y0, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, a);
		bb.addVertex(m, x0, y1, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, a);
		bb.addVertex(m, x1, y1, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, a);
		bb.addVertex(m, x1, y0, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, a);
		BufferUploader.drawWithShader(bb.buildOrThrow());
	}
}
