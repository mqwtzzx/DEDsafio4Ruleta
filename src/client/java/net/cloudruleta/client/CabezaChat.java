package net.cloudruleta.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/** Cabeza pixelada (8×8 caracteres de una fuente propia) del jugador que muere, delante del mensaje de muerte. */
public final class CabezaChat {
	private static final ResourceLocation FUENTE = ResourceLocation.fromNamespaceAndPath("cloudruleta", "cabeza");
	private static final Map<ResourceLocation, int[]> CACHE = new HashMap<>();

	private CabezaChat() {
	}

	public static Component para(Component mensaje) {
		if (mensaje == null || !(mensaje.getContents() instanceof TranslatableContents tr) || !tr.getKey().startsWith("death."))
			return null;
		Object[] args = tr.getArgs();
		if (args.length == 0)
			return null;
		String texto = args[0] instanceof Component c ? c.getString() : String.valueOf(args[0]);
		ClientPacketListener con = Minecraft.getInstance().getConnection();
		if (con == null)
			return null;
		PlayerInfo info = null;
		int mejor = 0;
		for (PlayerInfo pi : con.getOnlinePlayers()) {
			String n = pi.getProfile().getName();
			if (texto.contains(n) && n.length() > mejor) {
				info = pi;
				mejor = n.length();
			}
		}
		if (info == null)
			return null;
		// getSkinLocation() de 1.20.1 → getSkin().texture() en 1.21.1
		int[] cara = caraDe(info.getSkin().texture());
		if (cara == null)
			return null;
		MutableComponent r = Component.empty();
		for (int x = 0; x < 8; x++)
			for (int y = 0; y < 8; y++) {
				String s = "" + (char) (57600 + y) + (char) (y == 7 ? 57617 : 57616);
				r.append(Component.literal(s).setStyle(Style.EMPTY.withFont(FUENTE).withColor(TextColor.fromRgb(cara[y * 8 + x]))));
			}
		return r.append(Component.literal(" "));
	}

	/** Lee la cara (y el sombrero, si lo hay) de la textura de la skin ya cargada en la GPU. */
	private static int[] caraDe(ResourceLocation skin) {
		int[] guardada = CACHE.get(skin);
		if (guardada != null)
			return guardada;
		try {
			AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(skin);
			tex.bind();
			int w = GL11.glGetTexLevelParameteri(3553, 0, 4096);
			int h = GL11.glGetTexLevelParameteri(3553, 0, 4097);
			if (w < 64 || h < 64 || w > 4096 || h > 4096)
				return null;
			int[] r = new int[64];
			try (NativeImage img = new NativeImage(w, h, false)) {
				img.downloadTexture(0, false);
				double u = (double) w / 64.0D;
				double v = (double) h / 64.0D;
				for (int j = 0; j < 8; j++)
					for (int i = 0; i < 8; i++) {
						int base = img.getPixelRGBA((int) (((double) (8 + i) + 0.5D) * u), (int) (((double) (8 + j) + 0.5D) * v));
						int sombrero = img.getPixelRGBA((int) (((double) (40 + i) + 0.5D) * u), (int) (((double) (8 + j) + 0.5D) * v));
						int p = sombrero >>> 24 >= 128 ? sombrero : base;
						r[j * 8 + i] = (p & 255) << 16 | (p >> 8 & 255) << 8 | p >> 16 & 255;
					}
			}
			CACHE.put(skin, r);
			return r;
		} catch (Throwable t) {
			return null;
		}
	}
}
