package net.ruleta.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.ruleta.Ruleta;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.ClientPlayerNetworkEvent;

import java.io.InputStream;

/** Mientras Reviil está activo, la textura de la lluvia de vanilla se tiñe de rojo. */
public final class LluviaRoja {
	private static final ResourceLocation LLUVIA = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/rain.png");
	private static boolean puesta;

	private LluviaRoja() {
	}

	public static void poner(boolean roja) {
		if (roja == puesta)
			return;
		Minecraft mc = Minecraft.getInstance();
		TextureManager tm = mc.getTextureManager();
		if (roja) {
			NativeImage img = leer(mc);
			if (img == null)
				return;
			for (int y = 0; y < img.getHeight(); y++)
				for (int x = 0; x < img.getWidth(); x++) {
					int abgr = img.getPixelRGBA(x, y);
					int a = abgr >>> 24;
					int lum = ((abgr & 255) + (abgr >> 8 & 255) + (abgr >> 16 & 255)) / 3;
					int r = Math.min(255, 110 + lum / 2);
					int g = lum / 12;
					int b = lum / 14;
					img.setPixelRGBA(x, y, a << 24 | b << 16 | g << 8 | r);
				}
			tm.register(LLUVIA, new DynamicTexture(img));
		} else {
			tm.register(LLUVIA, new SimpleTexture(LLUVIA));
		}
		puesta = roja;
	}

	private static NativeImage leer(Minecraft mc) {
		try {
			Resource r = mc.getResourceManager().getResourceOrThrow(LLUVIA);
			try (InputStream in = r.open()) {
				return NativeImage.read(in);
			}
		} catch (Exception ex) {
			Ruleta.LOG.warn("[lluvia roja] no se pudo leer la textura de la lluvia", ex);
			return null;
		}
	}

	@SubscribeEvent
	public static void alSalir(ClientPlayerNetworkEvent.LoggingOut e) {
		poner(false);
	}
}
