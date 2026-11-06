package net.ruleta.client.eclipse;

import org.joml.Vector3f;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.ruleta.client.agujero.AgujeroCliente;

/**
 * Efectos del Overworld durante el Eclipse y el Agujero Negro (oscurece el mapa de luz y puede tapar cielo y nubes).
 * En Forge eran métodos de IDimensionSpecialEffectsExtension; en Fabric los llaman los mixins de LightTexture y
 * LevelRenderer (mixin.client.LightTextureMixin / LevelRendererMixin) en los mismos puntos que Forge.
 */
public final class EfectosEclipse extends DimensionSpecialEffects.OverworldEffects {
	public EfectosEclipse() {
		super();
	}

	public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightLevel, float skyLightLevel, int pixelX, int pixelY, Vector3f colors) {
		float k = Math.max(EclipseCliente.oscuridad(), AgujeroCliente.nivel() * 0.6F);
		float kb = EclipseCliente.oscuridadBloques();
		if (k <= 0.0F && kb <= 0.0F)
			return;
		float cielo = Math.max(0.0F, skyLightLevel);
		float bloque = Math.max(0.0F, blockLightLevel);
		float delCielo = cielo / (cielo + bloque + 0.02F);
		float factor = Math.max(0.0F, 1.0F - k * delCielo - kb * (1.0F - delCielo));
		colors.mul(factor);
	}

	/** true = no dibujar el cielo de vanilla (Forge: renderSky). */
	public boolean renderSky() {
		return AgujeroCliente.tapaCielo();
	}

	/** true = no dibujar las nubes de vanilla (Forge: renderClouds). */
	public boolean renderClouds() {
		return AgujeroCliente.tapaCielo();
	}
}
