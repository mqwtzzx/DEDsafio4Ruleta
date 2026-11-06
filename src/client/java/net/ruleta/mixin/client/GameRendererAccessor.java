package net.ruleta.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** GameRenderer.loadEffect(ResourceLocation) era público en 1.20.1 y es privado en 1.21.1. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
	@Invoker("loadEffect")
	void ruleta$loadEffect(ResourceLocation id);
}
