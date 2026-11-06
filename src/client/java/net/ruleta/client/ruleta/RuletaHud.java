package net.ruleta.client.ruleta;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.ruleta.Ruleta;
import net.ruleta.RuletaColor;
import net.ruleta.RuletaPayload;
import net.ruleta.client.OpcionesCliente;

/** La ruleta que gira en pantalla (HUD). */
public final class RuletaHud {
	private static final SoundEvent SONIDO = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("ruleta", "ruleta"));
	private static final String[] COLORES = {"rojo", "rosa", "morado", "azul", "cian", "verde", "amarillo", "naranja"};
	private static final float CX0 = -86.0F;
	private static final float CY0 = -216.0F;
	private static final float CX1 = 86.0F;
	private static final float CY1 = 6.0F;
	private static final float PUNTERO_Y = -228.0F;
	private static final float Z = 2000.0F;
	private static final float RESPLANDOR_DESTELLOS = 0.17F;
	private static final float RESPLANDOR_HALO = 0.5F;
	private static final float GEMA_ESCALA = 1.07F;
	private static final boolean DIBUJAR_SEPARADORES = false;
	private static final float GEMA_Y = -8.125F;
	private static Tirada actual;
	private static SoundInstance sonido;

	private RuletaHud() {
	}

	public static void shader(ShaderInstance s) {
		Pintor.shader(s);
	}

	public static void empezar(RuletaPayload p) {
		if (!OpcionesCliente.ruleta)
			return;
		parar();
		actual = new Tirada(p);
		sonido = SimpleSoundInstance.forUI(SONIDO, 1.0F, 1.0F);
		Minecraft.getInstance().getSoundManager().play(sonido);
		Ruleta.LOG.info("[ruleta] empieza la tirada ({})", actual.resultado.id);
	}

	public static void parar() {
		actual = null;
		if (sonido != null) {
			Minecraft.getInstance().getSoundManager().stop(sonido);
			sonido = null;
		}
	}

	public static boolean girando() {
		if (actual != null && actual.terminada())
			actual = null;
		return actual != null;
	}

	public static void precargar() {
		for (String c : COLORES) {
			Pintor.textura("casilla_" + c);
			Pintor.textura("destellos_" + c);
			Pintor.textura("resplandor_" + c);
		}
		for (String n : new String[]{"aro", "bombilla", "halo", "brillo", "disco", "centro", "centro_brillo", "puntero", "puntero_blanco",
				"gema", "gema_brillo", "gema_luz", "gema_vetas_0", "gema_vetas_1", "gema_vetas_2", "gema_franja_0", "gema_franja_1", "gema_franja_2",
				"resplandor", "linea", "separador"})
			Pintor.textura(n);
	}

	public static void dibujar(GuiGraphics ctx) {
		if (girando())
			dibujar(ctx, actual);
	}

	static void dibujar(GuiGraphics ctx, Tirada tr) {
		int sw = ctx.guiWidth();
		int sh = ctx.guiHeight();
		float px;
		float cx;
		float cy;
		if (tr.mini) {
			px = (float) sh * 0.55F * 0.34F / 512.0F;
			cx = (float) sh * 0.06F + 256.0F * px;
			cy = (float) sh * 0.06F + 295.0F * px;
		} else {
			px = Math.min((float) sh * 0.55F, (float) sw * 0.45F) / 512.0F;
			cx = (float) sw / 2.0F;
			cy = (float) sh * 0.5F;
		}

		ctx.flush();
		Pintor.empezar();
		try {
			dibujarRuleta(ctx.pose(), tr, tr.tiempo(), cx, cy, px);
		} finally {
			Pintor.terminar();
		}
	}

	private static void dibujarRuleta(PoseStack pose, Tirada tr, float t, float cx, float cy, float px) {
		float alfa = tr.alfa(t);
		float s = tr.escala(t);
		if (alfa <= 0.003F || s <= 0.001F)
			return;

		float angulo = tr.angulo(t);
		float blur = tr.desenfoque(t);
		int res = tr.resultado.ordinal();
		float blanco = tr.blanco(t);
		pose.pushPose();
		pose.translate(cx, cy, Z);
		pose.scale(px * s, px * s, 1.0F);

		float base = tr.baseBlanca(t);
		if (base > 0.003F)
			Pintor.rect(pose.last().pose(), "disco", -256.0F, -256.0F, 256.0F, 256.0F, Mates.argb(alfa * base, 16777215));

		int copias = blur < 1.0F ? 1 : Math.min(16, 3 + (int) (blur / 2.0F));
		for (int k = 0; k < copias; k++) {
			float atras = copias == 1 ? 0.0F : blur * (float) k / (float) (copias - 1);
			float a = alfa / (float) (k + 1);
			for (int i = 0; i < 8; i++) {
				float color = tr.colorCasilla(i, t);
				if (color <= 0.003F)
					continue;
				pose.pushPose();
				pose.mulPose(Axis.ZP.rotationDegrees(angulo - atras + (float) i * 45.0F));
				Matrix4f m = pose.last().pose();
				Pintor.rect(m, "casilla_" + COLORES[i], CX0, CY0, CX1, CY1, Mates.argb(a * color, 16777215));

				float f = tr.relleno(i, t);
				float[] tono = tr.tono(i, t);
				if (f > 0.003F && tono != null)
					Pintor.rectTono(m, "casilla_" + COLORES[res], CX0, CY0, CX1, CY1, Mates.argb(a * f, 16777215), tono[0], tono[1], tono[2]);

				float respira = respiro(t, i);
				Pintor.rect(m, "destellos_" + COLORES[i], CX0, CY0, CX1, CY1, Mates.argb(a * color * respira, 16777215));
				Pintor.rectSuma(m, "resplandor_" + COLORES[i], CX0, CY0, CX1, CY1, Mates.argb(a * color * respira * RESPLANDOR_DESTELLOS, 16777215));
				pose.popPose();
			}

			float aro = tr.aro(t);
			if (aro > 0.003F) {
				pose.pushPose();
				pose.mulPose(Axis.ZP.rotationDegrees(angulo - atras));
				Pintor.rect(pose.last().pose(), "aro", -256.0F, -256.0F, 256.0F, 256.0F, Mates.argb(a * aro, 16777215));
				pose.popPose();
			}
		}

		float focos = 0.8F * alfa * tr.aro(t) * (1.0F - Mates.entre(blur, 2.0F, 9.0F));
		if (focos > 0.003F) {
			for (int k = 0; k < 16; k++) {
				pose.pushPose();
				pose.mulPose(Axis.ZP.rotationDegrees(angulo + (float) k * 22.5F));
				Pintor.rect(pose.last().pose(), "bombilla", -256.0F, -256.0F, 256.0F, 256.0F, Mates.argb(focos, 16777215));
				pose.popPose();
			}
		}

		// (en el original se calculaba «separa» para los separadores, desactivados con DIBUJAR_SEPARADORES = false)
		float separa = alfa * Math.min(tr.colorCasilla(0, t), tr.colorCasilla(4, t)) * (1.0F - Mates.entre(blur, 2.0F, 9.0F));
		float sombra = alfa * Math.min(tr.colorCasilla(0, t), tr.colorCasilla(4, t));
		if (sombra > 0.003F)
			Pintor.rect(pose.last().pose(), "brillo", -256.0F, -256.0F, 256.0F, 256.0F, Mates.argb(sombra, 16777215));

		float halo = alfa * tr.aro(t) * RESPLANDOR_HALO;
		if (halo > 0.003F)
			Pintor.rectSuma(pose.last().pose(), "halo", -256.0F, -256.0F, 256.0F, 256.0F, Mates.argb(halo, 16777215));

		if (blanco > 0.003F)
			Pintor.rect(pose.last().pose(), "disco", -256.0F, -256.0F, 256.0F, 256.0F, Mates.argb(alfa * blanco, 16777215));

		for (int k = 0; k < 8; k++) {
			float l = tr.linea(k, t);
			if (l > 0.003F) {
				pose.pushPose();
				pose.mulPose(Axis.ZP.rotationDegrees(angulo + (float) k * 45.0F + 22.5F));
				Pintor.rect(pose.last().pose(), "linea", -2.2F, -214.0F * l, 2.2F, 0.0F, Mates.argb(alfa * 0.85F, 3816004));
				pose.popPose();
			}
		}

		float centro = tr.centro(t);
		if (centro > 0.003F) {
			pose.pushPose();
			pose.mulPose(Axis.ZP.rotationDegrees(angulo));
			Pintor.rect(pose.last().pose(), "centro", -32.0F, -32.0F, 32.0F, 32.0F, Mates.argb(alfa * centro, 16777215));
			pose.popPose();
			float vaiven = 3.0F * Mth.sin(t * 1.7F);
			Pintor.rect(pose.last().pose(), "centro_brillo", -32.0F + vaiven, -32.0F, 32.0F + vaiven, 32.0F, Mates.argb(alfa * centro * 0.85F, 16777215));
		}

		float ps = tr.punteroEscala(t);
		float pa = alfa * tr.punteroAlfa(t);
		if (ps > 0.003F && pa > 0.003F) {
			pose.pushPose();
			pose.translate(0.0F, PUNTERO_Y - tr.punteroSube(t), 0.0F);
			pose.mulPose(Axis.ZP.rotationDegrees(tr.puntero(t)));
			pose.scale(ps, ps, 1.0F);
			Matrix4f m = pose.last().pose();
			Pintor.rect(m, "puntero", -64.0F, -40.0F, 64.0F, 88.0F, Mates.argb(pa, 16777215));

			float gx0 = -68.48F;
			float gx1 = 68.48F;
			float gy0 = -42.23125F;
			float gy1 = 94.72875F;
			Pintor.rect(m, "gema", gx0, gy0, gx1, gy1, Mates.argb(pa, tr.gema(t)));

			float vetas = pa * tr.veteado(t);
			if (vetas > 0.003F) {
				int colorVeta = tr.gemaVeta(t);
				int colorFranja = tr.gemaFranja(t);
				for (int k = 0; k < 3; k++) {
					float w = Tirada.vetaPeso(k, t);
					if (w > 0.003F) {
						Pintor.rect(m, "gema_vetas_" + k, gx0, gy0, gx1, gy1, Mates.argb(vetas * w * 0.5F, colorVeta));
						Pintor.rect(m, "gema_franja_" + k, gx0, gy0, gx1, gy1, Mates.argb(vetas * w * 0.55F, colorFranja));
					}
				}
			}

			float bola = pa * tr.revelada(t);
			if (bola > 0.003F) {
				Pintor.rectSuma(m, "gema_luz", gx0, gy0, gx1, gy1, Mates.argb(bola, Mates.mezclar(tr.resultado.rgb, 16756800, 0.6F)));
				Pintor.rect(m, "gema_brillo", gx0, gy0, gx1, gy1, Mates.argb(bola * (0.85F + 0.15F * Mth.sin(t * 3.1F)), 16777215));
			}

			float pb = tr.punteroBlanco(t);
			if (pb > 0.003F)
				Pintor.rect(m, "puntero_blanco", -64.0F, -40.0F, 64.0F, 88.0F, Mates.argb(pa * pb, 16777215));
			pose.popPose();
		}

		float r = tr.resplandor(t);
		if (r > 0.003F) {
			float e = tr.resplandorEscala(t) * 2.0F;
			Pintor.rect(pose.last().pose(), "resplandor", -128.0F * e, -128.0F * e, 128.0F * e, 128.0F * e, Mates.argb(alfa * r, 16777215));
		}
		pose.popPose();
	}

	public static void casillaLanzador(Matrix4f m, int i, float luz) {
		Pintor.empezar();
		try {
			String c = COLORES[Math.floorMod(i, 8)];
			Pintor.rect(m, "casilla_" + c, CX0, CY0, CX1, CY1, -1);
			Pintor.rect(m, "destellos_" + c, CX0, CY0, CX1, CY1, -1);
			if (luz > 0.003F) {
				Pintor.rectSuma(m, "resplandor_" + c, CX0, CY0, CX1, CY1, Mates.argb(luz * 0.9F, 16777215));
				Pintor.rectSuma(m, "casilla_" + c, CX0, CY0, CX1, CY1, Mates.argb(luz * 0.3F, 16777215));
			}
		} finally {
			Pintor.terminar();
		}
	}

	private static float respiro(float t, int i) {
		return 0.86F + 0.14F * Mth.sin(t * 2.6F + (float) i * 0.8F);
	}

	public static void icono(GuiGraphics ctx, int x, int y, int lado, RuletaColor color) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath("ruleta", "textures/gui/icono_ruleta_" + color.id + ".png");
		RenderSystem.enableBlend();
		ctx.blit(id, x, y, lado, lado, 0.0F, 0.0F, 256, 256, 256, 256);
		RenderSystem.disableBlend();
	}
}
