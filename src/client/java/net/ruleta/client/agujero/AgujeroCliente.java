package net.ruleta.client.agujero;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.ruleta.agujero.AgujeroPayload;
import net.ruleta.client.FiltroPantalla;
import net.ruleta.client.bunker.BunkerMarcador;
import net.ruleta.compat.EventPriority;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.ClientPlayerNetworkEvent;
import net.ruleta.compat.client.RenderLevelStageEvent;
import net.ruleta.compat.client.ViewportEvent;

/** Agujero Negro (cliente): cielo estrellado, filtro de pantalla y los agujeros con lente gravitatoria. */
public final class AgujeroCliente {
	private static final ResourceLocation TINTE = ResourceLocation.fromNamespaceAndPath("ruleta", "shaders/post/agujero_tinte.json");
	private static final FiltroPantalla FILTRO = new FiltroPantalla(TINTE);
	private static final float FUNDIDO = 2.0F;
	private static ShaderInstance shader;
	private static ShaderInstance shaderObjeto;
	private static boolean activo;
	private static final List<AgujeroPayload.Agujero> AGUJEROS = new ArrayList<>();
	private static float nivel;
	private static long ultimo = -1L;
	private static final double SOMBRA = 2.598D;
	private static final double MEDIO_CUADRADO = 9.0D;
	private static TextureTarget copia;
	private static boolean profundidadGuardada;

	private AgujeroCliente() {
	}

	public static void shaderObjeto(ShaderInstance s) {
		shaderObjeto = s;
	}

	public static void shader(ShaderInstance s) {
		shader = s;
	}

	public static void recibir(AgujeroPayload p) {
		activo = p.activo();
		AGUJEROS.clear();
		AGUJEROS.addAll(p.agujeros());
	}

	public static float nivel() {
		return nivel;
	}

	public static boolean tapaCielo() {
		Minecraft mc = Minecraft.getInstance();
		return nivel >= 0.999F && mc.level != null && mc.level.dimension() == Level.OVERWORLD;
	}

