package net.ruleta.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.ruleta.mixin.client.GameRendererAccessor;

/** Desenfoque de fondo de las pantallas del mod (el post-efecto blur de vanilla). */
final class Fondo {
	private static boolean puesto;

	private Fondo() {
	}

	static void poner() {
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc.gameRenderer.currentEffect() == null) {
				// en 1.21.1 GameRenderer.loadEffect es privado: se llama con un accesor
				((GameRendererAccessor) mc.gameRenderer).ruleta$loadEffect(ResourceLocation.withDefaultNamespace("shaders/post/blur.json"));
				puesto = true;
			}
		} catch (Exception ignorado) {
			puesto = false;
		}
	}

	/**
	 * El fondo oscuro de las pantallas como en 1.20.1 (renderBackground dentro del mundo). En 1.21.1 Screen.render ya
	 * pinta su fondo (con su propio desenfoque); las pantallas del mod lo anulan y llaman a esto donde lo hacía el original.
	 */
	static void oscurecer(Screen pantalla, GuiGraphics g) {
		pantalla.renderTransparentBackground(g);
	}

	static void quitar() {
		if (!puesto)
			return;
		try {
			puesto = false;
			Minecraft.getInstance().gameRenderer.shutdownEffect();
		} catch (Exception ignorado) {
		}
	}
}
