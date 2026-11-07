package net.cloudruleta.client.phora;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.client.EfectosCliente;
import net.cloudruleta.client.agujero.AgujeroCliente;
import net.cloudruleta.client.reviil.CieloReviil;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.client.ClientPlayerNetworkEvent;
import net.cloudruleta.compat.client.Formatos;
import net.cloudruleta.compat.client.RenderLevelStageEvent;
import net.cloudruleta.compat.client.ViewportEvent;
import net.cloudruleta.compat.event.TickEvent;
import net.cloudruleta.phora.MeteoritoEntity;

/** Momento Phora (cliente): cielo, música, meteoritos con estela y los hongos de polvo de los impactos. */
public final class PhoraCliente {
	/** 1.21.1 ya no trae DefaultVertexFormat.POSITION_COLOR_TEX: ver compat.client.Formatos (mismo formato que en 1.20.1). */
	public static final VertexFormat POSITION_COLOR_TEX = Formatos.POSITION_COLOR_TEX;
	private static final SoundEvent TEMA_INTRO = sonido("phora.tema_intro");
	private static final SoundEvent TEMA_BUCLE = sonido("phora.tema_bucle");
	private static final SoundEvent VOZ = sonido("phora.voz");
	private static final SoundEvent SILBIDO = sonido("phora.silbido");
	private static final SoundEvent IMPACTO = sonido("phora.impacto");
	private static final float INTRO_DURA = 56.66F;
	private static final float ENTRA = 4.0F;
	private static final float SALE = 4.0F;
	private static final float RADIO = 1.7F;
	private static final float ESTELA = 58.0F;
	private static final float HUMO = 80.0F;
	private static final float POLVO_DURA = 26.0F;
	private static final float VINO_R = 0.3F;
	private static final float VINO_G = 0.035F;
	private static final float VINO_B = 0.06F;
	private static ShaderInstance shaderCielo;
	private static ShaderInstance shader;
	private static long desde = -1L;
	private static long acaba = -1L;
	private static SoundInstance intro;
	private static SoundInstance bucle;
	private static final Map<Integer, SoundInstance> SILBIDOS = new HashMap<>();
	private static final List<Hongo> POLVOS = new ArrayList<>();
	private static float destello;
	private static long destelloEn;

	private PhoraCliente() {
	}

	public static void shaderCielo(ShaderInstance s) {
		shaderCielo = s;
	}

	public static void shader(ShaderInstance s) {
		shader = s;
	}

	private static SoundEvent sonido(String id) {
		return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cloudruleta", id));
	}

	public static void recibir(int valor) {
		long ahora = System.nanoTime();
		if (valor >= 0) {
			if (desde < 0L || acaba >= 0L) {
				pararMusica();
				desde = ahora - (long) valor * 1000000000L;
				acaba = -1L;
				Minecraft mc = Minecraft.getInstance();
				if ((float) valor < 55.66F) {
					intro = new SonidoPhora(TEMA_INTRO, false, PhoraCliente::volumenMusica, null);
					mc.getSoundManager().play(intro);
				}
				if (valor <= 1)
					voz();
			}
		} else if (desde >= 0L && acaba < 0L) {
			acaba = ahora;
		}
	}

