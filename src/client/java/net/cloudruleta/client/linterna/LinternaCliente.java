package net.cloudruleta.client.linterna;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.joml.Matrix4f;
import org.joml.Vector4f;

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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.client.eclipse.EclipseCliente;
import net.cloudruleta.compat.EventPriority;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.client.InputEvent;
import net.cloudruleta.compat.client.RenderGuiEvent;
import net.cloudruleta.compat.client.RenderLevelStageEvent;
import net.cloudruleta.compat.event.TickEvent;
import net.cloudruleta.objetos.Linterna;
import net.cloudruleta.objetos.Objetos;

/** Linterna (cliente): carga en el HUD, haz de luz en el mundo y oscuridad del eclipse con huecos de luz. */
public final class LinternaCliente {
	private static boolean esperarSoltar;
	private static long luzEn;
	private static boolean luzLista;
	private static float luzCache;
	private static long otrosEn;
	private static boolean otrosListo;
	private static float otrosCache;
	private static Matrix4f proyeccion;
	private static Matrix4f vista;
	private static Vec3 camara;
	private static float parcial;
	private static ShaderInstance shaderHaz;
	private static ShaderInstance shaderNegro;
	private static TextureTarget profundidad;
	private static final String[] POS = {"Luz0Pos", "Luz1Pos", "Luz2Pos", "Luz3Pos"};
	private static final String[] DIR = {"Luz0Dir", "Luz1Dir", "Luz2Dir", "Luz3Dir"};

