package net.cloudruleta.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.cloudruleta.compat.Bus;
import net.cloudruleta.compat.client.CamaraAngulos;
import net.cloudruleta.compat.client.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ViewportEvent.ComputeCameraAngles: Forge 1.20.1 lo lanzaba en GameRenderer.renderLevel justo después de
 * camera.setup(...) y aplicaba guiñada/inclinación a la cámara y el alabeo a la vista. En 1.21.1 la vista sale de
 * camera.rotation(), así que los tres ángulos se aplican a la cámara (como hace NeoForge 1.21.1).
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "renderLevel", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
			shift = At.Shift.AFTER))
	private void cloudruleta$angulosCamara(DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!Bus.hasListeners(ViewportEvent.ComputeCameraAngles.class))
			return;
		Camera camara = ((GameRenderer) (Object) this).getMainCamera();
		float yaw = camara.getYRot();
		float pitch = camara.getXRot();
		ViewportEvent.ComputeCameraAngles e = new ViewportEvent.ComputeCameraAngles(camara, camara.getPartialTickTime(), yaw, pitch, 0.0F);
		Bus.post(e);
		if (e.getYaw() != yaw || e.getPitch() != pitch || e.getRoll() != 0.0F)
			((CamaraAngulos) camara).cloudruleta$angulos(e.getYaw(), e.getPitch(), e.getRoll());
	}
}
