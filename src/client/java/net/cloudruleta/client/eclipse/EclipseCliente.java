package net.cloudruleta.client.eclipse;

import java.lang.reflect.Method;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.client.agujero.AgujeroCliente;
import net.cloudruleta.client.linterna.LinternaCliente;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.client.ClientPlayerNetworkEvent;
import net.cloudruleta.compat.client.RenderLevelStageEvent;
import net.cloudruleta.compat.client.ViewportEvent;
import net.cloudruleta.compat.event.TickEvent;

/** Eclipse (cliente): fases, cielo con shader, oscurecimiento del mundo y sonidos. */
public final class EclipseCliente {
	private static final float ENTRA = 3.0F;
	private static final float PARCIAL = 24.0F;
	private static final float ANILLO = 2.5F;
	private static final float SALE_PARCIAL = 24.0F;
	private static final float SALE = 3.0F;
	private static final float OSCURO_PARCIAL = 0.25F;
	private static final float OSCURO_TOTAL = 1.0F;
	private static final float OSCURO_BLOQUES = 1.0F;
	private static final float OSCURO_SHADERS = 0.82F;
	private static final Vector3f SOL_FIJO = new Vector3f(0.55F, 0.8F, 0.24F).normalize();
	private static final SoundEvent AMBIENTE = sonido("eclipse.ambiente");
	private static final SoundEvent ANILLO_SND = sonido("eclipse.anillo");
	private static final SoundEvent TOTALIDAD_SND = sonido("eclipse.totalidad");
	private static final SoundEvent MUSICA = sonido("eclipse.musica");
	private static final float ADELANTO_ANILLO = 1.0F;
	private static SoundInstance ambiente;
	private static SoundInstance musica;
	private static float sonadoHasta = -1.0F;
	private static ShaderInstance shader;
	private static long desde = -1L;
	private static float totalidad = 30.0F;
	private static Vector3f solFijado;
	private static float cortado = -1.0F;
	private static long cortadoEn;
	private static long cacheEn = Long.MIN_VALUE;
	private static float[] cache;
	private static Boolean irisApi;
	private static Method irisInstancia;
	private static Method irisEnUso;

	private EclipseCliente() {
	}

	public static void shader(ShaderInstance s) {
		shader = s;
	}

	public static void recibir(int segundos, String lleva) {
		long ahora = System.nanoTime();
		if (segundos >= 0) {
			int ya = 0;
			try {
				ya = Integer.parseInt(lleva.trim());
			} catch (NumberFormatException ignorado) {
			}
			desde = ahora - (long) ya * 1000000000L;
			totalidad = (float) segundos;
			pararSonidos();
			sonadoHasta = ya > 0 ? (float) ya : -1.0F;
			solFijado = ya > 2 ? new Vector3f(SOL_FIJO) : solDeVerdad(0.0F);
			cortado = -1.0F;
		} else if (desde >= 0L && cortado < 0.0F) {
			cortado = segundos(ahora);
			cortadoEn = ahora;
		}
	}

