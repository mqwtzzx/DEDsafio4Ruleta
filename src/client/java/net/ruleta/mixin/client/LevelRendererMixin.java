package net.ruleta.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.ruleta.client.eclipse.EfectosEclipse;
import net.ruleta.compat.client.Etapas;
import net.ruleta.compat.client.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puntos de Forge 1.20.1 en LevelRenderer:
 * - RenderLevelStageEvent AFTER_TRANSLUCENT_BLOCKS al terminar la capa translúcida (renderChunkLayer → renderSectionLayer,
 *   antes de clearRenderState), AFTER_PARTICLES justo después de cada ParticleEngine.render (en «Fabulosos», antes de
 *   PARTICLES_TARGET.clearRenderState, como Forge) y AFTER_LEVEL al terminar renderLevel (Forge lo lanzaba en
 *   GameRenderer nada más volver de renderLevel: es el mismo momento).
 * - IDimensionSpecialEffectsExtension.renderSky / renderClouds al principio de renderSky / renderClouds: si devuelven
 *   true no se dibuja el cielo / las nubes de vanilla. Aquí se consulta EfectosEclipse (el único que lo usaba).
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Shadow
	@Nullable
	private ClientLevel level;
	@Shadow
	private int ticks;
	@Shadow
	private Frustum cullingFrustum;
	@Shadow
	@Nullable
	private Frustum capturedFrustum;

	@Unique
	private float ruleta$parcial;
	@Unique
	private Camera ruleta$camara;
	@Unique
	private Matrix4f ruleta$vista;
	@Unique
	private Matrix4f ruleta$proyeccion;

	@Unique
	private void ruleta$publicar(RenderLevelStageEvent.Stage etapa) {
		if (ruleta$camara == null)
			return;
		Etapas.publicar(etapa, (LevelRenderer) (Object) this, ruleta$vista, ruleta$proyeccion, ticks, ruleta$parcial, ruleta$camara,
				capturedFrustum != null ? capturedFrustum : cullingFrustum);
	}

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void ruleta$empezar(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
								LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		ruleta$parcial = deltaTracker.getGameTimeDeltaPartialTick(false);
		ruleta$camara = camera;
		ruleta$vista = frustumMatrix;
		ruleta$proyeccion = projectionMatrix;
	}

	// Forge: dispatchRenderStage(renderType, …) al final de renderChunkLayer, justo antes de renderType.clearRenderState()
	@Inject(method = "renderSectionLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;clearRenderState()V"))
	private void ruleta$trasCapa(RenderType tipo, double x, double y, double z, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		if (tipo == RenderType.translucent())
			ruleta$publicar(RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
			shift = At.Shift.AFTER))
	private void ruleta$trasParticulas(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
									   LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		ruleta$publicar(RenderLevelStageEvent.Stage.AFTER_PARTICLES);
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void ruleta$terminar(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
								 LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		ruleta$publicar(RenderLevelStageEvent.Stage.AFTER_LEVEL);
		ruleta$camara = null;
	}

	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
	private void ruleta$cielo(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy,
							  Runnable skyFogSetup, CallbackInfo ci) {
		if (level != null && level.effects() instanceof EfectosEclipse ef && ef.renderSky())
			ci.cancel();
	}

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void ruleta$nubes(com.mojang.blaze3d.vertex.PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
							  double camX, double camY, double camZ, CallbackInfo ci) {
		if (level != null && level.effects() instanceof EfectosEclipse ef && ef.renderClouds())
			ci.cancel();
	}
}
