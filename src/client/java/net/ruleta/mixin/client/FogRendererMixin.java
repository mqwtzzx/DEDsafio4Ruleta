package net.ruleta.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.ruleta.compat.Bus;
import net.ruleta.compat.client.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ViewportEvent de la niebla, en los mismos puntos que Forge 1.20.1:
 * - ComputeFogColor al final de FogRenderer.setupColor, justo antes de RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0).
 * - RenderFog al final de FogRenderer.setupFog; si se cancela se aplican las distancias y la forma del evento.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	@Shadow
	private static float fogRed;
	@Shadow
	private static float fogGreen;
	@Shadow
	private static float fogBlue;

	@WrapOperation(method = "setupColor", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clearColor(FFFF)V"))
	private static void ruleta$colorNiebla(float r, float g, float b, float a, Operation<Void> original, @Local(argsOnly = true) Camera camera,
										  @Local(argsOnly = true, ordinal = 0) float partialTick) {
		if (Bus.hasListeners(ViewportEvent.ComputeFogColor.class)) {
			ViewportEvent.ComputeFogColor e = new ViewportEvent.ComputeFogColor(camera, partialTick, r, g, b);
			Bus.post(e);
			fogRed = e.getRed();
			fogGreen = e.getGreen();
			fogBlue = e.getBlue();
			r = fogRed;
			g = fogGreen;
			b = fogBlue;
		}
		original.call(r, g, b, a);
	}

	@Inject(method = "setupFog", at = @At("RETURN"))
	private static void ruleta$distanciasNiebla(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean thickFog, float partialTick,
											   CallbackInfo ci) {
		if (!Bus.hasListeners(ViewportEvent.RenderFog.class))
			return;
		ViewportEvent.RenderFog e = new ViewportEvent.RenderFog(mode, camera.getFluidInCamera(), camera, partialTick, RenderSystem.getShaderFogStart(),
				RenderSystem.getShaderFogEnd(), RenderSystem.getShaderFogShape());
		if (Bus.post(e)) {
			RenderSystem.setShaderFogStart(e.getNearPlaneDistance());
			RenderSystem.setShaderFogEnd(e.getFarPlaneDistance());
			RenderSystem.setShaderFogShape(e.getFogShape());
		}
	}
}
