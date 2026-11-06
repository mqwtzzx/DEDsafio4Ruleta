package net.ruleta.puerta.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ruleta.puerta.PuertaBunker;
import net.ruleta.puerta.PuertaBunkerBE;
import net.ruleta.puerta.PuertaBunkerBloque;

/** Las dos hojas de la puerta del bunker (8×11 bloques) que giran sobre sus bisagras, con manijas. */
public class PuertaBunkerRenderer implements BlockEntityRenderer<PuertaBunkerBE> {
	private static final ResourceLocation TEX_IZQ = ResourceLocation.fromNamespaceAndPath("ruleta", "textures/block/puerta_bunker_hoja_izq.png");
	private static final ResourceLocation TEX_DER = ResourceLocation.fromNamespaceAndPath("ruleta", "textures/block/puerta_bunker_hoja_der.png");
	private static final ResourceLocation TEX_BORDE = ResourceLocation.fromNamespaceAndPath("ruleta", "textures/block/puerta_bunker_borde.png");
	private static final ResourceLocation TEX_MANIJA = ResourceLocation.fromNamespaceAndPath("ruleta", "textures/block/puerta_bunker_manija.png");
	private static final int ANCHO = 8;
	private static final int ALTO = 11;
	private static final float ZF = 1.0F;
	private static final float ZB = 0.0F;
	private static final float MEDIA = 4.0F;
	private static final float BISAGRA_X = 0.5F;
	private static final float BISAGRA_Z = 0.5F;
	private static final float ANGULO_ABIERTA = 90.0F;
	private static final float SENTIDO = 1.0F;
	private static final float MANIJA_Y = 1.75F;
	private static final float MANIJA_X = 2.1F;
	private static final float AMPLITUD_BALANCEO = 25.0F;

	public PuertaBunkerRenderer(BlockEntityRendererProvider.Context ctx) {
	}

