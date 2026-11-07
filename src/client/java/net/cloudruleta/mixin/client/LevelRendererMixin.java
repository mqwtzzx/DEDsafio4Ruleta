package net.cloudruleta.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.cloudruleta.client.eclipse.EfectosEclipse;
import net.cloudruleta.compat.client.Etapas;
import net.cloudruleta.compat.client.RenderLevelStageEvent;
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
@Mixin(value = LevelRenderer.class, priority = 900)
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
	private float cloudruleta$parcial;
	@Unique
	private Camera cloudruleta$camara;
	@Unique
	private Matrix4f cloudruleta$vista;
	@Unique
	private Matrix4f cloudruleta$proyeccion;

	@Unique
	private void cloudruleta$publicar(RenderLevelStageEvent.Stage etapa) {
		if (cloudruleta$camara == null)
			return;
		Etapas.publicar(etapa, (LevelRenderer) (Object) this, cloudruleta$vista, cloudruleta$proyeccion, ticks, cloudruleta$parcial, cloudruleta$camara,
				capturedFrustum != null ? capturedFrustum : cullingFrustum);
	}

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void cloudruleta$empezar(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
								LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		cloudruleta$parcial = deltaTracker.getGameTimeDeltaPartialTick(false);
		cloudruleta$camara = camera;
		cloudruleta$vista = frustumMatrix;
		cloudruleta$proyeccion = projectionMatrix;
	}

	// Sodium sobreescribe renderSectionLayer y puede suprimir la llamada interna a clearRenderState.
	// RETURN se conserva tanto en vanilla como en Sodium, tras dibujar la capa translúcida.
	// La prioridad 900 permite inyectar DESPUÉS del overwrite de Sodium (prioridad 1000).
	@Inject(method = "renderSectionLayer", at = @At("RETURN"))
	private void cloudruleta$trasCapa(RenderType tipo, double x, double y, double z, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		if (tipo == RenderType.translucent())
			cloudruleta$publicar(RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
			shift = At.Shift.AFTER))
	private void cloudruleta$trasParticulas(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
									   LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		cloudruleta$publicar(RenderLevelStageEvent.Stage.AFTER_PARTICLES);
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void cloudruleta$terminar(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
								 LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
		cloudruleta$publicar(RenderLevelStageEvent.Stage.AFTER_LEVEL);
		cloudruleta$camara = null;
	}

	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
	private void cloudruleta$cielo(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy,
							  Runnable skyFogSetup, CallbackInfo ci) {
		if (level != null && level.effects() instanceof EfectosEclipse ef && ef.renderSky())
			ci.cancel();
	}

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void cloudruleta$nubes(com.mojang.blaze3d.vertex.PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
							  double camX, double camY, double camZ, CallbackInfo ci) {
		if (level != null && level.effects() instanceof EfectosEclipse ef && ef.renderClouds())
			ci.cancel();
	}
}
