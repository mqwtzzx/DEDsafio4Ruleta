package net.ruleta.client.reviil;

import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.supernova.client.ScreenEffectHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.ruleta.client.EfectosCliente;
import net.ruleta.client.OpcionesCliente;

/** Cielo del Momento Reviil: tormenta que cubre el cielo, rayo, grieta que se abre y la cabeza de Reviil. */
public final class CieloReviil {
	private static final float MUSICA_ORIGINAL = 46.05F;
	private static final float MUSICA_DURA = 49.22F;
	private static final float ESCALA = 1.0688382F;
	private static final float EXTIENDE = 11.75722F;
	private static final float CUBRE_TODO = 3.3F;
	private static final float RAYO = 17.101412F;
	private static final float RAYO_DURA = 3.8478177F;
	private static final float ABRE = 20.94923F;
	private static final float ABRE_DURA = 1.7101412F;
	private static final float CIERRA_GRIETA = 1.5F;
	private static final float RETIRA = 10.0F;
	private static final float[] GOLPES = {0.0F, 1.7101412F, 3.8478177F};
	private static final float SACUDIDA_RAYO = 2.2F;
	private static final float SACUDIDA_ABRE = 3.4F;
	private static final float DIST_CUBO = 0.3F;
	private static final float DIST_CABEZA = 0.72F;
	private static final SoundEvent RETUMBO = sonido("momento_reviil.retumbo");
	private static final SoundEvent DESPEJA = sonido("momento_reviil.despeja");
	private static final SoundEvent GRIETA_RAYO = sonido("momento_reviil.grieta_rayo");
	private static final SoundEvent GRIETA_ABRE = sonido("momento_reviil.grieta_abre");
	private static final SoundEvent GRIETA_CIERRA = sonido("momento_reviil.grieta_cierra");
	private static final SoundEvent MUSICA = sonido("momento_reviil.musica");
	private static final SoundEvent MUSICA_CIELO = sonido("momento_reviil.musica_cielo");
	private static ShaderInstance shader;
	private static long desde = -1L;
	private static boolean bloqueado;
	private static long acaba = -1L;
	private static float cubreAlAcabar;
	private static float largoAlAcabar;
	private static float abreAlAcabar;
	private static float angulo;
	private static float semilla;
	private static float bordeGiro;
	private static float tormentaSemilla;
	private static float sonadoHasta;
	private static SoundInstance musica;
	private static SoundInstance musicaCielo;
	private static SoundInstance retumbo;
	private static SoundInstance despeja;
	private static SoundInstance musicaSola;

	private CieloReviil() {
	}

	public static void shader(ShaderInstance s) {
		shader = s;
	}

