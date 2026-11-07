package com.cloudruleta_supernova.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import com.cloudruleta_supernova.SupernovaState;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.client.Formatos;
import net.cloudruleta.compat.client.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

/**
 * Dibujo de la cloudruleta_supernova (esfera, halo, chorros, destello, cúpula de cielo y tintes de pantalla).
 * Portado de Forge 1.20.1: misma lógica y mismos uniforms. Cambios de 1.21.1:
 * la pila de modelo-vista es un Matrix4fStack de JOML; los vértices se escriben con
 * Tesselator.begin(...) + addVertex/setColor/setUv (sin endVertex); y el formato POSITION_COLOR_TEX, que
 * vanilla quitó, lo da {@link Formatos#POSITION_COLOR_TEX} con el mismo orden de elementos.
 */
public class SupernovaRenderHandler {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static long lastErrLog;
	private static long lastNullLog;
	public static boolean dbgFlip;
	public static boolean dbgNoCull;
	public static boolean dbgNoDepth;
	public static boolean dbgMagenta;
	public static boolean dbgNoChain;
	private static final int ALPHA = 0;
	private static final int ADD = 1;
	private static final int MUL = 2;
	private static final float JET_GIRO = 0.32F;
	private static final float JET_INCLINA = 42.0F;
	private static final float JET_VAIVEN = 12.0F;

	@SubscribeEvent
	public static void onRender(RenderLevelStageEvent e) {
		if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		if (ClientSetup.getSphere() == null || ClientSetup.getAdditive() == null || ClientSetup.getMultiply() == null) {
			if (System.nanoTime() - lastNullLog > 5000000000L) {
				lastNullLog = System.nanoTime();
				LOGGER.warn("Supernova: los shaders no se cargaron, no se dibuja nada (revisa el log al iniciar)");
			}
			return;
		}
		float pt = e.getPartialTick();
		float now = (float) mc.level.getGameTime() + pt;
		List<SupernovaState> list = SupernovaGhosts.active(now);
		if (list.isEmpty()) {
			return;
		}
		ScreenEffectHandler.compute(pt);

		Camera camera = e.getCamera();
		Vec3 cam = camera.getPosition();
		Matrix4f view = new Matrix4f().rotateX(camera.getXRot() * 0.017453292F).rotateY((camera.getYRot() + 180.0F) * 0.017453292F);
		Matrix4f proj = new Matrix4f(e.getProjectionMatrix());
		float far = (float) mc.options.getEffectiveRenderDistance() * 16.0F * 4.0F;

		Matrix4f prevProj = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting prevSort = RenderSystem.getVertexSorting();
		// 1.21.1: la pila de modelo-vista es un Matrix4fStack de JOML (antes un PoseStack)
		Matrix4fStack mv = RenderSystem.getModelViewStack();

		mc.getMainRenderTarget().bindWrite(true);
		RenderSystem.setProjectionMatrix(proj, VertexSorting.DISTANCE_TO_ORIGIN);
		mv.pushMatrix();
		mv.identity();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.depthFunc(515);
		RenderSystem.depthMask(false);
		try {
			float skyTime = ((float) (mc.level.getGameTime() % 72000L) + pt) / 20.0F;
			if (!dbgNoChain) {
				worldTint(proj);
				skyDome(view, far, skyTime);
			}
			for (SupernovaState n : list) {
				drawSupernova(n, cam, view, far, now);
			}
			if (!dbgNoChain) {
				overlays(proj);
			}
		} catch (RuntimeException ex) {
			if (System.nanoTime() - lastErrLog > 2000000000L) {
				lastErrLog = System.nanoTime();
				LOGGER.error("Supernova: error al dibujar", ex);
			}
		} finally {
			int glErr = GL11.glGetError();
			if (glErr != 0 && System.nanoTime() - lastErrLog > 2000000000L) {
				lastErrLog = System.nanoTime();
				LOGGER.warn("Supernova: error de OpenGL 0x{} tras dibujar", Integer.toHexString(glErr));
			}
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
			mv.popMatrix();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setProjectionMatrix(prevProj, prevSort);
		}
	}

