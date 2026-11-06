package net.ruleta.client.escenas;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Escena de muerte del Momento Reviil (cabeza animada + rayos), definida en textures/muerte_animacion/escena.json. */
public final class AnimacionMuerte {
	private static final Logger LOG = LoggerFactory.getLogger("ruleta");
	private static final ResourceLocation SONIDO = ResourceLocation.fromNamespaceAndPath("ruleta", "muerte_animacion.sonido");
	private static final int ROJO = 16722474;
	private static boolean cargada;
	private static float[] tiempos;
	private static int[] cabezas;
	private static int[] fondos;
	private static float x0;
	private static float y0;
	private static float x1;
	private static float y1;
	private static float refCentroX;
	private static float refCentroY;
	private static float refAlto;
	private static float altoPantalla;
	private static float bajada;
	private static float fin;
	private static float[][] sacudida;
	private static float sacudidaTau;
	private static float[][] zoom;
	private static float zoomTau;
	private static float vinDesde;
	private static float vinHasta;
	private static float vinMax;
	private static float vinEntra;
	private static float vinSale;
	private static float glDesde;
	private static float glHasta;
	private static float glFranjas;
	private static float glCorre;
	private static float glHz;
	private static float finDesde;
	private static float finHasta;
	private static float sonidoDesde;
	private static Flujo cabeza;
	private static Flujo fondo;
	private static long inicio = -1L;
	private static boolean sonado;
	private static SoundInstance sonando;
	private static Component mensaje = Component.empty();

	private AnimacionMuerte() {
	}

	private static void cargar() {
		if (cargada)
			return;
		cargada = true;
		Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("ruleta", "textures/muerte_animacion/escena.json"));
		if (r.isEmpty()) {
			LOG.warn("Falta textures/muerte_animacion/escena.json");
			return;
		}

