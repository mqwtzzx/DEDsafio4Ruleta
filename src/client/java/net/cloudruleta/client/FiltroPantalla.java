package net.cloudruleta.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

/** Post-efecto de pantalla completa (PostChain) con uniformes que se pueden cambiar en cada fotograma. */
public final class FiltroPantalla {
	private static final Logger LOG = LoggerFactory.getLogger("cloudruleta");
	private static Field campoPasadas;
	private final ResourceLocation ruta;
	private PostChain cadena;
	private List<PostPass> pasadas;
	private int ancho;
	private int alto;
	private boolean fallo;

	public FiltroPantalla(ResourceLocation ruta) {
		this.ruta = ruta;
	}

	public void fijar(String nombre, float valor) {
		if (this.pasadas == null)
			return;
		for (PostPass p : this.pasadas)
			p.getEffect().safeGetUniform(nombre).set(valor);
	}

	public void fijar(String nombre, float a, float b, float c, float d) {
		if (this.pasadas == null)
			return;
		for (PostPass p : this.pasadas)
			p.getEffect().safeGetUniform(nombre).set(a, b, c, d);
	}

	public boolean preparar() {
		if (this.fallo)
			return false;
		try {
			Minecraft mc = Minecraft.getInstance();
			Window v = mc.getWindow();
			if (this.cadena == null) {
				this.cadena = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), this.ruta);
				this.cadena.resize(v.getWidth(), v.getHeight());
				this.ancho = v.getWidth();
				this.alto = v.getHeight();
				this.pasadas = pasadasDe(this.cadena);
			} else if (this.ancho != v.getWidth() || this.alto != v.getHeight()) {
				this.cadena.resize(v.getWidth(), v.getHeight());
				this.ancho = v.getWidth();
				this.alto = v.getHeight();
			}
			return true;
		} catch (Exception ex) {
			LOG.warn("No se pudo cargar el filtro {}", this.ruta, ex);
			this.fallo = true;
			this.cerrar();
			return false;
		}
	}

	public void pasar(float parcial) {
		if (this.cadena == null)
			return;
		Minecraft mc = Minecraft.getInstance();
		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.resetTextureMatrix();
		this.cadena.process(parcial);
		mc.getMainRenderTarget().bindWrite(true);
	}

	public void cerrar() {
		if (this.cadena != null) {
			this.cadena.close();
			this.cadena = null;
			this.pasadas = null;
		}
	}

	public void reiniciar() {
		this.fallo = false;
		this.cerrar();
	}

	/** La lista de pasadas de PostChain es privada: se busca por tipo (no por nombre, que cambia con los mapeos). */
	@SuppressWarnings("unchecked")
	private static List<PostPass> pasadasDe(PostChain c) {
		try {
			if (campoPasadas == null) {
				for (Field f : PostChain.class.getDeclaredFields()) {
					if (!List.class.isAssignableFrom(f.getType()))
						continue;
					f.setAccessible(true);
					Object v = f.get(c);
					if (v instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof PostPass) {
						campoPasadas = f;
						break;
					}
				}
			}
			return campoPasadas == null ? null : (List<PostPass>) campoPasadas.get(c);
		} catch (Exception ex) {
			LOG.warn("No se pudieron leer las pasadas del filtro", ex);
			return null;
		}
	}
}