	private LinternaCliente() {
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ClientTickEvent e) {
		if (e.phase != TickEvent.Phase.END)
			return;
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || !mc.options.keyUse.isDown()) {
			esperarSoltar = false;
			return;
		}
		if (p.isUsingItem() && p.getUseItem().is(Objetos.LINTERNA.get()) && p.getTicksUsingItem() >= 59)
			esperarSoltar = true;
	}

	@SubscribeEvent
	public static void alClic(InputEvent.InteractionKeyMappingTriggered e) {
		if (esperarSoltar && e.isUseItem() && !enMano().isEmpty()) {
			e.setCanceled(true);
			e.setSwingHand(false);
		}
	}

	public static ItemStack enMano() {
		LocalPlayer p = Minecraft.getInstance().player;
		if (p == null)
			return ItemStack.EMPTY;
		if (p.getMainHandItem().is(Objetos.LINTERNA.get()))
			return p.getMainHandItem();
		return p.getOffhandItem().is(Objetos.LINTERNA.get()) ? p.getOffhandItem() : ItemStack.EMPTY;
	}

	public static float luz() {
		long ahora = System.nanoTime();
		if (luzLista && ahora - luzEn < 2000000L)
			return luzCache;
		ItemStack s = enMano();
		luzCache = s.isEmpty() ? 0.0F : Linterna.intensidad(s);
		luzEn = ahora;
		luzLista = true;
		return luzCache;
	}

	@SubscribeEvent
	public static void alDibujarHud(RenderGuiEvent.Post e) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || !p.isUsingItem() || !p.getUseItem().is(Objetos.LINTERNA.get()))
			return;

		float usados = (float) p.getTicksUsingItem() + e.getPartialTick();
		float k = Mth.clamp(usados / 60.0F, 0.0F, 1.0F);
		GuiGraphics g = e.getGuiGraphics();
		float cx = (float) g.guiWidth() / 2.0F;
		float cy = (float) g.guiHeight() - 62.0F;
		Matrix4f m = g.pose().last().pose();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		disco(b, m, cx, cy, 13.0F, 1.0F, -536870912);
		disco(b, m, cx, cy, 10.0F, 1.0F, 1090519039);
		disco(b, m, cx, cy, 10.0F, k, -1);
		disco(b, m, cx, cy, 7.0F, 1.0F, -16777216);
		// 1.21.1: build() devuelve null si no se añadió ningún vértice
		MeshData malla = b.build();
		if (malla != null)
			BufferUploader.drawWithShader(malla);
		RenderSystem.enableCull();

		float seg = Mth.clamp((float) Math.ceil((double) (usados / 2.0F)) / 10.0F, 0.1F, 3.0F);
		String n = String.format(Locale.ROOT, "%.1f", seg);
		g.pose().pushPose();
		g.pose().translate(cx, cy, 0.0F);
		g.pose().scale(0.75F, 0.75F, 1.0F);
		g.drawString(mc.font, n, -mc.font.width(n) / 2, -4, 16777215, false);
		g.pose().popPose();
	}

	private static void disco(BufferBuilder b, Matrix4f m, float cx, float cy, float r, float k, int argb) {
		int seg = 48;
		int hasta = Math.round((float) seg * k);
		for (int i = 0; i < hasta; i++) {
			float a0 = (float) (-Math.PI / 2.0D + (double) i * Math.PI * 2.0D / (double) seg);
			float a1 = (float) (-Math.PI / 2.0D + (double) (i + 1) * Math.PI * 2.0D / (double) seg);
			v(b, m, cx, cy, argb);
			v(b, m, cx + Mth.cos(a0) * r, cy + Mth.sin(a0) * r, argb);
			v(b, m, cx + Mth.cos(a1) * r, cy + Mth.sin(a1) * r, argb);
		}
	}

	private static void v(BufferBuilder b, Matrix4f m, float x, float y, int argb) {
		b.addVertex(m, x, y, 0.0F).setColor(argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >>> 24);
	}

	public static float luzOtros() {
		long ahora = System.nanoTime();
		if (otrosListo && ahora - otrosEn < 2000000L)
			return otrosCache;
		float m = 0.0F;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null) {
			for (Player otro : mc.level.players()) {
				if (otro == mc.player || otro.isSpectator())
					continue;
				m = Math.max(m, Math.max(Linterna.intensidad(otro.getMainHandItem().is(Objetos.LINTERNA.get()) ? otro.getMainHandItem() : ItemStack.EMPTY),
						Linterna.intensidad(otro.getOffhandItem().is(Objetos.LINTERNA.get()) ? otro.getOffhandItem() : ItemStack.EMPTY)));
			}
		}
		otrosCache = m;
		otrosEn = ahora;
		otrosListo = true;
		return m;
	}

	public static float luzTotal() {
		return Math.max(luz(), luzOtros());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void alEmpezarMundo(RenderLevelStageEvent e) {
		if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
			return;
		proyeccion = new Matrix4f(e.getProjectionMatrix());
		vista = new Matrix4f().rotateX(e.getCamera().getXRot() * 0.017453292F).rotateY((e.getCamera().getYRot() + 180.0F) * 0.017453292F);
		camara = e.getCamera().getPosition();
		parcial = e.getPartialTick();
	}

	private static float[] centroPropio() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || proyeccion == null)
			return new float[]{0.0F, 0.0F};
		float yaw = mc.player.getYRot() * 0.017453292F;
		Vec3 ojos = mc.player.getEyePosition();
		Vec3 p = ojos.add((double) (-Mth.sin(yaw)) * 7.0D, 0.0D, (double) Mth.cos(yaw) * 7.0D);
		Vector4f v = new Vector4f((float) (p.x - camara.x), (float) (p.y - camara.y), (float) (p.z - camara.z), 1.0F);
		vista.transform(v);
		proyeccion.transform(v);
		return v.w <= 0.1F ? null : new float[]{v.x / v.w, v.y / v.w};
	}

	public static List<float[]> focos() {
		List<float[]> r = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || proyeccion == null)
			return r;
		for (Player otro : mc.level.players()) {
			if (otro == mc.player || otro.isSpectator())
				continue;
			ItemStack s = otro.getMainHandItem().is(Objetos.LINTERNA.get()) ? otro.getMainHandItem()
					: otro.getOffhandItem().is(Objetos.LINTERNA.get()) ? otro.getOffhandItem() : ItemStack.EMPTY;
			float k = s.isEmpty() ? 0.0F : Linterna.intensidad(s);
			if (k <= 0.0F)
				continue;
			Vec3 ojos = otro.getEyePosition();
			float yawOtro = otro.getYRot() * 0.017453292F;
			Vec3 fin = ojos.add((double) (-Mth.sin(yawOtro)) * 9.0D, 0.0D, (double) Mth.cos(yawOtro) * 9.0D);
			HitResult hit = mc.level.clip(new ClipContext(ojos, fin, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, otro));
			Vec3 p = hit.getLocation();
			double largo = p.distanceTo(ojos);
			Vector4f v = new Vector4f((float) (p.x - camara.x), (float) (p.y - camara.y), (float) (p.z - camara.z), 1.0F);
			vista.transform(v);
			proyeccion.transform(v);
			if (v.w <= 0.1F)
				continue;
			float radioMundo = (float) (1.2D + largo * 0.16D) * (0.6F + 0.4F * k);
			float radio = radioMundo * proyeccion.m11() / v.w;
			r.add(new float[]{v.x / v.w, v.y / v.w, radio, k});
		}
		return r;
	}

	public static void oscurecer(float negro, float luz) {
		haz(true, 1.0F - Mth.clamp(negro, 0.0F, 0.995F));
	}

	public static void shaderHaz(ShaderInstance s) {
		shaderHaz = s;
	}

	public static void shaderNegro(ShaderInstance s) {
		shaderNegro = s;
	}

	private static float fuerza(Player j) {
		ItemStack s = j.getMainHandItem().is(Objetos.LINTERNA.get()) ? j.getMainHandItem()
				: j.getOffhandItem().is(Objetos.LINTERNA.get()) ? j.getOffhandItem() : ItemStack.EMPTY;
		return s.isEmpty() ? 0.0F : Linterna.intensidad(s);
	}

	private static List<float[]> linternas() {
		List<float[]> r = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || camara == null)
			return r;
		List<Player> otros = new ArrayList<>();
		for (Player j : mc.level.players()) {
			if (!j.isSpectator() && fuerza(j) > 0.0F && j.distanceToSqr(camara) < 4096.0D)
				otros.add(j);
		}
		otros.sort(Comparator.comparingDouble(j -> j == mc.player ? -1.0D : j.distanceToSqr(camara)));
		for (Player j : otros) {
			if (r.size() >= 4)
				break;
			Vec3 ojos = j.getEyePosition(parcial);
			Vec3 mira = j.getViewVector(parcial);
			Vec3 lado = mira.cross(new Vec3(0.0D, 1.0D, 0.0D));
			if (lado.lengthSqr() > 1.0E-4D)
				ojos = ojos.add(lado.normalize().scale(0.25D));
			ojos = ojos.add(0.0D, -0.2D, 0.0D);
			r.add(new float[]{(float) (ojos.x - camara.x), (float) (ojos.y - camara.y), (float) (ojos.z - camara.z), fuerza(j), (float) mira.x, (float) mira.y, (float) mira.z});
		}
		return r;
	}

	private static void haz(boolean negro, float fuera) {
		Minecraft mc = Minecraft.getInstance();
		ShaderInstance sh = negro ? shaderNegro : shaderHaz;
		if (sh == null || proyeccion == null)
			return;
		List<float[]> luces = linternas();
		if (luces.isEmpty() && !negro)
			return;

		RenderTarget principal = mc.getMainRenderTarget();
		if (profundidad == null)
			profundidad = new TextureTarget(principal.width, principal.height, true, Minecraft.ON_OSX);
		else if (profundidad.width != principal.width || profundidad.height != principal.height)
			profundidad.resize(principal.width, principal.height, Minecraft.ON_OSX);
		profundidad.copyDepthFrom(principal);
		principal.bindWrite(false);

		Matrix4f inv = new Matrix4f(proyeccion).mul(vista).invert();
		sh.safeGetUniform("InvMat").set(inv);
		for (int i = 0; i < 4; i++) {
			if (i < luces.size()) {
				float[] l = luces.get(i);
				sh.safeGetUniform(POS[i]).set(l[0], l[1], l[2], l[3]);
				sh.safeGetUniform(DIR[i]).set(l[4], l[5], l[6], 0.0F);
			} else {
				sh.safeGetUniform(POS[i]).set(0.0F, 0.0F, 0.0F, 0.0F);
			}
		}
		sh.safeGetUniform("Modo").set(negro ? 1.0F : 0.0F);
		sh.safeGetUniform("Fuera").set(fuera);

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		try {
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR, negro ? GlStateManager.DestFactor.ZERO : GlStateManager.DestFactor.ONE,
					GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
			RenderSystem.setShaderTexture(0, profundidad.getDepthTextureId());
			RenderSystem.setShader(() -> sh);
			BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
			b.addVertex(-1.0F, -1.0F, 0.0F);
			b.addVertex(1.0F, -1.0F, 0.0F);
			b.addVertex(1.0F, 1.0F, 0.0F);
			b.addVertex(-1.0F, 1.0F, 0.0F);
			BufferUploader.drawWithShader(b.buildOrThrow());
		} finally {
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
		}
	}

	private static void g(BufferBuilder b, float x, float y, float k) {
		b.addVertex(x, y, 0.0F).setColor(k, k, k, 1.0F);
	}

	@SubscribeEvent
	public static void alDibujarMundo(RenderLevelStageEvent e) {
		if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || EclipseCliente.activo())
			return;
		haz(false, 0.0F);
	}

	private static void mancha(BufferBuilder b, float cx, float cy, float r, float a, float aspecto) {
		int seg = 40;
		for (int i = 0; i < seg; i++) {
			float a0 = (float) ((double) i * Math.PI * 2.0D / (double) seg);
			float a1 = (float) ((double) (i + 1) * Math.PI * 2.0D / (double) seg);
			b.addVertex(cx, cy, 0.0F).setColor(1.0F, 0.98F, 0.9F, a);
			b.addVertex(cx + Mth.cos(a0) * r / aspecto, cy + Mth.sin(a0) * r, 0.0F).setColor(1.0F, 0.98F, 0.9F, 0.0F);
			b.addVertex(cx + Mth.cos(a1) * r / aspecto, cy + Mth.sin(a1) * r, 0.0F).setColor(1.0F, 0.98F, 0.9F, 0.0F);
		}
	}
}