	private static void avanzar(boolean listo) {
		long ahora = System.nanoTime();
		float dt = ultimo < 0L ? 0.0F : Math.min(0.25F, (float) (ahora - ultimo) / 1.0E9F);
		ultimo = ahora;
		float paso = activo ? (listo ? dt : 0.0F) : -dt;
		nivel = Mth.clamp(nivel + paso / FUNDIDO, 0.0F, 1.0F);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void alDibujarMundo(RenderLevelStageEvent e) {
		if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
			return;
		Minecraft mc = Minecraft.getInstance();
		boolean overworld = mc.level != null && mc.level.dimension() == Level.OVERWORLD;
		boolean listo = true;
		if (activo && overworld)
			listo = CieloBake.avanzar() && !CieloBake.fallo();
		avanzar(listo);

		if (nivel <= 0.0F || !overworld) {
			if (nivel <= 0.0F) {
				FILTRO.cerrar();
				if (!activo)
					CieloBake.liberar();
			}
			dibujarAgujeros(e, mc);
			BunkerMarcador.alDibujarMundo(e);
			return;
		}

		FogType dentro = e.getCamera().getFluidInCamera();
		if (shader != null && CieloBake.listo() && dentro != FogType.LAVA && dentro != FogType.POWDER_SNOW && dentro != FogType.WATER)
			cielo(e, mc);

		asegurarCopia(mc);
		copia.copyDepthFrom(mc.getMainRenderTarget());
		mc.getMainRenderTarget().bindWrite(true);
		if (FILTRO.preparar()) {
			FILTRO.fijar("Fuerza", nivel);
			float[] nb = colorNeblina();
			Matrix4f pr = e.getProjectionMatrix();
			float cerca = pr.m32() / (pr.m22() - 1.0F);
			float lejos = pr.m32() / (pr.m22() + 1.0F);
			FILTRO.fijar("Neblina", nb[0], nb[1], nb[2], 1.0F);
			FILTRO.fijar("Rango", cerca, lejos, (float) mc.options.getEffectiveRenderDistance() * 16.0F, 0.0F);
			FILTRO.pasar(e.getPartialTick());
			RenderSystem.enableDepthTest();
		}

		mc.getMainRenderTarget().copyDepthFrom(copia);
		mc.getMainRenderTarget().bindWrite(true);
		profundidadGuardada = true;
		dibujarAgujeros(e, mc);
		profundidadGuardada = false;
		BunkerMarcador.alDibujarMundo(e);
	}

	private static void dibujarAgujeros(RenderLevelStageEvent e, Minecraft mc) {
		if (shaderObjeto == null || mc.level == null)
			return;

		Camera camara = e.getCamera();
		Vec3 ojo = camara.getPosition();
		Vector3f mira = camara.getLookVector();
		float parcial = e.getPartialTick();
		String dimension = mc.level.dimension().location().toString();
		List<AgujeroPayload.Agujero> visibles = null;
		for (AgujeroPayload.Agujero agujero : AGUJEROS) {
			if (!agujero.dimension().equals(dimension))
				continue;
			Vec3 a = agujero.pos().subtract(ojo);
			double dist = a.length();
			double medio = (double) agujero.radio() / SOMBRA * MEDIO_CUADRADO;
			if (dist > medio) {
				double coseno = (a.x * (double) mira.x() + a.y * (double) mira.y() + a.z * (double) mira.z()) / dist;
				double rho = Math.atan(medio / dist);
				if (coseno < Math.cos(Math.min(Math.PI, rho + 1.4D)))
					continue;
			}
			if (visibles == null)
				visibles = new ArrayList<>();
			visibles.add(agujero);
		}
		if (visibles == null)
			return;

		Matrix4f vista = new Matrix4f().rotateX(camara.getXRot() * 0.017453292F).rotateY((camara.getYRot() + 180.0F) * 0.017453292F);
		Matrix4f vistaMundo = new Matrix4f(vista).transpose();
		float lejos = (float) mc.options.getEffectiveRenderDistance() * 16.0F * 4.0F;
		float maxD = lejos * 0.4F;
		boolean cielo = CieloBake.listo();
		ShaderInstance sh = shaderObjeto;
		sh.safeGetUniform("Tiempo").set((float) ((double) System.nanoTime() / 1.0E9D % 3600.0D));
		sh.safeGetUniform("MezclaCielo").set(cielo ? nivel : 0.0F);
		sh.safeGetUniform("Gris").set(0.0F);
		sh.safeGetUniform("VistaMundo").set(vistaMundo);
		sh.setSampler("Escena", Integer.valueOf(copiarEscena(mc)));
		sh.setSampler("EscenaProf", Integer.valueOf(copia.getDepthTextureId()));
		if (cielo) {
			sh.setSampler("Nebulosa", CieloBake.nebulosa());
			sh.setSampler("Constelaciones", CieloBake.constelaciones());
		}

		Vector3f arriba = camara.getUpVector();
		Vector3f izq = camara.getLeftVector();
		Matrix4f proyeccionAntes = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting ordenAntes = RenderSystem.getVertexSorting();
		// 1.21.1: la pila de modelo-vista es un Matrix4fStack de JOML (antes un PoseStack)
		Matrix4fStack mv = RenderSystem.getModelViewStack();
		mc.getMainRenderTarget().bindWrite(true);
		RenderSystem.setProjectionMatrix(new Matrix4f(e.getProjectionMatrix()), VertexSorting.DISTANCE_TO_ORIGIN);
		mv.pushMatrix();
		mv.identity();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setShader(() -> sh);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		try {
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(515);
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();

			for (AgujeroPayload.Agujero agujero : visibles) {
				Vec3 a = agujero.pos().subtract(ojo);
				double rs = (double) agujero.radio() / SOMBRA;
				double medio = rs * MEDIO_CUADRADO;
				double dist = a.length();
				if (dist < 0.001D) {
					a = new Vec3(0.0D, 0.0D, 0.001D);
					dist = 0.001D;
				}

				double nx = a.x / dist;
				double ny = a.y / dist;
				double nz = a.z / dist;
				double dx = (double) arriba.y() * nz - (double) arriba.z() * ny;
				double dy = (double) arriba.z() * nx - (double) arriba.x() * nz;
				double dz = (double) arriba.x() * ny - (double) arriba.y() * nx;
				double ld = Math.sqrt(dx * dx + dy * dy + dz * dz);
				if (ld < 1.0E-4D) {
					dx = (double) (-izq.x());
					dy = (double) (-izq.y());
					dz = (double) (-izq.z());
					ld = Math.sqrt(dx * dx + dy * dy + dz * dz);
				}
				dx /= ld;
				dy /= ld;
				dz /= ld;
				double ux = ny * dz - nz * dy;
				double uy = nz * dx - nx * dz;
				double uz = nx * dy - ny * dx;

				sh.safeGetUniform("CamRel").set((float) (-a.x / rs), (float) (-a.y / rs), (float) (-a.z / rs));
				sh.safeGetUniform("QDer").set((float) dx, (float) dy, (float) dz);
				sh.safeGetUniform("QArr").set((float) ux, (float) uy, (float) uz);
				sh.safeGetUniform("Escala").set((float) rs);

				BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
				if (dist > medio * 1.05D) {
					float k = dist > (double) maxD ? (float) ((double) maxD / dist) : 1.0F;
					double[][] esquinas = {{-1.0D, -1.0D}, {1.0D, -1.0D}, {1.0D, 1.0D}, {-1.0D, 1.0D}};
					for (double[] q : esquinas) {
						double x = a.x + (dx * q[0] + ux * q[1]) * medio;
						double y = a.y + (dy * q[0] + uy * q[1]) * medio;
						double z = a.z + (dz * q[0] + uz * q[1]) * medio;
						Vector3f v = vista.transformPosition(new Vector3f((float) x * k, (float) y * k, (float) z * k));
						b.addVertex(v.x(), v.y(), v.z()).setUv((float) (q[0] * 0.5D + 0.5D), (float) (q[1] * 0.5D + 0.5D));
					}
				} else {
					esferaAgujero(b, vista, (float) Math.max(0.2D, dist * 0.95D));
				}
				MeshData malla = b.build();
				if (malla != null)
					BufferUploader.drawWithShader(malla);
			}
		} finally {
			RenderSystem.enableCull();
			RenderSystem.depthMask(true);
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			mv.popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(proyeccionAntes, ordenAntes);
		}
	}

	private static void asegurarCopia(Minecraft mc) {
		RenderTarget principal = mc.getMainRenderTarget();
		if (copia == null) {
			copia = new TextureTarget(principal.width, principal.height, true, Minecraft.ON_OSX);
			copia.setFilterMode(9729);
		} else if (copia.width != principal.width || copia.height != principal.height) {
			copia.resize(principal.width, principal.height, Minecraft.ON_OSX);
			copia.setFilterMode(9729);
		}
	}

	private static int copiarEscena(Minecraft mc) {
		RenderTarget principal = mc.getMainRenderTarget();
		asegurarCopia(mc);
		GlStateManager._glBindFramebuffer(36008, principal.frameBufferId);
		GlStateManager._glBindFramebuffer(36009, copia.frameBufferId);
		GlStateManager._glBlitFrameBuffer(0, 0, principal.width, principal.height, 0, 0, copia.width, copia.height, 16384, 9728);
		if (!profundidadGuardada)
			copia.copyDepthFrom(principal);
		principal.bindWrite(true);
		return copia.getColorTextureId();
	}

	private static void esferaAgujero(BufferBuilder b, Matrix4f vista, float r) {
		int lat = 24;
		int lon = 48;
		for (int i = 0; i < lat; i++) {
			float a0 = 3.1415927F * (float) i / 24.0F;
			float a1 = 3.1415927F * (float) (i + 1) / 24.0F;
			for (int j = 0; j < lon; j++) {
				float b0 = 6.2831855F * (float) j / 48.0F;
				float b1 = 6.2831855F * (float) (j + 1) / 48.0F;
				puntoEsfera(b, vista, r, a0, b0);
				puntoEsfera(b, vista, r, a1, b0);
				puntoEsfera(b, vista, r, a1, b1);
				puntoEsfera(b, vista, r, a0, b1);
			}
		}
	}

	private static void puntoEsfera(BufferBuilder b, Matrix4f vista, float r, float a, float c) {
		float x = r * Mth.sin(a) * Mth.cos(c);
		float y = r * Mth.cos(a);
		float z = r * Mth.sin(a) * Mth.sin(c);
		Vector3f v = vista.transformPosition(new Vector3f(x, y, z));
		b.addVertex(v.x(), v.y(), v.z()).setUv(0.0F, 0.0F);
	}

	private static void cuboAgujero(BufferBuilder b, Matrix4f vista, float s) {
		for (float[] c : caras(s)) {
			for (int k = 0; k < 12; k += 3) {
				Vector3f v = vista.transformPosition(new Vector3f(c[k], c[k + 1], c[k + 2]));
				b.addVertex(v.x(), v.y(), v.z()).setUv(0.0F, 0.0F);
			}
		}
	}

	private static float[][] caras(float s) {
		return new float[][]{
				{-s, s, -s, s, s, -s, s, s, s, -s, s, s},
				{-s, -s, -s, -s, -s, s, s, -s, s, s, -s, -s},
				{-s, -s, -s, s, -s, -s, s, s, -s, -s, s, -s},
				{s, -s, s, -s, -s, s, -s, s, s, s, s, s},
				{-s, -s, s, -s, -s, -s, -s, s, -s, -s, s, s},
				{s, -s, -s, s, -s, s, s, s, s, s, s, -s}
		};
	}

	private static void cielo(RenderLevelStageEvent e, Minecraft mc) {
		Camera camara = e.getCamera();
		Vec3 ojo = camara.getPosition();
		ShaderInstance sh = shader;
		sh.safeGetUniform("Tiempo").set((float) ((double) System.nanoTime() / 1.0E9D % 3600.0D));
		sh.safeGetUniform("Alfa").set(nivel);
		sh.setSampler("Nebulosa", CieloBake.nebulosa());
		sh.setSampler("Constelaciones", CieloBake.constelaciones());
		Matrix4f vista = new Matrix4f().rotateX(camara.getXRot() * 0.017453292F).rotateY((camara.getYRot() + 180.0F) * 0.017453292F);
		float lejos = (float) mc.options.getEffectiveRenderDistance() * 16.0F * 4.0F;
		Matrix4f proyeccionAntes = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting ordenAntes = RenderSystem.getVertexSorting();
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

	private static float faseRoja() {
		float tiempo = (float) ((double) System.nanoTime() / 1.0E9D % 3600.0D);
		float s = 0.5F + 0.5F * Mth.sin(tiempo * 6.2831855F / 90.0F - 1.2F);
		float x = Mth.clamp((s - 0.55F) / 0.3F, 0.0F, 1.0F);
		return x * x * (3.0F - 2.0F * x);
	}

	private static float[] colorNeblina() {
		float r = faseRoja();
		return new float[]{0.2F, 0.21F, 0.25F};
	}

	@SubscribeEvent
	public static void alColorearNiebla(ViewportEvent.ComputeFogColor e) {
		Minecraft mc = Minecraft.getInstance();
		if (nivel <= 0.0F || mc.level == null || mc.level.dimension() != Level.OVERWORLD)
			return;
		float[] c = colorNeblina();
		e.setRed(Mth.lerp(nivel, e.getRed(), c[0] * 0.55F));
		e.setGreen(Mth.lerp(nivel, e.getGreen(), c[1] * 0.55F));
		e.setBlue(Mth.lerp(nivel, e.getBlue(), c[2] * 0.55F));
	}

	@SubscribeEvent
	public static void alSalir(ClientPlayerNetworkEvent.LoggingOut e) {
		activo = false;
		AGUJEROS.clear();
		nivel = 0.0F;
		ultimo = -1L;
		FILTRO.reiniciar();
		CieloBake.liberar();
		if (copia != null) {
			copia.destroyBuffers();
			copia = null;
		}
	}

	private static void cubo(float s) {
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		for (float[] c : caras(s)) {
			for (int k = 0; k < 12; k += 3)
				b.addVertex(c[k], c[k + 1], c[k + 2]);
		}
		BufferUploader.drawWithShader(b.buildOrThrow());
	}
}