		try (InputStreamReader in = new InputStreamReader(r.get().open(), StandardCharsets.UTF_8)) {
			JsonObject j = JsonParser.parseReader(in).getAsJsonObject();
			JsonArray c = j.getAsJsonArray("cuadros");
			tiempos = new float[c.size()];
			cabezas = new int[c.size()];
			fondos = new int[c.size()];
			for (int i = 0; i < c.size(); i++) {
				JsonArray e = c.get(i).getAsJsonArray();
				tiempos[i] = e.get(0).getAsFloat();
				cabezas[i] = e.get(1).getAsInt();
				fondos[i] = e.get(2).getAsInt();
			}

			JsonArray caja = j.getAsJsonArray("caja");
			x0 = caja.get(0).getAsFloat();
			y0 = caja.get(1).getAsFloat();
			x1 = caja.get(2).getAsFloat();
			y1 = caja.get(3).getAsFloat();
			JsonObject enc = j.getAsJsonObject("encuadre");
			refCentroX = enc.getAsJsonArray("centro").get(0).getAsFloat();
			refCentroY = enc.getAsJsonArray("centro").get(1).getAsFloat();
			refAlto = enc.get("alto").getAsFloat();
			altoPantalla = enc.get("alto_pantalla").getAsFloat();
			bajada = enc.get("bajada").getAsFloat();
			fin = j.get("fin").getAsFloat();
			sacudida = pares(j.getAsJsonArray("sacudida"));
			sacudidaTau = j.get("sacudida_tau").getAsFloat();
			zoom = pares(j.getAsJsonArray("zoom"));
			zoomTau = j.get("zoom_tau").getAsFloat();
			JsonObject v = j.getAsJsonObject("vineta");
			vinDesde = v.get("desde").getAsFloat();
			vinHasta = v.get("hasta").getAsFloat();
			vinMax = v.get("max").getAsFloat();
			vinEntra = v.get("entrada").getAsFloat();
			vinSale = v.get("salida").getAsFloat();
			JsonObject g = j.getAsJsonObject("glitch");
			glDesde = g.get("desde").getAsFloat();
			glHasta = g.get("hasta").getAsFloat();
			glFranjas = g.get("franjas").getAsFloat();
			glCorre = g.get("corrimiento").getAsFloat();
			glHz = g.get("hz").getAsFloat();
			JsonObject f = j.getAsJsonObject("final");
			finDesde = f.get("desde").getAsFloat();
			finHasta = f.get("hasta").getAsFloat();
			sonidoDesde = j.get("sonido_desde").getAsFloat();
			JsonArray tc = j.getAsJsonArray("tam_cabeza");
			JsonArray tf = j.getAsJsonArray("tam_fondo");
			cabeza = new Flujo("muerte_cabeza", "textures/muerte_animacion/cabeza_anim_reviil", tc.get(0).getAsInt(), tc.get(1).getAsInt());
			fondo = new Flujo("muerte_fondo", "textures/muerte_animacion/impacto_y_rayos", tf.get(0).getAsInt(), tf.get(1).getAsInt());
		} catch (Exception ex) {
			LOG.warn("No se pudo leer la escena de muerte", ex);
			tiempos = null;
		}
	}

	private static float[][] pares(JsonArray a) {
		float[][] r = new float[a.size()][2];
		for (int i = 0; i < a.size(); i++) {
			r[i][0] = a.get(i).getAsJsonArray().get(0).getAsFloat();
			r[i][1] = a.get(i).getAsJsonArray().get(1).getAsFloat();
		}
		return r;
	}

	public static void empezar(Component texto) {
		cargar();
		if (tiempos == null)
			return;
		pararSonido();
		mensaje = texto == null ? Component.empty() : texto;
		inicio = System.nanoTime();
		sonado = false;
	}

	public static boolean activa() {
		return inicio >= 0L;
	}

	public static void parar() {
		inicio = -1L;
		pararSonido();
		soltar();
	}

	private static void soltar() {
		if (cabeza != null)
			cabeza.soltar();
		if (fondo != null)
			fondo.soltar();
	}

	private static void pararSonido() {
		if (sonando != null) {
			Minecraft.getInstance().getSoundManager().stop(sonando);
			sonando = null;
		}
	}

	private static int cuadro(float t) {
		int lo = 0;
		int hi = tiempos.length - 1;
		if (t < tiempos[0])
			return -1;
		while (lo < hi) {
			int m = lo + hi + 1 >>> 1;
			if (tiempos[m] <= t)
				lo = m;
			else
				hi = m - 1;
		}
		return lo;
	}

	private static int siguiente(int[] lista, int desde, int actual) {
		for (int i = desde + 1; i < lista.length && i < desde + 40; i++) {
			if (lista[i] >= 0 && lista[i] != actual)
				return lista[i];
		}
		return -1;
	}

	private static float impulsos(float[][] lista, float tau, float t) {
		float s = 0.0F;
		for (float[] p : lista) {
			float dt = t - p[0];
			if (dt >= 0.0F && dt < tau * 8.0F)
				s += p[1] * (float) Math.exp((double) (-dt / tau));
		}
		return s;
	}

	private static float azar(float a, float b) {
		float x = Mth.sin(a * 12.9898F + b * 78.233F) * 43758.547F;
		return x - (float) Mth.floor(x);
	}

	public static void dibujar(GuiGraphics ctx) {
		if (inicio < 0L)
			return;

		float t = (float) (System.nanoTime() - inicio) / 1.0E9F + tiempos[0];
		if (t >= fin) {
			parar();
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		if (!sonado && t >= sonidoDesde) {
			sonado = true;
			sonando = new SimpleSoundInstance(SONIDO, SoundSource.MASTER, 1.0F, 1.0F, RandomSource.create(), false, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
			mc.getSoundManager().play(sonando);
		}

		int w = ctx.guiWidth();
		int h = ctx.guiHeight();
		int n = cuadro(t);

		float vin = 0.0F;
		if (t >= vinDesde && t <= vinHasta)
			vin = vinMax * Math.min(Mth.clamp((t - vinDesde) / vinEntra, 0.0F, 1.0F), Mth.clamp((vinHasta - t) / vinSale, 0.0F, 1.0F));
		if (vin > 0.0F) {
			int a = Math.round(vin * 255.0F) << 24;
			int borde = a | 1703936;
			int tb = h / 4;
			ctx.fillGradient(0, 0, w, tb, borde, 0);
			ctx.fillGradient(0, h - tb, w, h, 0, borde);
		}

		float k = (float) h * altoPantalla / refAlto;
		float z = 1.0F + impulsos(zoom, zoomTau, t);
		float amp = impulsos(sacudida, sacudidaTau, t) * k;
		float dx = (azar(t * 60.0F, 1.0F) * 2.0F - 1.0F) * amp;
		float dy = (azar(t * 60.0F, 2.0F) * 2.0F - 1.0F) * amp;
		float alfa = 1.0F;
		if (t >= finDesde)
			alfa = 1.0F - Mth.clamp((t - finDesde) / Math.max(0.001F, finHasta - finDesde), 0.0F, 1.0F);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alfa);

		if (n >= 0 && fondos[n] >= 0 && fondo.mostrar(fondos[n], siguiente(fondos, n, fondos[n]))) {
			float fw = (float) w * z;
			float fh = (float) h * z;
			float fx = ((float) w - fw) / 2.0F + dx;
			float fy = ((float) h - fh) / 2.0F + dy;
			float extra = Math.abs(dx) + Math.abs(dy) + 2.0F;
			ctx.blit(fondo.id(), Math.round(fx - extra), Math.round(fy - extra), Math.round(fw + extra * 2.0F), Math.round(fh + extra * 2.0F),
					0.0F, 0.0F, fondo.ancho, fondo.alto, fondo.ancho, fondo.alto);
		} else if (n < 0 || fondos[n] < 0) {
			int prox = siguiente(fondos, Math.max(n, 0) - 1, -1);
			if (prox >= 0)
				fondo.mostrar(prox, -1);
		}

		float cx = (float) w / 2.0F;
		float cy = (float) h / 2.0F + bajada * (float) h;
		float bx = cx + (x0 - refCentroX) * k * z + dx;
		float by = cy + (y0 - refCentroY) * k * z + dy;
		float bw = (x1 - x0) * k * z;
		float bh = (y1 - y0) * k * z;
		if (n >= 0 && cabezas[n] >= 0 && cabeza.mostrar(cabezas[n], siguiente(cabezas, n, cabezas[n]))) {
			if (t >= glDesde && t <= glHasta) {
				int franjas = Math.max(1, Math.round(glFranjas));
				float paso = (float) Math.floor((double) (t * glHz));
				for (int i = 0; i < franjas; i++) {
					float v0 = (float) i / (float) franjas;
					float corre = (azar(paso, (float) i) * 2.0F - 1.0F) * glCorre * k * 2.0F;
					int sy = Math.round(by + bh * v0);
					int sh = Math.round(by + bh * (v0 + 1.0F / (float) franjas)) - sy;
					ctx.blit(cabeza.id(), Math.round(bx + corre), sy, Math.round(bw), sh, 0.0F, (float) cabeza.alto * v0, cabeza.ancho,
							Math.round((float) cabeza.alto / (float) franjas), cabeza.ancho, cabeza.alto);
				}
			} else {
				ctx.blit(cabeza.id(), Math.round(bx), Math.round(by), Math.round(bw), Math.round(bh), 0.0F, 0.0F, cabeza.ancho, cabeza.alto, cabeza.ancho, cabeza.alto);
			}
		} else if (n < 0) {
			int prox = siguiente(cabezas, -1, -1);
			if (prox >= 0)
				cabeza.mostrar(prox, -1);
		}

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		if (t >= sonidoDesde && mensaje != null) {
			Font f = mc.font;
			float aparece = Mth.clamp((t - sonidoDesde) / 0.25F, 0.0F, 1.0F) * alfa;
			int a = Math.max(4, Math.round(aparece * 255.0F));
			float escala = Math.max(1.0F, Math.min(2.0F, (float) w / 320.0F));
			Component texto = Component.empty().append(mensaje).withStyle(s -> s.withColor(ROJO));
			int ancho = f.width(texto);
			float ty = Math.min((float) h - 14.0F * escala, cy + (y1 - 95.0F - refCentroY) * k);
			ctx.pose().pushPose();
			ctx.pose().translate((float) w / 2.0F, ty, 0.0F);
			ctx.pose().scale(escala, escala, 1.0F);
			ctx.drawString(f, texto, -ancho / 2, 0, a << 24 | ROJO, true);
			ctx.pose().popPose();
		}

		RenderSystem.disableBlend();
	}
}
