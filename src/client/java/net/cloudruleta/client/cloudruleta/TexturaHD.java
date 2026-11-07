package net.cloudruleta.client.cloudruleta;

import java.io.IOException;
import java.io.InputStream;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Textura grande con mipmaps propios (promedio ponderado por alfa) para que la cloudruleta se vea nítida al reducirla. */
final class TexturaHD extends AbstractTexture {
	private static final int NIVELES = 5;
	private final ResourceLocation archivo;
	private int ancho;
	private int alto;

	TexturaHD(ResourceLocation archivo) {
		this.archivo = archivo;
	}

	int ancho() {
		return this.ancho;
	}

	int alto() {
		return this.alto;
	}

	@Override
	public void load(ResourceManager recursos) throws IOException {
		NativeImage base;
		try (InputStream in = recursos.getResourceOrThrow(this.archivo).open()) {
			base = NativeImage.read(in);
		}
		this.ancho = base.getWidth();
		this.alto = base.getHeight();
		int niveles = 1;
		while (niveles < NIVELES && this.ancho >> niveles >= 1 && this.alto >> niveles >= 1)
			niveles++;

		NativeImage[] mips = new NativeImage[niveles];
		mips[0] = base;
		for (int k = 1; k < niveles; k++)
			mips[k] = reducir(mips[k - 1]);

		if (RenderSystem.isOnRenderThreadOrInit())
			this.subir(mips);
		else
			RenderSystem.recordRenderCall(() -> this.subir(mips));
	}

	private void subir(NativeImage[] mips) {
		TextureUtil.prepareImage(this.getId(), mips.length - 1, mips[0].getWidth(), mips[0].getHeight());
		for (int k = 0; k < mips.length; k++)
			mips[k].upload(k, 0, 0, 0, 0, mips[k].getWidth(), mips[k].getHeight(), true, true, true, true);
		this.setFilter(true, true);
	}

	private static NativeImage reducir(NativeImage src) {
		int w = Math.max(1, src.getWidth() / 2);
		int h = Math.max(1, src.getHeight() / 2);
		NativeImage out = new NativeImage(w, h, false);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				long sa = 0L;
				long sr = 0L;
				long sg = 0L;
				long sb = 0L;
				for (int dy = 0; dy < 2; dy++) {
					for (int dx = 0; dx < 2; dx++) {
						int c = src.getPixelRGBA(Math.min(src.getWidth() - 1, 2 * x + dx), Math.min(src.getHeight() - 1, 2 * y + dy));
						int a = c >>> 24;
						sa += a;
						sr += (long) (c & 255) * (long) a;
						sg += (long) (c >> 8 & 255) * (long) a;
						sb += (long) (c >> 16 & 255) * (long) a;
					}
				}
				int a = (int) ((sa + 2L) / 4L);
				int r = sa == 0L ? 0 : (int) (sr / sa);
				int g = sa == 0L ? 0 : (int) (sg / sa);
				int b = sa == 0L ? 0 : (int) (sb / sa);
				out.setPixelRGBA(x, y, a << 24 | b << 16 | g << 8 | r);
			}
		}
		return out;
	}
}