	private static Vector3f solDeVerdad(float pt) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return new Vector3f(SOL_FIJO);
		float giro = mc.level.getTimeOfDay(pt) * 6.2831855F;
		Vector3f sol = new Vector3f(-Mth.sin(giro), Mth.cos(giro), 0.0F);
		return sol.y < 0.03F ? new Vector3f(SOL_FIJO) : sol;
	}

	public static void limpiar() {
		pararSonidos();
		desde = -1L;
		cortado = -1.0F;
	}

	public static boolean activo() {
		return desde >= 0L;
	}

	private static float segundos(long ahora) {
		return (float) (ahora - desde) / 1.0E9F;
	}

	private static float suave(float x) {
		x = Mth.clamp(x, 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	private static float inicioTotal() {
		return 29.5F;
	}

	private static float finTotal() {
		return inicioTotal() + totalidad;
	}

	private static float fin() {
		return finTotal() + ANILLO + SALE_PARCIAL + SALE;
	}

	private static float t(long ahora) {
		float t = segundos(ahora);
		if (cortado >= 0.0F) {
			float pasa = (float) (ahora - cortadoEn) / 1.0E9F;
			float salida = fin() - SALE;
			float u = suave(pasa / 4.0F);
			t = cortado < salida ? cortado + (salida - cortado) * u : cortado;
			if (pasa > 4.0F)
				t = salida + (pasa - 4.0F);
		}
		return t;
	}

	private static float[] fases(float t) {
		float opac = suave(t / ENTRA) * (1.0F - suave((t - (fin() - SALE)) / SALE));
		float a = ENTRA;
		float b = a + PARCIAL;
		float c = inicioTotal();
		float d = finTotal();
		float e = d + ANILLO;
		float f = e + SALE_PARCIAL;
		float luna;
		if (t < b)
			luna = 1.0F - suave((t - a) / PARCIAL) * 0.93F;
		else if (t < c)
			luna = 0.07F * (1.0F - suave((t - b) / ANILLO));
		else if (t < d)
			luna = 0.0F;
		else if (t < e)
			luna = -0.07F * suave((t - d) / ANILLO);
		else
			luna = -0.07F - 0.93F * suave((t - e) / SALE_PARCIAL);

		float queda = Mth.clamp(Math.abs(luna) * 2.4F / 2.05F, 0.0F, 1.0F);
		queda = (float) Math.pow((double) queda, 0.8D);
		float total = suave((t - c + 0.6F) / 1.2F) * (1.0F - suave((t - d + 0.6F) / 1.2F));
		float tapado = 1.0F - queda;
		float noche = Math.max(total, (float) Math.pow((double) tapado, 1.5D) * 0.9F) * opac;
		float anillo = (float) (Math.exp(-Math.pow((double) ((t - (c - 0.9F)) / 0.7F), 2.0D)) + Math.exp(-Math.pow((double) ((t - (d + 0.9F)) / 0.7F), 2.0D)));
		float corona = total;
		float oscuro = Math.max(total * OSCURO_TOTAL, Math.max(tapado * tapado * OSCURO_PARCIAL, (float) Math.pow((double) tapado, 6.0D) * OSCURO_TOTAL)) * opac;
		float bloques = Math.max(total, (float) Math.pow((double) tapado, 8.0D)) * OSCURO_BLOQUES * opac;
		return new float[]{luna, noche, Math.min(1.0F, anillo) * opac, opac, queda * (1.0F - total), corona * opac, oscuro, bloques};
	}

	private static SoundEvent sonido(String id) {
		return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cloudruleta", id));
	}

	private static void tocar(SoundEvent evento, float volumen) {
		Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(evento.getLocation(), SoundSource.MASTER, volumen, 1.0F, RandomSource.create(), false, 0,
				SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true));
	}

	private static void pararSonidos() {
		for (SoundInstance s : new SoundInstance[]{ambiente, musica}) {
			if (s != null)
				Minecraft.getInstance().getSoundManager().stop(s);
		}
		ambiente = null;
		musica = null;
	}

	private static float volumenAmbiente() {
		if (desde < 0L)
			return -1.0F;
		float t = t(System.nanoTime());
		if (t >= fin())
			return -1.0F;
		float[] f = fases(t);
		float tapado = 1.0F - f[4] - f[5];
		return f[3] * (0.12F + 0.55F * Math.max(0.0F, tapado)) * (1.0F - 0.6F * f[5]);
	}

	private static float volumenMusica() {
		if (desde < 0L)
			return -1.0F;
		float t = t(System.nanoTime());
		float entra = suave((t - inicioTotal()) / 2.0F);
		float sale = 1.0F - suave((t - finTotal()) / 4.0F);
		if (t > finTotal() + 4.0F || (cortado >= 0.0F && t > finTotal()))
			return -1.0F;
		return 0.85F * entra * sale * (cortado >= 0.0F ? 1.0F - suave((float) (System.nanoTime() - cortadoEn) / 3.0E9F) : 1.0F);
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ClientTickEvent e) {
		if (e.phase != TickEvent.Phase.END || desde < 0L)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;
		float t = t(System.nanoTime());
		if (t >= fin()) {
			pararSonidos();
			return;
		}

		if ((ambiente == null || (ambiente instanceof SonidoEclipse s && s.isStopped())) && t < fin() - SALE) {
			ambiente = new SonidoEclipse(AMBIENTE, true, EclipseCliente::volumenAmbiente);
			mc.getSoundManager().play(ambiente);
		}

		if (cortado < 0.0F) {
			cruzar(t, inicioTotal(), TOTALIDAD_SND, 1.0F);
			cruzar(t, finTotal() + 0.9F - ADELANTO_ANILLO, ANILLO_SND, 0.9F);
			if (musica == null && t >= inicioTotal() && t < finTotal()) {
				musica = new SonidoEclipse(MUSICA, true, EclipseCliente::volumenMusica);
				mc.getSoundManager().play(musica);
			}
		}
		sonadoHasta = t;
	}

	private static void cruzar(float t, float m, SoundEvent evento, float volumen) {
		if (sonadoHasta < m && t >= m)
			tocar(evento, volumen);
	}

	public static float oscuridad() {
		float[] f = ahora();
		return f == null ? 0.0F : f[6] * (1.0F - 0.8F * LinternaCliente.luzTotal());
	}

	public static float oscuridadBloques() {
		float[] f = ahora();
		return f == null ? 0.0F : f[7] * (1.0F - LinternaCliente.luzTotal());
	}

	public static boolean linternaEnMano() {
		return LinternaCliente.luz() > 0.0F;
	}

	private static float[] ahora() {
		if (desde < 0L)
			return null;
		long ahora = System.nanoTime();
		if (cache != null && ahora - cacheEn < 2000000L)
			return cache;
		float t = t(ahora);
		if (t >= fin()) {
			desde = -1L;
			cache = null;
			return null;
		}
		cache = fases(t);
		cacheEn = ahora;
		return cache;
	}

	@SubscribeEvent
	public static void alDibujarMundo(RenderLevelStageEvent e) {
		if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || desde < 0L || shader == null)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.level.dimension() != Level.OVERWORLD || mc.level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL)
			return;
		FogType dentro = e.getCamera().getFluidInCamera();
		if (dentro == FogType.LAVA || dentro == FogType.POWDER_SNOW)
			return;

		long ahora = System.nanoTime();
		float t = t(ahora);
		if (t >= fin()) {
			desde = -1L;
			return;
		}

		float[] f = fases(t);
		float pt = e.getPartialTick();
		Camera camara = e.getCamera();
		if (solFijado == null)
			solFijado = solDeVerdad(pt);
		Vector3f sol = new Vector3f(solFijado);
		Vector3f lado = new Vector3f(sol).cross(0.0F, 1.0F, 0.0F);
		if (lado.lengthSquared() < 1.0E-4F)
			lado.set(1.0F, 0.0F, 0.0F);
		lado.normalize();
		Vector3f arriba = new Vector3f(lado).cross(sol).normalize();
		lado.mul(0.8F).add(arriba.mul(0.6F)).normalize();
		Vec3 cielo = mc.level.getSkyColor(camara.getPosition(), pt);
		Matrix4f vista = new Matrix4f().rotateX(camara.getXRot() * 0.017453292F).rotateY((camara.getYRot() + 180.0F) * 0.017453292F);
		float lejos = (float) mc.options.getEffectiveRenderDistance() * 16.0F * 4.0F;

		float conShaders = shadersActivos() ? Math.max(f[6] - 0.125F, 0.0F) / OSCURO_TOTAL * OSCURO_SHADERS : 0.0F;
		float total = Math.max(f[7] * 0.995F, conShaders);
		float luz = LinternaCliente.luz();
		if ((luz > 0.0F || LinternaCliente.luzOtros() > 0.0F) && total > 0.005F)
			LinternaCliente.oscurecer(total, luz);
		else if (total > 0.005F)
			oscurecerMundo(1.0F - Math.min(0.995F, total));

		if (AgujeroCliente.tapaCielo())
			return;

		ShaderInstance sh = shader;
		sh.safeGetUniform("Tiempo").set(t);
		sh.safeGetUniform("SolDir").set(sol.x, sol.y, sol.z);
		sh.safeGetUniform("Lado").set(lado.x, lado.y, lado.z);
		sh.safeGetUniform("Cielo").set((float) cielo.x, (float) cielo.y, (float) cielo.z);
		sh.safeGetUniform("Horizonte").set(Mth.lerp(0.6F, (float) cielo.x, 0.78F), Mth.lerp(0.6F, (float) cielo.y, 0.86F), Mth.lerp(0.6F, (float) cielo.z, 1.0F));
		sh.safeGetUniform("Fase").set(f[0], f[1], f[2], f[3]);
		sh.safeGetUniform("Brillo").set(f[4], f[5]);

		Matrix4f proyeccionAntes = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting ordenAntes = RenderSystem.getVertexSorting();
		// 1.21.1: la pila de modelo-vista es un Matrix4fStack de JOML (antes un PoseStack)
		Matrix4fStack mv = RenderSystem.getModelViewStack();
		mc.getMainRenderTarget().bindWrite(true);
		RenderSystem.setProjectionMatrix(new Matrix4f(e.getProjectionMatrix()), VertexSorting.DISTANCE_TO_ORIGIN);
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
			cubo(lejos * 0.4F);
		} finally {
			RenderSystem.enableCull();
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
			mv.popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(proyeccionAntes, ordenAntes);
		}
	}

	/** Si hay un pack de shaders de Iris en uso (por reflexión, como en el original). */
	private static boolean shadersActivos() {
		try {
			if (irisApi == null) {
				irisApi = false;
				Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
				irisInstancia = c.getMethod("getInstance");
				irisEnUso = c.getMethod("isShaderPackInUse");
				irisApi = true;
			}
			return irisApi && (Boolean) irisEnUso.invoke(irisInstancia.invoke(null));
		} catch (Throwable ex) {
			irisApi = false;
			return false;
		}
	}

	private static void oscurecerMundo(float k) {
		Matrix4f proyAntes = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting ordenAntes = RenderSystem.getVertexSorting();
		Matrix4fStack mv = RenderSystem.getModelViewStack();
		mv.pushMatrix();
		mv.identity();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
		try {
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);
			RenderSystem.disableDepthTest();
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			b.addVertex(-1.0F, -1.0F, 0.0F).setColor(k, k, k, 1.0F);
			b.addVertex(1.0F, -1.0F, 0.0F).setColor(k, k, k, 1.0F);
			b.addVertex(1.0F, 1.0F, 0.0F).setColor(k, k, k, 1.0F);
			b.addVertex(-1.0F, 1.0F, 0.0F).setColor(k, k, k, 1.0F);
			BufferUploader.drawWithShader(b.buildOrThrow());
		} finally {
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			mv.popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(proyAntes, ordenAntes);
		}
	}

	@SubscribeEvent
	public static void alColorearNiebla(ViewportEvent.ComputeFogColor e) {
		if (desde < 0L)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.level.dimension() != Level.OVERWORLD)
			return;
		float[] f = fases(t(System.nanoTime()));
		float k = f[1] * 0.92F;
		e.setRed(Mth.lerp(k, e.getRed(), 0.22F));
		e.setGreen(Mth.lerp(k, e.getGreen(), 0.12F));
		e.setBlue(Mth.lerp(k, e.getBlue(), 0.1F));
	}

	@SubscribeEvent
	public static void alSalir(ClientPlayerNetworkEvent.LoggingOut e) {
		limpiar();
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
}