	public static void voz() {
		Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(VOZ.getLocation(), SoundSource.MASTER, 1.0F, 1.0F, RandomSource.create(), false, 0,
				SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true));
	}

	public static void impacto(int potencia, String donde) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;
		String[] p = donde.trim().split(" ");
		if (p.length < 3)
			return;
		Vec3 c;
		try {
			c = new Vec3(Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]));
		} catch (NumberFormatException ex) {
			return;
		}

		double d = mc.player.getEyePosition().distanceTo(c);
		POLVOS.add(new Hongo(c, (float) potencia / 60.0F));
		float f = (float) Math.max(0.0D, 1.0D - d / 260.0D);
		if (f > 0.0F) {
			destello = Math.max(destello * fadeDestello(), (float) Math.pow((double) f, 1.4D));
			destelloEn = System.nanoTime();
		}
		if (d < 220.0D)
			EfectosCliente.golpe((float) (7.0D * (1.0D - d / 220.0D)));

		boolean cerca = d < 40.0D;
		mc.getSoundManager().play(new SimpleSoundInstance(IMPACTO.getLocation(), SoundSource.MASTER, cerca ? 2.0F : 9.0F, 0.9F + mc.level.random.nextFloat() * 0.2F,
				RandomSource.create(), false, 0, cerca ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR, c.x, c.y, c.z, cerca));
	}

	public static void limpiar() {
		pararMusica();
		desde = -1L;
		acaba = -1L;
		POLVOS.clear();
		SILBIDOS.clear();
		destello = 0.0F;
	}

	private static void pararMusica() {
		for (SoundInstance s : new SoundInstance[]{intro, bucle}) {
			if (s != null)
				Minecraft.getInstance().getSoundManager().stop(s);
		}
		intro = null;
		bucle = null;
	}

	private static float suave(float x) {
		x = Mth.clamp(x, 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	private static float segundos() {
		return (float) (System.nanoTime() - desde) / 1.0E9F;
	}

	public static boolean enCurso() {
		return desde >= 0L && acaba < 0L;
	}

	private static float fuerza() {
		if (desde < 0L)
			return 0.0F;
		float f = suave(segundos() / ENTRA);
		if (acaba >= 0L) {
			float s = (float) (System.nanoTime() - acaba) / 1.0E9F;
			f *= 1.0F - suave(s / SALE);
			if (s > SALE) {
				desde = -1L;
				acaba = -1L;
				return 0.0F;
			}
		}
		return f;
	}

	private static float volumenMusica() {
		if (desde < 0L)
			return -1.0F;
		if (acaba >= 0L) {
			float s = (float) (System.nanoTime() - acaba) / 1.0E9F;
			return s > 3.0F ? -1.0F : 1.0F - suave(s / 3.0F);
		}
		return 1.0F;
	}

	private static float fadeDestello() {
		return (float) Math.exp((double) (-((float) (System.nanoTime() - destelloEn) / 1.0E9F) / 0.35F));
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ClientTickEvent e) {
		if (e.phase != TickEvent.Phase.END)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;

		if (desde >= 0L && acaba < 0L && bucle == null && segundos() >= 56.61F) {
			bucle = new SonidoPhora(TEMA_BUCLE, true, PhoraCliente::volumenMusica, null);
			mc.getSoundManager().play(bucle);
		}

		for (Entity ent : mc.level.entitiesForRendering()) {
			if (ent instanceof MeteoritoEntity m && !SILBIDOS.containsKey(m.getId()) && mc.player != null && mc.player.distanceToSqr(m) < 48400.0D) {
				int id = m.getId();
				SoundInstance s = new SonidoPhora(SILBIDO, false, () -> m.isRemoved() ? -1.0F : 6.0F, () -> m.isRemoved() ? null : m.position());
				SILBIDOS.put(id, s);
				mc.getSoundManager().play(s);
			}
		}

		SILBIDOS.entrySet().removeIf(x -> mc.level.getEntity(x.getKey()) == null && !mc.getSoundManager().isActive(x.getValue()));
		POLVOS.removeIf(p -> p.edad() > POLVO_DURA);
	}

	@SubscribeEvent
	public static void alSalir(ClientPlayerNetworkEvent.LoggingOut e) {
		limpiar();
	}

	@SubscribeEvent
	public static void alColorearNiebla(ViewportEvent.ComputeFogColor e) {
		float k = fuerza() * 0.92F;
		if (k <= 0.0F) {
			float v = CieloReviil.neblina() * 0.92F;
			if (v > 0.0F) {
				e.setRed(Mth.lerp(v, e.getRed(), VINO_R));
				e.setGreen(Mth.lerp(v, e.getGreen(), VINO_G));
				e.setBlue(Mth.lerp(v, e.getBlue(), VINO_B));
			}
			return;
		}
		e.setRed(Mth.lerp(k, e.getRed(), 0.8F));
		e.setGreen(Mth.lerp(k, e.getGreen(), 0.67F));
		e.setBlue(Mth.lerp(k, e.getBlue(), 0.5F));
	}

	@SubscribeEvent
	public static void alDibujar(RenderLevelStageEvent e) {
		if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;

		float pt = e.getPartialTick();
		float fuerza = fuerza();
		boolean hayMeteoritos = false;
		for (Entity ent : mc.level.entitiesForRendering()) {
			if (ent instanceof MeteoritoEntity) {
				hayMeteoritos = true;
				break;
			}
		}

		float flash = destello * fadeDestello();
		float vino = fuerza > 0.0F ? 0.0F : CieloReviil.neblina();
		if (fuerza <= 0.0F && vino <= 0.0F && !hayMeteoritos && POLVOS.isEmpty() && flash < 0.003F)
			return;

		Camera cam = e.getCamera();
		Vec3 c = cam.getPosition();
		Matrix4f vista = new Matrix4f().rotateX(cam.getXRot() * 0.017453292F).rotateY((cam.getYRot() + 180.0F) * 0.017453292F);
		float lejos = (float) mc.options.getEffectiveRenderDistance() * 16.0F * 4.0F;
		float t = (float) (System.nanoTime() % 3600000000000L) / 1.0E9F;
		Matrix4f proyAntes = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting ordenAntes = RenderSystem.getVertexSorting();
		// 1.21.1: la pila de modelo-vista es un Matrix4fStack de JOML (antes un PoseStack)
		Matrix4fStack mv = RenderSystem.getModelViewStack();
		mc.getMainRenderTarget().bindWrite(true);
		RenderSystem.setProjectionMatrix(new Matrix4f(e.getProjectionMatrix()), VertexSorting.DISTANCE_TO_ORIGIN);
		mv.pushMatrix();
		mv.identity();
		mv.mul(vista);
		RenderSystem.applyModelViewMatrix();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(515);
		try {
			if (fuerza > 0.0F && shaderCielo != null && mc.level.dimensionType().hasSkyLight() && !AgujeroCliente.tapaCielo())
				cielo(lejos, t, fuerza, flash);
			if (shader != null && fuerza > 0.0F)
				plano(0.8F, 0.66F, 0.48F, 0.3F * fuerza, false);
			if (shader != null && vino > 0.0F)
				plano(VINO_R, VINO_G, VINO_B, 0.34F * vino, false);
			if (shader != null) {
				for (Entity ent : mc.level.entitiesForRendering()) {
					if (ent instanceof MeteoritoEntity m)
						meteorito(m, c, pt, t, cam);
				}
				for (Hongo p : POLVOS)
					p.dibujar(c, cam, t);
				if (flash > 0.003F)
					plano(1.0F, 0.92F, 0.74F, Math.min(1.0F, flash), true);
			}
		} finally {
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
			mv.popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(proyAntes, ordenAntes);
		}
	}

	private static void cielo(float lejos, float t, float fuerza, float flash) {
		ShaderInstance sh = shaderCielo;
		sh.safeGetUniform("Tiempo").set(t % 3600.0F);
		sh.safeGetUniform("Fuerza").set(fuerza);
		sh.safeGetUniform("Destello").set(flash);
		RenderSystem.setShader(() -> sh);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		float s = lejos * 0.3F;
		float[][] caras = {
				{-s, s, -s, s, s, -s, s, s, s, -s, s, s},
				{-s, -s, -s, -s, -s, s, s, -s, s, s, -s, -s},
				{-s, -s, -s, s, -s, -s, s, s, -s, -s, s, -s},
				{s, -s, s, -s, -s, s, -s, s, s, s, s, s},
				{-s, -s, s, -s, -s, -s, -s, s, -s, -s, s, s},
				{s, -s, -s, s, -s, s, s, s, s, s, s, -s}
		};
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		for (float[] q : caras) {
			for (int k = 0; k < 12; k += 3)
				b.addVertex(q[k], q[k + 1], q[k + 2]);
		}
		BufferUploader.drawWithShader(b.buildOrThrow());
	}

	private static ShaderInstance usar(float modo, float t, float semilla, boolean sumar) {
		ShaderInstance sh = shader;
		sh.safeGetUniform("Modo").set(modo);
		sh.safeGetUniform("Tiempo").set(t % 3600.0F);
		sh.safeGetUniform("Semilla").set(semilla);
		RenderSystem.setShader(() -> sh);
		RenderSystem.enableBlend();
		if (sumar)
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		else
			RenderSystem.defaultBlendFunc();
		return sh;
	}

	private static void meteorito(MeteoritoEntity m, Vec3 cam, float pt, float t, Camera camara) {
		Vec3 p = new Vec3(Mth.lerp((double) pt, m.xo, m.getX()), Mth.lerp((double) pt, m.yo, m.getY()), Mth.lerp((double) pt, m.zo, m.getZ())).add(0.0D, 1.0D, 0.0D).subtract(cam);
		Vec3 v = m.getDeltaMovement();
		if (v.lengthSqr() < 1.0E-6D)
			v = new Vec3(0.0D, -1.0D, 0.0D);
		Vector3f dir = new Vector3f((float) v.x, (float) v.y, (float) v.z).normalize();
		Vector3f h = new Vector3f((float) p.x, (float) p.y, (float) p.z);
		float semilla = (float) (m.getId() % 97) * 1.37F;
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		usar(2.0F, t, semilla, true);
		cinta(h, dir, 49.300003F, 5.44F, 0.55F);
		cinta(h, dir, ESTELA, 2.38F, 1.0F);
		usar(1.0F, t, semilla, true);
		cartel(h, 15.3F, camara, 1.0F, 1.0F, 1.0F, 0.55F);
		cartel(h, 5.44F, camara, 1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static void cinta(Vector3f cabeza, Vector3f dir, float largo, float ancho, float alfa) {
		Vector3f cola = new Vector3f(dir).mul(-largo).add(cabeza);
		Vector3f medio = new Vector3f(cabeza).add(cola).mul(0.5F);
		Vector3f lado = new Vector3f(dir).cross(medio);
		if (lado.lengthSquared() < 1.0E-6F)
			lado.set(1.0F, 0.0F, 0.0F);
		lado.normalize();
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, POSITION_COLOR_TEX);
		int tramos = 12;
		for (int i = 0; i < tramos; i++) {
			float u0 = (float) i / (float) tramos;
			float u1 = (float) (i + 1) / (float) tramos;
			Vector3f a = new Vector3f(dir).mul(-largo * u0).add(cabeza);
			Vector3f c = new Vector3f(dir).mul(-largo * u1).add(cabeza);
			float w0 = ancho * (0.55F + 0.45F * (1.0F - u0));
			float w1 = ancho * (0.55F + 0.45F * (1.0F - u1));
			b.addVertex(a.x - lado.x * w0, a.y - lado.y * w0, a.z - lado.z * w0).setColor(1.0F, 1.0F, 1.0F, alfa).setUv(u0, 0.0F);
			b.addVertex(c.x - lado.x * w1, c.y - lado.y * w1, c.z - lado.z * w1).setColor(1.0F, 1.0F, 1.0F, alfa).setUv(u1, 0.0F);
			b.addVertex(c.x + lado.x * w1, c.y + lado.y * w1, c.z + lado.z * w1).setColor(1.0F, 1.0F, 1.0F, alfa).setUv(u1, 1.0F);
			b.addVertex(a.x + lado.x * w0, a.y + lado.y * w0, a.z + lado.z * w0).setColor(1.0F, 1.0F, 1.0F, alfa).setUv(u0, 1.0F);
		}
		BufferUploader.drawWithShader(b.buildOrThrow());
	}

	static void cartel(Vector3f c, float r, Camera cam, float cr, float cg, float cb, float a) {
		Vector3f iz = new Vector3f(cam.getLeftVector()).mul(r);
		Vector3f ar = new Vector3f(cam.getUpVector()).mul(r);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, POSITION_COLOR_TEX);
		b.addVertex(c.x + iz.x - ar.x, c.y + iz.y - ar.y, c.z + iz.z - ar.z).setColor(cr, cg, cb, a).setUv(0.0F, 0.0F);
		b.addVertex(c.x - iz.x - ar.x, c.y - iz.y - ar.y, c.z - iz.z - ar.z).setColor(cr, cg, cb, a).setUv(1.0F, 0.0F);
		b.addVertex(c.x - iz.x + ar.x, c.y - iz.y + ar.y, c.z - iz.z + ar.z).setColor(cr, cg, cb, a).setUv(1.0F, 1.0F);
		b.addVertex(c.x + iz.x + ar.x, c.y + iz.y + ar.y, c.z + iz.z + ar.z).setColor(cr, cg, cb, a).setUv(0.0F, 1.0F);
		BufferUploader.drawWithShader(b.buildOrThrow());
	}

	private static void esfera(Vector3f c, float r, float giro) {
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, POSITION_COLOR_TEX);
		int lat = 14;
		int lon = 24;
		for (int i = 0; i < lat; i++) {
			for (int j = 0; j < lon; j++) {
				punto(b, c, r, i, j, lat, lon, giro);
				punto(b, c, r, i, j + 1, lat, lon, giro);
				punto(b, c, r, i + 1, j + 1, lat, lon, giro);
				punto(b, c, r, i + 1, j, lat, lon, giro);
			}
		}
		BufferUploader.drawWithShader(b.buildOrThrow());
	}

	private static void punto(BufferBuilder b, Vector3f c, float r, int i, int j, int lat, int lon, float giro) {
		float a = 3.1415927F * (float) i / (float) lat;
		float o = 6.2831855F * (float) j / (float) lon;
		float nx = Mth.sin(a) * Mth.cos(o);
		float ny = Mth.cos(a);
		float nz = Mth.sin(a) * Mth.sin(o);
		float bulto = 1.0F + 0.1F * Mth.sin(nx * 5.1F + ny * 3.3F) * Mth.cos(nz * 4.7F - ny * 2.1F);
		float cg = Mth.cos(giro);
		float sg = Mth.sin(giro);
		float x = (nx * cg - nz * sg) * r * bulto;
		float z = (nx * sg + nz * cg) * r * bulto;
		float y = ny * r * bulto;
		b.addVertex(c.x + x, c.y + y, c.z + z).setColor(nx * 0.5F + 0.5F, ny * 0.5F + 0.5F, nz * 0.5F + 0.5F, 1.0F).setUv(o / 6.2831855F, a / 3.1415927F);
	}

	private static void plano(float r, float g, float bl, float a, boolean sumar) {
		Matrix4f proy = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting orden = RenderSystem.getVertexSorting();
		Matrix4fStack mv = RenderSystem.getModelViewStack();
		mv.pushMatrix();
		mv.identity();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
		RenderSystem.disableDepthTest();
		usar(5.0F, 0.0F, 0.0F, sumar);
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, POSITION_COLOR_TEX);
		b.addVertex(-1.0F, -1.0F, 0.0F).setColor(r, g, bl, a).setUv(0.0F, 0.0F);
		b.addVertex(1.0F, -1.0F, 0.0F).setColor(r, g, bl, a).setUv(1.0F, 0.0F);
		b.addVertex(1.0F, 1.0F, 0.0F).setColor(r, g, bl, a).setUv(1.0F, 1.0F);
		b.addVertex(-1.0F, 1.0F, 0.0F).setColor(r, g, bl, a).setUv(0.0F, 1.0F);
		BufferUploader.drawWithShader(b.buildOrThrow());
		RenderSystem.enableDepthTest();
		mv.popMatrix();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setProjectionMatrix(proy, orden);
	}

	/** Hongo de polvo y fuego de un impacto (partículas de humo ordenadas de lejos a cerca). */
	private static final class Hongo {
		final Vec3 centro;
		final long nace = System.nanoTime();
		final float escala;
		final float[][] columna;
		final float[][] cabeza;
		final float[][] base;
		final float[][] bola;
		final float semilla;

		{
			// en el original este bloque de inicialización iba antes del cuerpo del constructor (semilla por tiempo)
			Random r2 = new Random(System.nanoTime());
			this.bola = new float[70][];
			for (int i = 0; i < this.bola.length; i++) {
				double u = r2.nextDouble() * 2.0D - 1.0D;
				double th = r2.nextDouble() * Math.PI * 2.0D;
				double rr = Math.cbrt(r2.nextDouble());
				double sq = Math.sqrt(1.0D - u * u);
				this.bola[i] = new float[]{(float) (sq * Math.cos(th) * rr), (float) (u * rr), (float) (sq * Math.sin(th) * rr), 0.35F + r2.nextFloat() * 0.3F, r2.nextFloat()};
			}
		}

		Hongo(Vec3 centro, float escala) {
			this.centro = centro;
			this.escala = escala;
			Random r = new Random(Double.doubleToLongBits(centro.x * 31.0D + centro.z * 17.0D));
			this.semilla = r.nextFloat() * 50.0F;
			this.columna = new float[80][];
			for (int i = 0; i < this.columna.length; i++)
				this.columna[i] = new float[]{r.nextFloat(), r.nextFloat() * 6.283F, (float) Math.sqrt((double) r.nextFloat()), 4.0F + r.nextFloat() * 3.0F, 0.8F + r.nextFloat() * 0.25F, r.nextFloat()};
			this.cabeza = new float[170][];
			for (int i = 0; i < this.cabeza.length; i++)
				this.cabeza[i] = new float[]{r.nextFloat() * 6.283F, r.nextFloat() * 6.283F, 0.35F + 0.65F * (float) Math.sqrt((double) r.nextFloat()), 7.0F + r.nextFloat() * 5.0F, 0.88F + r.nextFloat() * 0.22F, r.nextFloat()};
			this.base = new float[90][];
			for (int i = 0; i < this.base.length; i++)
				this.base[i] = new float[]{r.nextFloat() * 6.283F, 0.25F + 0.85F * (float) Math.sqrt((double) r.nextFloat()), r.nextFloat() * 4.0F, 6.0F + r.nextFloat() * 5.0F, 0.78F + r.nextFloat() * 0.25F, r.nextFloat()};
		}

		float edad() {
			return (float) (System.nanoTime() - this.nace) / 1.0E9F;
		}

		void dibujar(Vec3 cam, Camera camara, float t) {
			float e = this.edad();
			float s = this.escala;
			float alfa = Mth.clamp(e / 0.3F, 0.0F, 1.0F) * (1.0F - suave((e - 12.0F) / 14.0F));
			if (alfa <= 0.003F)
				return;

			float fuego = (float) Math.exp((double) (-e) / 6.0D);
			float alto = 70.0F * s * (1.0F - (float) Math.exp((double) (-e) / 3.5D));
			float anillo = 8.0F + 40.0F * s * (1.0F - (float) Math.exp((double) (-e) / 4.0D));
			float tubo = (5.0F + 6.0F * (1.0F - (float) Math.exp((double) (-e) / 3.0D))) * s;
			float suelo = 6.0F + 42.0F * s * (1.0F - (float) Math.exp((double) (-e) / 2.2D));
			float cx = (float) (this.centro.x - cam.x);
			float cy = (float) (this.centro.y - cam.y);
			float cz = (float) (this.centro.z - cam.z);
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();

			float onda = 150.0F * s * (1.0F - (float) Math.exp((double) (-e) / 1.2D));
			float ondaAlfa = (1.0F - suave(e / 3.5F)) * 0.9F;
			if (ondaAlfa > 0.01F) {
				usar(7.0F, t, this.semilla, false);
				anilloSuelo(cx, cy + 0.6F, cz, Math.max(0.0F, onda - 10.0F * s), onda, ondaAlfa);
			}

			float bola = (1.0F - suave(e / 4.0F)) * Mth.clamp(e / 0.12F, 0.0F, 1.0F);
			if (bola > 0.01F) {
				usar(8.0F, t, this.semilla, true);
				float rb = (14.0F + 46.0F * (1.0F - (float) Math.exp((double) (-e) / 0.5D))) * s;
				cartel(new Vector3f(cx, cy + rb * 0.35F, cz), rb, camara, 1.0F, 1.0F, 1.0F, bola);
			}

			ArrayList<float[]> todas = new ArrayList<>(this.base.length + this.columna.length + this.cabeza.length + 20);
			for (float[] q : this.base) {
				float rr = suelo * q[1];
				float x = cx + Mth.cos(q[0] + e * 0.04F) * rr;
				float z = cz + Mth.sin(q[0] + e * 0.04F) * rr;
				float y = cy + 1.0F + q[2] * s * (1.0F - q[1] * 0.5F) + e * 0.12F;
				todas.add(new float[]{x, y, z, q[3] * s * (0.8F + e * 0.05F), q[4] * 0.85F, q[5], fuego * 0.5F, alfa * 0.9F});
			}

			for (float[] q : this.columna) {
				float h = alto * q[0];
				float cintura = 1.0F - 0.45F * (float) Math.sin((double) q[0] * Math.PI);
				float ancho = tubo * 0.9F * cintura * q[2];
				float giro = q[1] + e * (0.25F + 0.2F * q[0]);
				float x = cx + Mth.cos(giro) * ancho;
				float z = cz + Mth.sin(giro) * ancho;
				todas.add(new float[]{x, cy + h, z, q[3] * s * (0.9F + 0.5F * q[0]), q[4] * 0.86F, q[5], fuego * (1.0F - q[0] * 0.4F), alfa});
			}

			for (float[] q : this.cabeza) {
				float phi = q[1] + e * 0.5F;
				float rt = tubo * 1.5F * q[2];
				float rr = anillo + Mth.cos(phi) * rt;
				float x = cx + Mth.cos(q[0]) * rr;
				float z = cz + Mth.sin(q[0]) * rr;
				float y = cy + alto + Mth.sin(phi) * rt * 0.38F;
				float tono = q[4] * (0.82F + 0.18F * (0.5F + 0.5F * Mth.sin(phi)));
				todas.add(new float[]{x, y, z, q[3] * s * (0.8F + 0.5F * (1.0F - (float) Math.exp((double) (-e) / 3.0D))), tono, q[5],
						Math.max(fuego, 0.75F - 0.25F * Mth.sin(phi)) * Mth.clamp(1.0F - (e - 14.0F) / 10.0F, 0.0F, 1.0F), alfa});
			}

			float bf = Mth.clamp(e / 0.15F, 0.0F, 1.0F) * (1.0F - suave((e - 5.0F) / 4.0F));
			if (bf > 0.01F) {
				float rb = (10.0F + 26.0F * (1.0F - (float) Math.exp((double) (-e) / 0.6D))) * s;
				float calorBola = Mth.clamp(1.15F - e / 7.0F, 0.0F, 1.0F);
				for (float[] q : this.bola)
					todas.add(new float[]{cx + q[0] * rb, cy + rb * 0.75F + q[1] * rb * 0.85F + e * 1.5F * s, cz + q[2] * rb, q[3] * rb, 1.0F, q[4], calorBola, alfa * bf});
			}

			for (int i = 0; i < 14; i++) {
				float ang = (float) i * 2.39996F;
				float rr = anillo * 0.75F * (float) Math.sqrt((double) (((float) i + 0.5F) / 14.0F));
				todas.add(new float[]{cx + Mth.cos(ang) * rr, cy + alto + tubo * 0.45F, cz + Mth.sin(ang) * rr, 11.0F * s, 1.0F, (float) i * 0.173F % 1.0F,
						Math.max(fuego, 0.55F) * Mth.clamp(1.0F - (e - 14.0F) / 10.0F, 0.0F, 1.0F), alfa});
			}

			todas.sort((p1, p2) -> Float.compare(p2[0] * p2[0] + p2[1] * p2[1] + p2[2] * p2[2], p1[0] * p1[0] + p1[1] * p1[1] + p1[2] * p1[2]));
			usar(4.0F, t, this.semilla, false);
			BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, POSITION_COLOR_TEX);
			for (float[] p : todas)
				bocanada(b, camara, p[0], p[1], p[2], p[3], Math.min(1.0F, p[4]), p[5], p[6], p[7]);
			MeshData malla = b.build();
			if (malla != null)
				BufferUploader.drawWithShader(malla);
		}

		private static void bocanada(BufferBuilder b, Camera cam, float x, float y, float z, float r, float tono, float semilla, float fuego, float a) {
			Vector3f iz = new Vector3f(cam.getLeftVector()).mul(r);
			Vector3f ar = new Vector3f(cam.getUpVector()).mul(r);
			float g = semilla;
			b.addVertex(x + iz.x - ar.x, y + iz.y - ar.y, z + iz.z - ar.z).setColor(tono, g, fuego, a).setUv(0.0F, 0.0F);
			b.addVertex(x - iz.x - ar.x, y - iz.y - ar.y, z - iz.z - ar.z).setColor(tono, g, fuego, a).setUv(1.0F, 0.0F);
			b.addVertex(x - iz.x + ar.x, y - iz.y + ar.y, z - iz.z + ar.z).setColor(tono, g, fuego, a).setUv(1.0F, 1.0F);
			b.addVertex(x + iz.x + ar.x, y + iz.y + ar.y, z + iz.z + ar.z).setColor(tono, g, fuego, a).setUv(0.0F, 1.0F);
		}

		private static void anilloSuelo(float cx, float cy, float cz, float r0, float r1, float a) {
			BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, POSITION_COLOR_TEX);
			int n = 64;
			for (int i = 0; i < n; i++) {
				float a0 = 6.2831855F * (float) i / (float) n;
				float a1 = 6.2831855F * (float) (i + 1) / (float) n;
				float u0 = (float) i / (float) n;
				float u1 = (float) (i + 1) / (float) n;
				b.addVertex(cx + Mth.cos(a0) * r0, cy, cz + Mth.sin(a0) * r0).setColor(1.0F, 1.0F, 1.0F, a).setUv(u0, 0.0F);
				b.addVertex(cx + Mth.cos(a1) * r0, cy, cz + Mth.sin(a1) * r0).setColor(1.0F, 1.0F, 1.0F, a).setUv(u1, 0.0F);
				b.addVertex(cx + Mth.cos(a1) * r1, cy + 1.5F, cz + Mth.sin(a1) * r1).setColor(1.0F, 1.0F, 1.0F, a).setUv(u1, 1.0F);
				b.addVertex(cx + Mth.cos(a0) * r1, cy + 1.5F, cz + Mth.sin(a0) * r1).setColor(1.0F, 1.0F, 1.0F, a).setUv(u0, 1.0F);
			}
			BufferUploader.drawWithShader(b.buildOrThrow());
		}
	}
}
