package net.ruleta.mixin.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Acceso al mapa privado DimensionSpecialEffects.EFFECTS (lo usa compat.client.DimensionEfectos). */
@Mixin(DimensionSpecialEffects.class)
public interface DimensionSpecialEffectsAccessor {
	@Accessor("EFFECTS")
	static Object2ObjectMap<ResourceLocation, DimensionSpecialEffects> ruleta$efectos() {
		throw new AssertionError();
	}
}
