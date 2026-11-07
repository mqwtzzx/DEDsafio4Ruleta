package net.cloudruleta.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.cloudruleta.client.eclipse.EfectosEclipse;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * IDimensionSpecialEffectsExtension.adjustLightmapColors de Forge. En Forge 1.20.1 (y NeoForge 1.21.1) se llama en
 * LightTexture.updateLightTexture para cada píxel del mapa de luz, después de mezclar la luz del cielo y la de los
 * bloques y justo antes del bloque de visión nocturna:
 * {@code clientlevel.effects().adjustLightmapColors(clientlevel, partialTicks, f, f7, f8, j, i, vector3f1)}
 * con f = getSkyDarken(1), f7 = blockLightRedFlicker + 1.5, f8 = getBrightness(dimensión, i) * f1 (luz del cielo de
 * la fila i), j = columna (luz de bloque) e i = fila (luz del cielo).
 * <p>
 * Aquí: cada {@code vector3f1.set(...)} empieza un píxel nuevo (la fila y la columna salen del número de píxel,
 * el bucle es «for i … for j …»), y el ajuste se aplica una sola vez por píxel en el primero de los puntos que
 * vienen después en el código: la visión nocturna (primer Vector3f.x()), la resta de oscuridad (primer
 * Vector3f.add(float, float, float)) o el clampColor del bloque «if (!flag)» (el segundo clampColor del método).
 * Solo actúa si los efectos de la dimensión son {@link EfectosEclipse} (el único que lo usaba).
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
	@Shadow
	private float blockLightRedFlicker;
	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private int cloudruleta$pixel;
	@Unique
	private boolean cloudruleta$hecho = true;
	@Unique
	private float cloudruleta$parcial;

	@Inject(method = "updateLightTexture", at = @At("HEAD"))
	private void cloudruleta$empezar(float partialTicks, CallbackInfo ci) {
		cloudruleta$pixel = -1;
		cloudruleta$hecho = true;
		cloudruleta$parcial = partialTicks;
	}

	@WrapOperation(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;set(FFF)Lorg/joml/Vector3f;", remap = false))
	private Vector3f cloudruleta$nuevoPixel(Vector3f color, float r, float g, float b, Operation<Vector3f> original) {
		cloudruleta$pixel++;
		cloudruleta$hecho = false;
		return original.call(color, r, g, b);
	}

	@WrapOperation(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;x()F", ordinal = 0, remap = false))
	private float cloudruleta$antesVisionNocturna(Vector3f color, Operation<Float> original) {
		cloudruleta$ajustar(color);
		return original.call(color);
	}

	@WrapOperation(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3f;add(FFF)Lorg/joml/Vector3f;", ordinal = 0, remap = false))
	private Vector3f cloudruleta$antesOscuridad(Vector3f color, float x, float y, float z, Operation<Vector3f> original) {
		cloudruleta$ajustar(color);
		return original.call(color, x, y, z);
	}

	@WrapOperation(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;clampColor(Lorg/joml/Vector3f;)V", ordinal = 1))
	private void cloudruleta$antesLimitar(Vector3f color, Operation<Void> original) {
		cloudruleta$ajustar(color);
		original.call(color);
	}

	@Unique
	private void cloudruleta$ajustar(Vector3f color) {
		if (cloudruleta$hecho)
			return;
		cloudruleta$hecho = true;
		ClientLevel nivel = this.minecraft.level;
		if (nivel == null || cloudruleta$pixel < 0 || !(nivel.effects() instanceof EfectosEclipse efectos))
			return;
		int i = cloudruleta$pixel / 16;
		int j = cloudruleta$pixel % 16;
		float f = nivel.getSkyDarken(1.0F);
		float f1 = nivel.getSkyFlashTime() > 0 ? 1.0F : f * 0.95F + 0.05F;
		float f7 = this.blockLightRedFlicker + 1.5F;
		float f8 = LightTexture.getBrightness(nivel.dimensionType(), i) * f1;
		efectos.adjustLightmapColors(nivel, cloudruleta$parcial, f, f7, f8, j, i, color);
	}
}
