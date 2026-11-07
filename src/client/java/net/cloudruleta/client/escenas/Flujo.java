package net.cloudruleta.client.escenas;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/** Secuencia de cuadros PNG leídos en segundo plano y subidos a una textura dinámica (vídeo de las escenas). */
public final class Flujo {
	private static final Logger LOG = LoggerFactory.getLogger("cloudruleta");
	private static final ExecutorService LECTOR = Executors.newFixedThreadPool(2, r -> {
		Thread t = new Thread(r, "cloudruleta-escenas");
		t.setDaemon(true);
		return t;
	});
	private final String carpeta;
	private final ResourceLocation id;
	public final int ancho;
	public final int alto;
	private DynamicTexture textura;
	private int subido = -1;
	private int pedido = -1;
	private CompletableFuture<NativeImage> lectura;

	public Flujo(String nombre, String carpeta, int ancho, int alto) {
		this.carpeta = carpeta;
		this.id = ResourceLocation.fromNamespaceAndPath("cloudruleta", "dinamica/" + nombre);
		this.ancho = ancho;
		this.alto = alto;
	}

	public ResourceLocation id() {
		return this.id;
	}

	public boolean mostrar(int n, int proximo) {
		if (n < 0)
			return false;

		if (this.lectura != null && this.lectura.isDone()) {
			NativeImage img = this.lectura.getNow(null);
			int leido = this.pedido;
			this.lectura = null;
			if (img != null) {
				if (leido == n || this.subido != n) {
					this.subir(img);
					this.subido = leido;
				}
				img.close();
			}
		}

		if (this.lectura == null) {
			if (this.subido != n)
				this.pedir(n);
			else if (proximo >= 0 && proximo != n)
				this.pedir(proximo);
		}

		return this.textura != null && this.subido >= 0;
	}

	public int subido() {
		return this.subido;
	}

	private void subir(NativeImage img) {
		if (this.textura == null) {
			this.textura = new DynamicTexture(this.ancho, this.alto, false);
			this.textura.setFilter(true, false);
			Minecraft.getInstance().getTextureManager().register(this.id, this.textura);
		}
		NativeImage destino = this.textura.getPixels();
		if (destino != null && img.getWidth() == this.ancho && img.getHeight() == this.alto) {
			destino.copyFrom(img);
			this.textura.upload();
		}
	}

	private void pedir(int n) {
		this.pedido = n;
		ResourceLocation ruta = ResourceLocation.fromNamespaceAndPath("cloudruleta", String.format("%s/%03d.png", this.carpeta, n));
		this.lectura = CompletableFuture.supplyAsync(() -> leer(ruta), LECTOR);
	}

	private static NativeImage leer(ResourceLocation ruta) {
		try {
			Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(ruta);
			if (r.isEmpty())
				return null;
			try (InputStream in = r.get().open()) {
				return NativeImage.read(NativeImage.Format.RGBA, in);
			}
		} catch (Exception ex) {
			LOG.warn("No se pudo leer el cuadro {}", ruta, ex);
			return null;
		}
	}

	public void soltar() {
		if (this.lectura != null) {
			this.lectura.thenAccept(img -> {
				if (img != null)
					img.close();
			});
			this.lectura = null;
		}
		if (this.textura != null) {
			Minecraft.getInstance().getTextureManager().release(this.id);
			this.textura = null;
		}
		this.subido = -1;
		this.pedido = -1;
	}
}