	private static ShaderInstance use(int blend) {
		RenderSystem.enableBlend();
		Supplier<ShaderInstance> sup;
		switch (blend) {
			case ADD:
				sup = ClientSetup::getAdditive;
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
				break;
			case MUL:
				sup = ClientSetup::getMultiply;
				RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);
				break;
			default:
				sup = ClientSetup::getSphere;
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		}
		RenderSystem.setShader(sup);
		ShaderInstance sh = sup.get();
		sh.safeGetUniform("Debug").set(dbgMagenta ? 1.0F : 0.0F);
		sh.safeGetUniform("Fade").set(1.0F);
		sh.safeGetUniform("Intensity").set(1.0F);
		sh.safeGetUniform("Charge").set(0.0F);
		sh.safeGetUniform("Expl").set(0.0F);
		sh.safeGetUniform("SkyMix").set(0.0F);
		sh.safeGetUniform("SkyBright").set(0.0F);
		sh.safeGetUniform("SkyDark").set(0.0F);
		sh.safeGetUniform("SphereCenter").set(0.0F, 0.0F, 0.0F);
		sh.safeGetUniform("ViewDirW").set(0.0F, 0.0F, 1.0F);
		sh.safeGetUniform("Radius").set(1.0F);
		return sh;
	}

	private static void depth(boolean test) {
		if (test && !dbgNoDepth) {
			RenderSystem.enableDepthTest();
		} else {
			RenderSystem.disableDepthTest();
		}
	}

	private static void worldTint(Matrix4f proj) {
		boolean mul = ScreenEffectHandler.mulR < 0.999F || ScreenEffectHandler.mulG < 0.999F || ScreenEffectHandler.mulB < 0.999F;
		boolean add = ScreenEffectHandler.addR + ScreenEffectHandler.addG + ScreenEffectHandler.addB > 0.003F;
		if (!mul && !add) {
			return;
		}
		if (mul) {
			ShaderInstance sh = use(MUL);
			sh.safeGetUniform("Mode").set(6.0F);
			fullscreen(proj, ScreenEffectHandler.mulR, ScreenEffectHandler.mulG, ScreenEffectHandler.mulB, 1.0F);
		}
		if (add) {
			ShaderInstance sh = use(ADD);
			sh.safeGetUniform("Mode").set(6.0F);
			fullscreen(proj, ScreenEffectHandler.addR, ScreenEffectHandler.addG, ScreenEffectHandler.addB, 1.0F);
		}
	}

	private static void skyDome(Matrix4f view, float far, float time) {
		if (ScreenEffectHandler.skyMix <= 0.002F) {
			return;
		}
		ShaderInstance sh = use(ALPHA);
		sh.safeGetUniform("Mode").set(3.0F);
		sh.safeGetUniform("Time").set(time);
		sh.safeGetUniform("SkyMix").set(ScreenEffectHandler.skyMix);
		sh.safeGetUniform("SkyBright").set(ScreenEffectHandler.skyBright);
		sh.safeGetUniform("SkyDark").set(ScreenEffectHandler.skyDark);
		sh.safeGetUniform("Aparece").set(ScreenEffectHandler.aparece);
		sh.safeGetUniform("StarDirW").set(ScreenEffectHandler.starX, ScreenEffectHandler.starY, ScreenEffectHandler.starZ);
		RenderSystem.disableCull();
		depth(true);
		drawSphere(view, far * 0.7F, true);
	}

	private static void drawSupernova(SupernovaState n, Vec3 cam, Matrix4f view, float far, float now) {
		float fade = n.fade(now);
		if (fade <= 0.002F) {
			return;
		}
		float time = n.age(now) / 20.0F;

		Vec3 c = n.center();
		Vector3f cv = view.transformPosition(new Vector3f((float) (c.x - cam.x), (float) (c.y - cam.y), (float) (c.z - cam.z)));
		float dist = cv.length();
		if (dist < 0.001F) {
			return;
		}

		float maxD = far * 0.4F;
		float scale = dist > maxD ? maxD / dist : 1.0F;
		Vector3f cs = new Vector3f(cv).mul(scale);
		float dS = dist * scale;

		Vec3 toCam = cam.subtract(c);
		Vec3 vd = toCam.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : toCam.normalize();
		Matrix4f sphereM = new Matrix4f().translation(cs).mul(view);

		if (!n.isBlasting(now)) {
			float charge = n.charge(now);
			float R = n.starRadius(now) * n.breath(now) * scale;

			if (dS > R * 1.02F) {
				ShaderInstance sh = use(ALPHA);
				setSphere(sh, 0.0F, time, charge, 0.0F, fade, cs, vd);
				if (dbgNoCull) {
					RenderSystem.disableCull();
				} else {
					RenderSystem.enableCull();
				}
				depth(true);
				drawSphere(sphereM, R, dbgFlip);
			}

			float shellR = R * 1.5F;
			if (dS > shellR * 1.02F) {
				ShaderInstance gs = use(ADD);
				setSphere(gs, 9.0F, time, charge, 0.0F, fade, cs, vd);
				gs.safeGetUniform("Radius").set(R);
				gs.safeGetUniform("SkyBright").set(n.activation(now));
				gs.safeGetUniform("Intensity").set(1.0F);
				RenderSystem.enableCull();
				depth(true);
				drawSphere(sphereM, shellR, false);
			}

			float k = SupernovaState.smooth(charge);
			float size = Mth.lerp(k, R * 3.2F, dS * 0.55F);
			ShaderInstance sh = use(ADD);
			setSphere(sh, 4.0F, time, charge, 0.0F, fade, cs, vd);
			sh.safeGetUniform("Intensity").set(0.85F + 0.9F * charge);
			RenderSystem.disableCull();
			depth(true);
			billboard(cs, size);
			return;
		}

		float bt = n.blastTicks(now);
		float expl = SupernovaState.sat(bt / 30.0F);
		float rReal = n.radius(now);
		float R = rReal * scale;

		if (dS > R * 1.02F) {
			ShaderInstance sh = use(ALPHA);
			setSphere(sh, 2.0F, time, 1.0F, expl, fade, cs, vd);
			if (dbgNoCull) {
				RenderSystem.disableCull();
			} else {
				RenderSystem.enableCull();
			}
			depth(true);
			drawSphere(sphereM, R, dbgFlip);

			float shellR = R * 1.35F;
			ShaderInstance sh2 = use(ADD);
			setSphere(sh2, 7.0F, time, 1.0F, expl, fade, cs, vd);
			sh2.safeGetUniform("Radius").set(R);
			sh2.safeGetUniform("Intensity").set(SupernovaState.smooth(bt / 20.0F));
			RenderSystem.enableCull();
			depth(true);
			drawSphere(sphereM, shellR, dS < shellR);
		}

		float jetI = SupernovaState.smooth(bt / 40.0F) * 1.5F;
		if (jetI > 0.002F) {
			float secs = bt / 20.0F;
			float ang = (float) n.id * 2.39996F + secs * JET_GIRO;
			float tilt = (JET_INCLINA + JET_VAIVEN * Mth.sin(secs * 0.45F)) * 0.017453292F;
			Vector3f axisW = new Vector3f(Mth.sin(tilt) * Mth.cos(ang), Mth.cos(tilt), Mth.sin(tilt) * Mth.sin(ang));
			Vector3f axis = view.transformDirection(axisW).normalize();
			float len = Math.min(Math.max(R * 2.6F, 30.0F * scale), far * 0.45F);
			float width = Math.max(R * 0.55F, 4.0F * scale);
			ShaderInstance sh = use(ADD);
			setSphere(sh, 5.0F, time, 1.0F, expl, fade, cs, vd);
			sh.safeGetUniform("Intensity").set(jetI);
			RenderSystem.disableCull();
			depth(true);
			beam(cs, axis, len, width);

			groundLight(n, axisW, len / scale, width / scale, cam, view, far, jetI * fade);
		}

		float big = 1.0F - SupernovaState.smooth(bt / 70.0F);
		float size = Math.max(Mth.lerp(big, R * 0.45F, dS * 0.75F), 1.0F * scale);
		ShaderInstance sh = use(ADD);
		setSphere(sh, 4.0F, time, 1.0F, 1.0F, fade, cs, vd);
		sh.safeGetUniform("Intensity").set(0.55F + 1.2F * big);
		RenderSystem.disableCull();
		depth(true);
		billboard(cs, size);
	}

	private static void groundLight(SupernovaState n, Vector3f axisW, float lenW, float widthW, Vec3 cam, Matrix4f view, float far, float strength) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || strength <= 0.01F) {
			return;
		}
		Vec3 c = n.center();
		Vec3 to = c.subtract((double) (axisW.x() * lenW), (double) (axisW.y() * lenW), (double) (axisW.z() * lenW));
		BlockHitResult hit = mc.level.clip(new ClipContext(c, to, ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, mc.player));
		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		Vec3 p = hit.getLocation();
		if (p.distanceTo(cam) > (double) (far * 0.9F)) {
			return;
		}

		float r = Math.max(6.0F, widthW * 2.2F);
		float caida = Math.max(0.25F, Math.abs(axisW.y()));
		Vector3f horiz = new Vector3f(axisW.x(), 0.0F, axisW.z());
		if (horiz.lengthSquared() < 1.0E-5F) {
			horiz.set(1.0F, 0.0F, 0.0F);
		}
		horiz.normalize();
		Vector3f u = new Vector3f(horiz).mul(r / caida);
		Vector3f w = new Vector3f(-horiz.z(), 0.0F, horiz.x()).mul(r);
		float y = (float) (p.y - cam.y) + 0.06F;
		float px = (float) (p.x - cam.x);
		float pz = (float) (p.z - cam.z);

		ShaderInstance sh = use(ADD);
		sh.safeGetUniform("Mode").set(8.0F);
		sh.safeGetUniform("Intensity").set(Math.min(1.0F, strength));
		RenderSystem.disableCull();
		depth(true);
		BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, Formatos.POSITION_COLOR_TEX);
		float cr = 1.0F;
		float cg = 0.86F;
		float cb = 0.95F;
		float ca = 0.9F;
		groundVert(bb, view, px - u.x() - w.x(), y, pz - u.z() - w.z(), cr, cg, cb, ca, 0.0F, 0.0F);
		groundVert(bb, view, px + u.x() - w.x(), y, pz + u.z() - w.z(), cr, cg, cb, ca, 1.0F, 0.0F);
		groundVert(bb, view, px + u.x() + w.x(), y, pz + u.z() + w.z(), cr, cg, cb, ca, 1.0F, 1.0F);
		groundVert(bb, view, px - u.x() + w.x(), y, pz - u.z() + w.z(), cr, cg, cb, ca, 0.0F, 1.0F);
		BufferUploader.drawWithShader(bb.buildOrThrow());
	}

	private static void groundVert(BufferBuilder bb, Matrix4f view, float x, float y, float z, float r, float g, float b, float a, float u, float v) {
		Vector3f q = view.transformPosition(new Vector3f(x, y, z));
		bb.addVertex(q.x(), q.y(), q.z()).setColor(r, g, b, a).setUv(u, v);
	}

	private static void setSphere(ShaderInstance sh, float mode, float time, float charge, float expl, float fade, Vector3f center, Vec3 viewDirW) {
		sh.safeGetUniform("Mode").set(mode);
		sh.safeGetUniform("Time").set(time);
		sh.safeGetUniform("Charge").set(charge);
		sh.safeGetUniform("Expl").set(expl);
		sh.safeGetUniform("Fade").set(fade);
		sh.safeGetUniform("SphereCenter").set(center.x(), center.y(), center.z());
		sh.safeGetUniform("ViewDirW").set((float) viewDirW.x, (float) viewDirW.y, (float) viewDirW.z);
	}

	private static void overlays(Matrix4f proj) {
		if (ScreenEffectHandler.flash > 0.003F) {
			ShaderInstance sh = use(ADD);
			sh.safeGetUniform("Mode").set(6.0F);
			float f = ScreenEffectHandler.flash;
			fullscreen(proj, 1.0F, 0.98F, 1.0F, Math.min(1.0F, f * 1.15F));
		}
		if (ScreenEffectHandler.inside > 0.003F) {
			ShaderInstance sh = use(ALPHA);
			sh.safeGetUniform("Mode").set(6.0F);
			fullscreen(proj, 1.0F, 0.7F, 0.85F, 0.62F * ScreenEffectHandler.inside);
			ShaderInstance sh2 = use(ADD);
			sh2.safeGetUniform("Mode").set(6.0F);
			fullscreen(proj, 0.45F, 0.95F, 0.9F, 0.18F * ScreenEffectHandler.inside);
		}
	}

	private static void fullscreen(Matrix4f proj, float r, float g, float b, float a) {
		r = Mth.clamp(r, 0.0F, 1.0F);
		g = Mth.clamp(g, 0.0F, 1.0F);
		b = Mth.clamp(b, 0.0F, 1.0F);
		a = Mth.clamp(a, 0.0F, 1.0F);
		RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();
		BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, Formatos.POSITION_COLOR_TEX);
		bb.addVertex(-1.0F, -1.0F, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F);
		bb.addVertex(1.0F, -1.0F, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F);
		bb.addVertex(1.0F, 1.0F, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F);
		bb.addVertex(-1.0F, 1.0F, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F);
		BufferUploader.drawWithShader(bb.buildOrThrow());
		RenderSystem.setProjectionMatrix(proj, VertexSorting.DISTANCE_TO_ORIGIN);
	}

	private static void billboard(Vector3f c, float h) {
		BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, Formatos.POSITION_COLOR_TEX);
		bb.addVertex(c.x() - h, c.y() - h, c.z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.0F, 0.0F);
		bb.addVertex(c.x() + h, c.y() - h, c.z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(1.0F, 0.0F);
		bb.addVertex(c.x() + h, c.y() + h, c.z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(1.0F, 1.0F);
		bb.addVertex(c.x() - h, c.y() + h, c.z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.0F, 1.0F);
		BufferUploader.drawWithShader(bb.buildOrThrow());
	}

	private static void beam(Vector3f c, Vector3f axis, float len, float width) {
		Vector3f side = new Vector3f(axis).cross(c);
		if (side.lengthSquared() < 1.0E-6F) {
			side = new Vector3f(axis).cross(0.0F, 1.0F, 0.0F);
		}
		if (side.lengthSquared() < 1.0E-6F) {
			side = new Vector3f(1.0F, 0.0F, 0.0F);
		}
		side.normalize().mul(width);
		Vector3f a = new Vector3f(axis).mul(len);
		Vector3f p0 = new Vector3f(c).sub(a);
		Vector3f p1 = new Vector3f(c).add(a);
		BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, Formatos.POSITION_COLOR_TEX);
		bb.addVertex(p0.x() - side.x(), p0.y() - side.y(), p0.z() - side.z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.0F, 0.0F);
		bb.addVertex(p1.x() - side.x(), p1.y() - side.y(), p1.z() - side.z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(1.0F, 0.0F);
		bb.addVertex(p1.x() + side.x(), p1.y() + side.y(), p1.z() + side.z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(1.0F, 1.0F);
		bb.addVertex(p0.x() + side.x(), p0.y() + side.y(), p0.z() + side.z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.0F, 1.0F);
		BufferUploader.drawWithShader(bb.buildOrThrow());
	}

	private static void drawSphere(Matrix4f m, float r, boolean inside) {
		BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, Formatos.POSITION_COLOR_TEX);
		float PI = 3.1415927F;
		int lat = 40;
		int lon = 80;
		for (int i = 0; i < lat; i++) {
			float a0 = PI * (float) i / 40.0F;
			float a1 = PI * (float) (i + 1) / 40.0F;
			for (int j = 0; j < lon; j++) {
				float b0 = 6.2831855F * (float) j / 80.0F;
				float b1 = 6.2831855F * (float) (j + 1) / 80.0F;
				if (inside) {
					vert(bb, m, r, a0, b0);
					vert(bb, m, r, a1, b0);
					vert(bb, m, r, a1, b1);
					vert(bb, m, r, a0, b1);
				} else {
					vert(bb, m, r, a0, b0);
					vert(bb, m, r, a0, b1);
					vert(bb, m, r, a1, b1);
					vert(bb, m, r, a1, b0);
				}
			}
		}
		BufferUploader.drawWithShader(bb.buildOrThrow());
	}

	private static void vert(BufferBuilder bb, Matrix4f m, float r, float a, float b) {
		float nx = Mth.sin(a) * Mth.cos(b);
		float ny = Mth.cos(a);
		float nz = Mth.sin(a) * Mth.sin(b);
		bb.addVertex(m, nx * r, ny * r, nz * r).setColor(nx * 0.5F + 0.5F, ny * 0.5F + 0.5F, nz * 0.5F + 0.5F, 1.0F).setUv(b / 6.2831855F, a / 3.1415927F);
	}
}
