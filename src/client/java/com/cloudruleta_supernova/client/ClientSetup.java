package com.cloudruleta_supernova.client;

import com.cloudruleta_supernova.SupernovaMod;

import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.cloudruleta.compat.client.Formatos;

/** Antes: suscriptor del bus MOD (RegisterShadersEvent / RegisterRenderers). Lo llama RuletaClient. */
public class ClientSetup {
	private static ShaderInstance sphere;
	private static ShaderInstance additive;
	private static ShaderInstance multiply;

	public static ShaderInstance getSphere() {
		return sphere;
	}

	public static ShaderInstance getAdditive() {
		return additive;
	}

	public static ShaderInstance getMultiply() {
		return multiply;
	}

	public static void registrar() {
		// 1.21.1: DefaultVertexFormat.POSITION_COLOR_TEX ya no existe; Formatos.POSITION_COLOR_TEX es el mismo formato
		CoreShaderRegistrationCallback.EVENT.register(ctx -> {
			ctx.register(ResourceLocation.fromNamespaceAndPath("cloudruleta_supernova", "cloudruleta_supernova"), Formatos.POSITION_COLOR_TEX, s -> sphere = s);
			ctx.register(ResourceLocation.fromNamespaceAndPath("cloudruleta_supernova", "cloudruleta_supernova_add"), Formatos.POSITION_COLOR_TEX, s -> additive = s);
			ctx.register(ResourceLocation.fromNamespaceAndPath("cloudruleta_supernova", "cloudruleta_supernova_mul"), Formatos.POSITION_COLOR_TEX, s -> multiply = s);
		});
		EntityRendererRegistry.register(SupernovaMod.SUPERNOVA.get(), NoopRenderer::new);
	}
}