	private static SoundEvent sonido(String id) {
		return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("ruleta", id));
	}

	public static void recibir(int valor) {
		long ahora = System.nanoTime();
		if (valor >= 0) {
			if (desde < 0L || acaba >= 0L) {
				bloqueado = ScreenEffectHandler.haySupernova;
				angulo = (float) (Math.random() * Math.PI * 2.0D);
				semilla = (float) (Math.random() * 100.0D);
				bordeGiro = (float) (Math.random() * Math.PI * 2.0D);
				tormentaSemilla = (float) (Math.random() * 50.0D);
			}

			desde = ahora - (long) valor * 1000000000L;
			acaba = -1L;
			sonadoHasta = valor > 0 ? (float) valor : -1.0F;
			SoundInstance seguida = temaSonando() ? musicaSola : null;
			if (seguida != null)
				musicaSola = null;

			pararSonidos();
			pararMusicaSola();
			if (seguida != null)
				musica = seguida;
			else if ((float) valor < MUSICA_DURA)
				musica = tocar(MUSICA);

			if ((float) valor >= 47.22F)
				musicaCielo = seguir(MUSICA_CIELO, true, CieloReviil::volumenMusicaCielo);
			if ((float) valor < EXTIENDE)
				retumbo = seguir(RETUMBO, false, CieloReviil::volumenRetumbo);
		} else if (desde >= 0L && acaba < 0L) {
			float t = segundos(ahora);
			cubreAlAcabar = cubre(t);
			largoAlAcabar = largo(t);
			abreAlAcabar = abre(t);
			acaba = ahora;
			parar(musica);
			parar(retumbo);
			musica = null;
			retumbo = null;
			if (largoAlAcabar > 0.0F)
				tocar(GRIETA_CIERRA);
		}
	}

	public static void limpiar() {
		desde = -1L;
		acaba = -1L;
		pararSonidos();
		pararMusicaSola();
	}

	public static float neblina() {
		if (!visible())
			return 0.0F;
		long ahora = System.nanoTime();
		float k = suave(segundos(ahora) / 6.0F);
		if (acaba >= 0L)
			k *= 1.0F - suave((float) (ahora - acaba) / 1.0E9F / 4.0F);
		return k;
	}

	public static boolean musicaMomentoSonando() {
		SoundManager sm = Minecraft.getInstance().getSoundManager();
		return (musica != null && acaba < 0L && sm.isActive(musica)) || (musicaSola != null && sm.isActive(musicaSola));
	}

	public static void musicaSinCielo() {
		if (desde >= 0L && acaba < 0L)
			return;
		if (temaSonando())
			return;
		pararMusicaSola();
		musicaSola = tocar(MUSICA);
	}

	public static void temaAdelantado() {
		if (desde >= 0L && acaba < 0L)
			return;
		pararMusicaSola();
		musicaSola = tocar(MUSICA);
	}

	private static boolean temaSonando() {
		return musicaSola != null && Minecraft.getInstance().getSoundManager().isActive(musicaSola);
	}

	public static void pararMusicaSola() {
		parar(musicaSola);
		musicaSola = null;
	}

	public static boolean activo() {
		return desde >= 0L;
	}

	public static boolean visible() {
		if (desde < 0L || !OpcionesCliente.reviilCielo)
			return false;
		if (bloqueado && !ScreenEffectHandler.haySupernova)
			bloqueado = false;
		return !bloqueado;
	}

	private static void pararSonidos() {
		for (SoundInstance s : new SoundInstance[]{musica, musicaCielo, retumbo, despeja})
			parar(s);
		musica = null;
		musicaCielo = null;
		retumbo = null;
		despeja = null;
	}

	private static void parar(SoundInstance s) {
		if (s != null)
			Minecraft.getInstance().getSoundManager().stop(s);
	}

	private static SoundInstance tocar(SoundEvent evento) {
		SoundInstance s = new SimpleSoundInstance(evento.getLocation(), SoundSource.MASTER, 1.0F, 1.0F, RandomSource.create(), false, 0,
				SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
		Minecraft.getInstance().getSoundManager().play(s);
		return s;
	}

	private static SoundInstance seguir(SoundEvent evento, boolean repetir, Supplier<float[]> estado) {
		SoundInstance s = new SonidoReviil(evento, repetir, estado);
		Minecraft.getInstance().getSoundManager().play(s);
		return s;
	}

	private static float segundos(long ahora) {
		return (float) (ahora - desde) / 1.0E9F;
	}

	private static float suave(float x) {
		x = Math.max(0.0F, Math.min(1.0F, x));
		return x * x * (3.0F - 2.0F * x);
	}

	private static float sale(float x) {
		x = Math.max(0.0F, Math.min(1.0F, x));
		return 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
	}

	private static float avance(float x) {
		x = Math.max(0.0F, Math.min(1.0F, x));
		return (float) ((double) x - 0.083D * (1.0D - Math.exp((double) (-x) / 0.083D))) / 0.917F;
	}

	private static float cubre(float t) {
		return CUBRE_TODO * avance(t / EXTIENDE);
	}

	private static float largo(float t) {
		float u = (t - RAYO) / RAYO_DURA;
		return 0.35F * sale((u - 0.02F) / 0.17F) + 0.65F * sale((u - 0.44F) / 0.14F);
	}

	private static float abre(float t) {
		return suave((t - ABRE) / ABRE_DURA);
	}

	private static float[] golpes(float t) {
		if (t < RAYO || t > 22.149231F)
			return new float[]{0.0F, 0.0F, 0.0F};

		float destello = 0.0F;
		float radio = 0.0F;
		float aro = 0.0F;
		for (float m : GOLPES) {
			float dt = t - RAYO - m;
			if (dt < 0.0F)
				continue;
			destello = Math.max(destello, dt < 0.03F ? dt / 0.03F : (float) Math.exp((double) (-(dt - 0.03F) / 0.12F)));
			float da = dt - 0.1F;
			if (da >= 0.0F && da < 0.9F) {
				float a = (float) Math.exp((double) (-da / 0.22F)) * Math.min(1.0F, da / 0.03F);
				if (a > aro) {
					aro = a;
					radio = 0.15F + 1.9F * da;
				}
			}
		}
		return new float[]{destello, radio, aro};
	}

	private static float acabando(long ahora) {
		return acaba >= 0L ? (float) (ahora - acaba) / 1.0E9F : -1.0F;
	}

	private static float esperaGrieta() {
		return largoAlAcabar > 0.0F ? CIERRA_GRIETA : 0.0F;
	}

	private static float cubreAcabando(float f) {
		return cubreAlAcabar * (1.0F - avance((f - esperaGrieta()) / RETIRA));
	}

	private static float cubreAhora() {
		long ahora = System.nanoTime();
		float f = acabando(ahora);
		return f >= 0.0F ? cubreAcabando(f) : cubre(segundos(ahora));
	}

	private static float[] grave(float c) {
		float volumen = (0.4F + 0.6F * suave(c / 0.8F)) * (1.0F - suave((c - 2.6F) / 0.7F));
		return new float[]{volumen, 0.85F + 0.15F * suave(c / 2.2F)};
	}

	private static float[] volumenRetumbo() {
		if (desde < 0L || acaba >= 0L)
			return null;
		float t = segundos(System.nanoTime());
		return t > 12.75722F ? null : grave(cubre(t));
	}

	private static float[] volumenDespeja() {
		long ahora = System.nanoTime();
		float f = acabando(ahora);
		if (desde < 0L || f < 0.0F)
			return null;
		return f < esperaGrieta() ? new float[]{0.0F, 0.85F} : grave(cubreAcabando(f));
	}

	private static float[] volumenMusicaCielo() {
		if (desde < 0L)
			return null;
		float t = segundos(System.nanoTime());
		float libre = suave((t - 47.22F) / 3.0F);
		return new float[]{suave((cubreAhora() - 2.6F) / 0.7F) * libre, 1.0F};
	}

	public static void tick() {
		if (desde < 0L)
			return;
		long ahora = System.nanoTime();
		float f = acabando(ahora);
		if (f >= 0.0F) {
			if (despeja == null && f >= esperaGrieta())
				despeja = seguir(DESPEJA, false, CieloReviil::volumenDespeja);
			if (f > esperaGrieta() + RETIRA) {
				desde = -1L;
				acaba = -1L;
				pararSonidos();
			}
			return;
		}

		float t = segundos(ahora);
		if (musicaCielo == null && t >= 47.22F)
			musicaCielo = seguir(MUSICA_CIELO, true, CieloReviil::volumenMusicaCielo);

		for (float m : GOLPES) {
			if (cruzar(t, RAYO + m, GRIETA_RAYO) && visible())
				EfectosCliente.golpe(SACUDIDA_RAYO);
		}
		if (cruzar(t, ABRE, GRIETA_ABRE) && visible())
			EfectosCliente.golpe(SACUDIDA_ABRE);
		sonadoHasta = t;
	}

	private static boolean cruzar(float t, float m, SoundEvent evento) {
		if (sonadoHasta < m && t >= m) {
			tocar(evento);
			return true;
		}
		return false;
	}

	public static float[] niebla(float r, float g, float b) {
		if (!visible())
			return null;
		float k = suave((cubreAhora() - 1.35F) / 0.9F);
		if (k <= 0.0F)
			return null;
		return new float[]{r + (0.06F - r) * k, g + (0.0F - g) * k, b + (0.006F - b) * k};
	}

	public static void dibujar(Matrix4f vista, float lejos) {
		ShaderInstance sh = shader;
		if (desde < 0L || sh == null)
			return;

		long ahora = System.nanoTime();
		float f = acabando(ahora);
		float cubre;
		float largo;
		float abre;
		float[] luz;
		if (f >= 0.0F) {
			float cierra = 1.0F - suave(f / CIERRA_GRIETA);
			cubre = cubreAcabando(f);
			largo = largoAlAcabar * cierra;
			abre = abreAlAcabar * cierra;
			luz = new float[]{0.0F, 0.0F, 0.0F};
		} else {
			float t = segundos(ahora);
			cubre = cubre(t);
			largo = largo(t);
			abre = abre(t);
			luz = golpes(t);
		}

		fijar(sh, "Tiempo", new float[]{(float) (ahora - desde) / 1.0E9F % 3600.0F});
		fijar(sh, "Cubre", new float[]{cubre});
		fijar(sh, "Forma", new float[]{bordeGiro, tormentaSemilla, 0.0F, 0.0F});
		fijar(sh, "GrietaForma", new float[]{angulo, semilla, luz[1], luz[2]});
		float tiempo = (float) (ahora - desde) / 1.0E9F % 3600.0F;
		if (abre > 0.001F) {
			pasada(sh, vista, largo, abre, 1.0F, luz[0], lejos);
			CabezaReviil.dibujar(vista, angulo, CabezaReviil.centroGrieta(semilla), abre, tiempo, lejos * DIST_CABEZA);
			pasada(sh, vista, largo, abre, 2.0F, luz[0], lejos);
		} else {
			pasada(sh, vista, largo, abre, 0.0F, luz[0], lejos);
		}
	}

	private static void pasada(ShaderInstance sh, Matrix4f vista, float largo, float abre, float modo, float destello, float lejos) {
		fijar(sh, "Grieta", new float[]{largo, abre, modo, destello});
		// 1.21.1: la pila de modelo-vista es un Matrix4fStack de JOML (antes un PoseStack)
		Matrix4fStack mv = RenderSystem.getModelViewStack();
		mv.pushMatrix();
		mv.identity();
		mv.mul(vista);
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setShader(() -> sh);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		try {
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(515);
			RenderSystem.disableCull();
			cubo(lejos * DIST_CUBO);
		} finally {
			RenderSystem.enableCull();
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			mv.popMatrix();
			RenderSystem.applyModelViewMatrix();
		}
	}

	private static void cubo(float s) {
		float[][] caras = {
				{-s, s, -s, s, s, -s, s, s, s, -s, s, s},
				{-s, -s, -s, -s, -s, s, s, -s, s, s, -s, -s},
				{-s, -s, -s, s, -s, -s, s, s, -s, -s, s, -s},
				{s, -s, s, -s, -s, s, -s, s, s, s, s, s},
				{-s, -s, s, -s, -s, -s, -s, s, -s, -s, s, s},
				{s, -s, -s, s, -s, s, s, s, s, s, s, -s}
		};
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		for (float[] c : caras) {
			for (int k = 0; k < 12; k += 3)
				b.addVertex(c[k], c[k + 1], c[k + 2]);
		}
		BufferUploader.drawWithShader(b.buildOrThrow());
	}

	private static void fijar(ShaderInstance sh, String nombre, float[] v) {
		Uniform u = sh.getUniform(nombre);
		if (u == null)
			return;
		if (v.length == 4)
			u.set(v[0], v[1], v[2], v[3]);
		else
			u.set(v[0]);
	}
}