	@Override
	public boolean shouldRenderOffScreen(PuertaBunkerBE be) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 192;
	}

	@Override
	public void render(PuertaBunkerBE be, float parcial, PoseStack ps, MultiBufferSource buf, int luzCelda, int overlay) {
		Level level = be.getLevel();
		BlockState st = be.getBlockState();
		if (level == null || !st.is(PuertaBunker.BLOQUE.get()))
			return;

		Direction f = st.getValue(PuertaBunkerBloque.FACING);
		Direction r = PuertaBunkerBloque.derecha(f);
		BlockPos o = be.getBlockPos();
		float p = Mth.lerp(parcial, be.anterior, be.progreso);
		float e = p * p * (3.0F - 2.0F * p);
		float angulo = ANGULO_ABIERTA * e * SENTIDO;

		int[] fIzq = new int[ALTO];
		int[] bIzq = new int[ALTO];
		int[] fDer = new int[ALTO];
		int[] bDer = new int[ALTO];
		for (int j = 0; j < ALTO; j++) {
			BlockPos a = o.relative(r, 1).above(j);
			BlockPos b = o.relative(r, 6).above(j);
			fIzq[j] = LevelRenderer.getLightColor(level, a.relative(f));
			bIzq[j] = LevelRenderer.getLightColor(level, a.relative(f.getOpposite()));
			fDer[j] = LevelRenderer.getLightColor(level, b.relative(f));
			bDer[j] = LevelRenderer.getLightColor(level, b.relative(f.getOpposite()));
		}

		ps.pushPose();
		switch (f) {
			case WEST -> ps.translate(1.0D, 0.0D, 0.0D);
			case NORTH -> ps.translate(1.0D, 0.0D, 1.0D);
			case EAST -> ps.translate(0.0D, 0.0D, 1.0D);
			default -> {
			}
		}
		ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));
		float balanceo = -(1.0F - 2.0F * p) * Mth.sin(3.1415927F * p) * AMPLITUD_BALANCEO * SENTIDO;

		ps.pushPose();
		ps.translate(0.5D, 0.0D, 0.5D);
		ps.mulPose(Axis.YP.rotationDegrees(angulo));
		ps.translate(-0.5D, 0.0D, -0.5D);
		hoja(buf, ps, TEX_IZQ, TEX_DER, 0.0F, MEDIA, false, fIzq, bIzq, overlay);
		manijas(buf, ps, MANIJA_X, balanceo, fIzq[1], bIzq[1], overlay);
		ps.popPose();

		ps.pushPose();
		ps.translate(7.5D, 0.0D, 0.5D);
		ps.mulPose(Axis.YP.rotationDegrees(-angulo));
		ps.translate(-7.5D, 0.0D, -0.5D);
		hoja(buf, ps, TEX_DER, TEX_IZQ, MEDIA, 8.0F, true, fDer, bDer, overlay);
		manijas(buf, ps, 5.9F, balanceo, fDer[1], bDer[1], overlay);
		ps.popPose();

		ps.popPose();
	}

	private static void hoja(MultiBufferSource buf, PoseStack ps, ResourceLocation texFrente, ResourceLocation texDorso, float xa, float xb, boolean derecha, int[] luzF, int[] luzB, int ovl) {
		PoseStack.Pose pose = ps.last();
		VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(texFrente));
		for (int j = 0; j < ALTO; j++) {
			float va = (float) (ALTO - (j + 1)) / 11.0F;
			float vb = (float) (ALTO - j) / 11.0F;
			cara(vc, pose, xa, xb, (float) j, (float) (j + 1), ZF, 0.0F, 1.0F, va, vb, 1.0F, luzF[j], ovl);
		}

		vc = buf.getBuffer(RenderType.entityCutoutNoCull(texDorso));
		for (int j = 0; j < ALTO; j++) {
			float va = (float) (ALTO - (j + 1)) / 11.0F;
			float vb = (float) (ALTO - j) / 11.0F;
			cara(vc, pose, xa, xb, (float) j, (float) (j + 1), ZB, 1.0F, 0.0F, va, vb, -1.0F, luzB[j], ovl);
		}

		vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX_BORDE));
		float xBisagra = derecha ? xb : xa;
		float xLibre = derecha ? xa : xb;
		for (int j = 0; j < ALTO; j++) {
			int luz = Math.max(luzF[j], luzB[j]);
			canto(vc, pose, xLibre, (float) j, (float) (j + 1), derecha ? -1.0F : 1.0F, luz, ovl);
			canto(vc, pose, xBisagra, (float) j, (float) (j + 1), derecha ? 1.0F : -1.0F, luz, ovl);
		}

		int ancho = Math.round(xb - xa);
		for (int i = 0; i < ancho; i++) {
			tapa(vc, pose, xa + (float) i, xa + (float) i + 1.0F, 11.0F, 1.0F, luzF[10], ovl);
			tapa(vc, pose, xa + (float) i, xa + (float) i + 1.0F, 0.0F, -1.0F, luzF[0], ovl);
		}
	}

	private static void manijas(MultiBufferSource buf, PoseStack ps, float x, float balanceo, int luzF, int luzB, int ovl) {
		VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX_MANIJA));
		manija(vc, ps, x, MANIJA_Y, ZF, 1.0F, balanceo, luzF, ovl);
		manija(vc, ps, x, MANIJA_Y, ZB, -1.0F, balanceo, luzB, ovl);
	}

	private static void cara(VertexConsumer vc, PoseStack.Pose pose, float xa, float xb, float y0, float y1, float z, float ua, float ub, float va, float vb, float nz, int luz, int ovl) {
		vert(vc, pose, xa, y0, z, ua, vb, 0.0F, 0.0F, nz, luz, ovl);
		vert(vc, pose, xb, y0, z, ub, vb, 0.0F, 0.0F, nz, luz, ovl);
		vert(vc, pose, xb, y1, z, ub, va, 0.0F, 0.0F, nz, luz, ovl);
		vert(vc, pose, xa, y1, z, ua, va, 0.0F, 0.0F, nz, luz, ovl);
	}

	private static void tapa(VertexConsumer vc, PoseStack.Pose pose, float xa, float xb, float y, float ny, int luz, int ovl) {
		vert(vc, pose, xa, y, 0.0F, 0.0F, 0.0F, 0.0F, ny, 0.0F, luz, ovl);
		vert(vc, pose, xb, y, 0.0F, 1.0F, 0.0F, 0.0F, ny, 0.0F, luz, ovl);
		vert(vc, pose, xb, y, 1.0F, 1.0F, 1.0F, 0.0F, ny, 0.0F, luz, ovl);
		vert(vc, pose, xa, y, 1.0F, 0.0F, 1.0F, 0.0F, ny, 0.0F, luz, ovl);
	}

	private static void canto(VertexConsumer vc, PoseStack.Pose pose, float x, float y0, float y1, float nx, int luz, int ovl) {
		vert(vc, pose, x, y0, 0.0F, 0.0F, 1.0F, nx, 0.0F, 0.0F, luz, ovl);
		vert(vc, pose, x, y0, 1.0F, 1.0F, 1.0F, nx, 0.0F, 0.0F, luz, ovl);
		vert(vc, pose, x, y1, 1.0F, 1.0F, 0.0F, nx, 0.0F, 0.0F, luz, ovl);
		vert(vc, pose, x, y1, 0.0F, 0.0F, 0.0F, nx, 0.0F, 0.0F, luz, ovl);
	}

	private static void manija(VertexConsumer vc, PoseStack ps, float cx, float cy, float zCara, float dir, float grados, int luz, int ovl) {
		float radio = 0.4F;
		float tubo = 0.1F;
		float cuello = 0.55F;
		float separacion = 0.12F;
		ps.pushPose();
		ps.translate(cx, cy + radio + cuello, zCara);
		caja(vc, ps.last(), -0.17F, -0.12F, 0.0F, 0.17F, 0.12F, dir * 0.07F, luz, ovl);
		ps.mulPose(Axis.XP.rotationDegrees(grados));
		caja(vc, ps.last(), -0.08F, -cuello, dir * 0.02F, 0.08F, 0.0F, dir * 0.22F, luz, ovl);
		toro(vc, ps.last(), 0.0F, -0.95000005F, dir * separacion, dir, radio, tubo, luz, ovl);
		ps.popPose();
	}

	private static void caja(VertexConsumer vc, PoseStack.Pose pose, float xa, float ya, float za, float xb, float yb, float zb, int luz, int ovl) {
		float xl = Math.min(xa, xb);
		float xh = Math.max(xa, xb);
		float yl = Math.min(ya, yb);
		float yh = Math.max(ya, yb);
		float zl = Math.min(za, zb);
		float zh = Math.max(za, zb);
		quad(vc, pose, xl, yl, zl, xh, yl, zl, xh, yh, zl, xl, yh, zl, 0.0F, 0.0F, -1.0F, luz, ovl);
		quad(vc, pose, xl, yl, zh, xh, yl, zh, xh, yh, zh, xl, yh, zh, 0.0F, 0.0F, 1.0F, luz, ovl);
		quad(vc, pose, xl, yl, zl, xl, yl, zh, xl, yh, zh, xl, yh, zl, -1.0F, 0.0F, 0.0F, luz, ovl);
		quad(vc, pose, xh, yl, zl, xh, yl, zh, xh, yh, zh, xh, yh, zl, 1.0F, 0.0F, 0.0F, luz, ovl);
		quad(vc, pose, xl, yl, zl, xh, yl, zl, xh, yl, zh, xl, yl, zh, 0.0F, -1.0F, 0.0F, luz, ovl);
		quad(vc, pose, xl, yh, zl, xh, yh, zl, xh, yh, zh, xl, yh, zh, 0.0F, 1.0F, 0.0F, luz, ovl);
	}

	private static void quad(VertexConsumer vc, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
							 float dx, float dy, float dz, float nx, float ny, float nz, int luz, int ovl) {
		vert(vc, pose, ax, ay, az, 0.0F, 0.0F, nx, ny, nz, luz, ovl);
		vert(vc, pose, bx, by, bz, 1.0F, 0.0F, nx, ny, nz, luz, ovl);
		vert(vc, pose, cx, cy, cz, 1.0F, 1.0F, nx, ny, nz, luz, ovl);
		vert(vc, pose, dx, dy, dz, 0.0F, 1.0F, nx, ny, nz, luz, ovl);
	}

	private static void toro(VertexConsumer vc, PoseStack.Pose pose, float cx, float cy, float cz, float dir, float radio, float tubo, int luz, int ovl) {
		int n = 16;
		int m = 6;
		for (int i = 0; i < n; i++) {
			double a0 = 6.283185307179586D * (double) i / 16.0D;
			double a1 = 6.283185307179586D * (double) (i + 1) / 16.0D;
			for (int j = 0; j < m; j++) {
				double b0 = 6.283185307179586D * (double) j / 6.0D;
				double b1 = 6.283185307179586D * (double) (j + 1) / 6.0D;
				punto(vc, pose, cx, cy, cz, dir, radio, tubo, a0, b0, (float) i / 16.0F, (float) j / 6.0F, luz, ovl);
				punto(vc, pose, cx, cy, cz, dir, radio, tubo, a1, b0, (float) (i + 1) / 16.0F, (float) j / 6.0F, luz, ovl);
				punto(vc, pose, cx, cy, cz, dir, radio, tubo, a1, b1, (float) (i + 1) / 16.0F, (float) (j + 1) / 6.0F, luz, ovl);
				punto(vc, pose, cx, cy, cz, dir, radio, tubo, a0, b1, (float) i / 16.0F, (float) (j + 1) / 6.0F, luz, ovl);
			}
		}
	}

	private static void punto(VertexConsumer vc, PoseStack.Pose pose, float cx, float cy, float cz, float dir, float radio, float tubo, double a, double b, float u, float v, int luz, int ovl) {
		float ca = (float) Math.cos(a);
		float sa = (float) Math.sin(a);
		float cb = (float) Math.cos(b);
		float sb = (float) Math.sin(b);
		float x = cx + (radio + tubo * cb) * ca;
		float y = cy + (radio + tubo * cb) * sa;
		float z = cz + dir * tubo * sb;
		vert(vc, pose, x, y, z, u, v, cb * ca, cb * sa, dir * sb, luz, ovl);
	}

	/** 1.21.1: addVertex/setColor/setUv/setOverlay/setLight/setNormal (sin endVertex). */
	private static void vert(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, float nx, float ny, float nz, int luz, int ovl) {
		vc.addVertex(pose.pose(), x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(ovl).setLight(luz).setNormal(pose, nx, ny, nz);
	}
}
