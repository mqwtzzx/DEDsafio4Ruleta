package net.ruleta.client.reviil;

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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvent;
import net.ruleta.client.escenas.Flujo;

/** Animación de entrada del Momento Reviil (vídeo + celdas de color), definida en textures/momento_reviil/escena.json. */
public final class AnimacionReviil {
	private static final Logger LOG = LoggerFactory.getLogger("ruleta");
	public static final float DURACION = 15.2F;
	public static final float RETRASO = 0.6F;
	private static final SoundEvent SONIDO = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("ruleta", "momento_reviil.animacion"));
	private static boolean cargada;
	private static float[] tiempos;
	private static int[] cuadros;
	private static float[] celdasT;
	private static int[][] celdasBlanco;
	private static int[][] celdasRojo;
	private static int colorBlanco;
	private static int colorRojo;
	private static float x0;
	private static float y0;
	private static float x1;
	private static float y1;
	private static float refX;
	private static float refY;
	private static float refAlto;
	private static float altoPantalla;
	private static float bajada;
	private static Flujo flujo;
	private static long inicio = -1L;
	private static SonidoReviil sonando;
	private static boolean sonidoVivo;
	private static final float ENTRADA_DURA = 15.25F;
	private static long temaEn = -1L;

	private AnimacionReviil() {
	}

	private static void cargar() {
		if (cargada)
			return;
		cargada = true;
		Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("ruleta", "textures/momento_reviil/escena.json"));
		if (r.isEmpty()) {
			LOG.warn("Falta textures/momento_reviil/escena.json");
			return;
		}

		try (InputStreamReader in = new InputStreamReader(r.get().open(), StandardCharsets.UTF_8)) {
			JsonObject j = JsonParser.parseReader(in).getAsJsonObject();
			JsonArray c = j.getAsJsonArray("cuadros");
			tiempos = new float[c.size()];
			cuadros = new int[c.size()];
			for (int i = 0; i < c.size(); i++) {
				tiempos[i] = c.get(i).getAsJsonArray().get(0).getAsFloat();
				cuadros[i] = c.get(i).getAsJsonArray().get(1).getAsInt();
			}

			JsonArray caja = j.getAsJsonArray("caja");
			x0 = caja.get(0).getAsFloat();
			y0 = caja.get(1).getAsFloat();
			x1 = caja.get(2).getAsFloat();
			y1 = caja.get(3).getAsFloat();
			JsonObject enc = j.getAsJsonObject("encuadre");
			refX = enc.getAsJsonArray("centro").get(0).getAsFloat();
			refY = enc.getAsJsonArray("centro").get(1).getAsFloat();
			refAlto = enc.get("alto").getAsFloat();
			altoPantalla = enc.get("alto_pantalla").getAsFloat();
			bajada = enc.get("bajada").getAsFloat();
			JsonObject cel = j.getAsJsonObject("celdas");
			colorBlanco = rgb(cel.getAsJsonArray("blanco"));
			colorRojo = rgb(cel.getAsJsonArray("rojo"));
			JsonArray cc = cel.getAsJsonArray("cuadros");
			celdasT = new float[cc.size()];
			celdasBlanco = new int[cc.size()][];
			celdasRojo = new int[cc.size()][];
			for (int i = 0; i < cc.size(); i++) {
				JsonArray e = cc.get(i).getAsJsonArray();
				celdasT[i] = e.get(0).getAsFloat();
				celdasBlanco[i] = enteros(e.get(1).getAsJsonArray());
				celdasRojo[i] = enteros(e.get(2).getAsJsonArray());
			}

			JsonArray tam = j.getAsJsonArray("tam");
			flujo = new Flujo("animacion_reviil", "textures/momento_reviil/animacion_reviil", tam.get(0).getAsInt(), tam.get(1).getAsInt());
		} catch (Exception ex) {
			LOG.warn("No se pudo leer la escena de Reviil", ex);
			tiempos = null;
		}
	}

	private static int rgb(JsonArray a) {
		return a.get(0).getAsInt() << 16 | a.get(1).getAsInt() << 8 | a.get(2).getAsInt();
	}

	private static int[] enteros(JsonArray a) {
		int[] r = new int[a.size()];
		for (int i = 0; i < r.length; i++)
			r[i] = a.get(i).getAsInt();
		return r;
	}

	public static void empezar() {
		empezar(false);
	}

	public static void empezar(boolean conTema) {
		parar();
		cargar();
		if (tiempos == null)
			return;
		long ahora = System.nanoTime();
		inicio = ahora + 600000000L;
		// (long) (ENTRADA_DURA * 1.0E9F) calculado en float por el compilador del original
		temaEn = conTema ? ahora + 15249999872L : -1L;
		sonidoVivo = true;
		sonando = new SonidoReviil(SONIDO, false, () -> sonidoVivo ? new float[]{1.0F, 1.0F} : null);
		Minecraft.getInstance().getSoundManager().play(sonando);
	}

	public static void parar() {
		terminar();
		temaEn = -1L;
		sonidoVivo = false;
		if (sonando != null) {
			Minecraft.getInstance().getSoundManager().stop(sonando);
			sonando = null;
		}
	}

	private static void terminar() {
		inicio = -1L;
		if (flujo != null)
			flujo.soltar();
	}

	public static boolean activa() {
		return inicio >= 0L;
	}

	public static boolean musicaSonando() {
		return sonando != null && sonidoVivo && Minecraft.getInstance().getSoundManager().isActive(sonando);
	}

	private static int buscar(float[] lista, float t) {
		if (lista.length == 0 || t < lista[0])
			return -1;
		int lo = 0;
		int hi = lista.length - 1;
		while (lo < hi) {
			int m = lo + hi + 1 >>> 1;
			if (lista[m] <= t)
				lo = m;
			else
				hi = m - 1;
		}
		return lo;
	}

	public static void dibujar(GuiGraphics ctx) {
		if (temaEn >= 0L && System.nanoTime() >= temaEn) {
			temaEn = -1L;
			CieloReviil.temaAdelantado();
		}
		if (inicio < 0L)
			return;
		float t = (float) (System.nanoTime() - inicio) / 1.0E9F;
		if (t < 0.0F)
			return;
		if (t >= DURACION) {
			terminar();
			return;
		}

		int w = ctx.guiWidth();
		int h = ctx.guiHeight();
		float k = (float) h * altoPantalla / refAlto;
		float cx = (float) w / 2.0F;
		float cy = (float) h / 2.0F + bajada * (float) h;
		int n = buscar(tiempos, t);
		int prox = -1;
		for (int i = Math.max(0, n + 1); i < cuadros.length && i < n + 30; i++) {
			if (cuadros[i] >= 0 && (n < 0 || cuadros[i] != cuadros[n])) {
				prox = cuadros[i];
				break;
			}
		}

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		if (n >= 0 && cuadros[n] >= 0) {
			if (flujo.mostrar(cuadros[n], prox)) {
				int bx = Math.round(cx + (x0 - refX) * k);
				int by = Math.round(cy + (y0 - refY) * k);
				int bw = Math.round((x1 - x0) * k);
				int bh = Math.round((y1 - y0) * k);
				ctx.blit(flujo.id(), bx, by, bw, bh, 0.0F, 0.0F, flujo.ancho, flujo.alto, flujo.ancho, flujo.alto);
			}
		} else if (prox >= 0) {
			flujo.mostrar(prox, -1);
		}

		int c = buscar(celdasT, t);
		if (c >= 0 && (c + 1 < celdasT.length ? t < celdasT[c + 1] : t < celdasT[c] + 0.07F) && c < celdasT.length - 1) {
			celdas(ctx, celdasBlanco[c], colorBlanco, cx, cy, k);
			celdas(ctx, celdasRojo[c], colorRojo, cx, cy, k);
		}
		RenderSystem.disableBlend();
	}

	private static void celdas(GuiGraphics ctx, int[] tramos, int color, float cx, float cy, float k) {
		for (int i = 0; i + 2 < tramos.length; i += 3) {
			float fy = cy + ((float) tramos[i] - refY) * k;
			float fx0 = cx + ((float) tramos[i + 1] - refX) * k;
			float fx1 = cx + ((float) (tramos[i + 2] + 1) - refX) * k;
			ctx.fill(Math.round(fx0), Math.round(fy), Math.max(Math.round(fx0) + 1, Math.round(fx1)), Math.max(Math.round(fy) + 1, Math.round(fy + k)), -16777216 | color);
		}
	}
}
