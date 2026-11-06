package net.ruleta.compat.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.ruleta.compat.Bus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/** Reparte RenderLevelStageEvent desde mixin.client.LevelRendererMixin con el mismo estado que Forge 1.20.1. */
public final class Etapas {
	private Etapas() {
	}

	/**
	 * @param vista matriz de rotación de la cámara (en 1.20.1 era la PoseStack que recibía renderLevel)
	 * @param proyeccion matriz de proyección del mundo
	 */
	public static void publicar(RenderLevelStageEvent.Stage etapa, LevelRenderer lr, Matrix4f vista, Matrix4f proyeccion, int ticks,
								float parcial, Camera camara, @Nullable Frustum frustum) {
		if (!Bus.hasListeners(RenderLevelStageEvent.class))
			return;
		PoseStack pose = new PoseStack();
		pose.mulPose(vista);
		// en Forge 1.20.1 la matriz de modelo-vista de RenderSystem era la identidad en estos puntos
		Matrix4fStack mv = RenderSystem.getModelViewStack();
		mv.pushMatrix();
		mv.identity();
		RenderSystem.applyModelViewMatrix();
		try {
			Bus.post(new RenderLevelStageEvent(etapa, lr, pose, proyeccion, ticks, parcial, camara, frustum));
		} finally {
			mv.popMatrix();
			RenderSystem.applyModelViewMatrix();
		}
	}
}
