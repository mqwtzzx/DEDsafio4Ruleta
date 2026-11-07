package net.cloudruleta.compat.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.cloudruleta.mixin.client.DimensionSpecialEffectsAccessor;

/**
 * Sustituto de RegisterDimensionSpecialEffectsEvent.register de Forge: pone (o reemplaza) los efectos de una
 * dimensión en el mismo mapa que usa DimensionSpecialEffects.forType al crear el ClientLevel.
 */
public final class DimensionEfectos {
	private DimensionEfectos() {
	}

	public static void poner(ResourceLocation tipo, DimensionSpecialEffects efectos) {
		DimensionSpecialEffectsAccessor.cloudruleta$efectos().put(tipo, efectos);
	}
}
