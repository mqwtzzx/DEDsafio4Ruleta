package net.cloudruleta.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.cloudruleta.Mensajes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Recuadro de aviso arriba a la izquierda (título metálico + texto), se oculta con una tecla. */
public final class AvisoHud {
	public static final KeyMapping OCULTAR = new KeyMapping("key.cloudruleta.ocultar_aviso", InputConstants.Type.KEYSYM, 79, "key.categories.cloudruleta");
	private static final int X = 10;
	private static final int Y = 10;
	private static final int ANCHO = 240;
	private static final int ALTO = 80;
	private static final int MARGEN = 8;
	private static final int TITULO = 6;
	private static final int CUERPO = 21;
	private static final int PISTA = 4;
	private static final float LINEA = 11.5F;
	private static final float[] ESCALAS = {1.0F, 0.8F, 0.7F, 0.6F, 0.5F};
	private static final int[] LINEAS = {3, 4, 4, 4, 4};
	private static final int FONDO = 94;
	private static final float COORDS = 11.0F;
	private static final float OCULTAR_SEG = 0.25F;
	private static Component titulo;
	private static List<FormattedCharSequence> lineas = List.of();
	private static float escala = 1.0F;
	private static boolean saliendo;
	private static boolean oculto;
	private static final long DURA_NS = 10000000000L;
	private static long puesto;
	private static final Fundido fundido = new Fundido(0.2F, 1.5F);
	private static final Fundido bajada = new Fundido(0.3F, 0.3F);

	private AvisoHud() {
	}

	public static void poner(Component tituloNuevo, Component aviso) {
		titulo = tituloNuevo;
		Font tr = Minecraft.getInstance().font;
		for (int i = 0; i < ESCALAS.length; i++) {
			escala = ESCALAS[i];
			lineas = tr.split(aviso, (int) ((float) (ANCHO - 2 * MARGEN) / escala));
			if (lineas.size() <= LINEAS[i])
				break;
		}
		saliendo = false;
		oculto = false;
		puesto = System.nanoTime();
	}

	public static boolean visible() {
		return titulo != null && !saliendo;
	}

	public static void quitar() {
		if (titulo != null)
			saliendo = true;
	}

	static void limpiar() {
		titulo = null;
		saliendo = false;
		fundido.cero();
	}

	public static void alternar() {
		if (titulo != null && !saliendo)
			oculto = !oculto;
	}

	static void render(GuiGraphics ctx, Font tr) {
		if (titulo != null && !saliendo && System.nanoTime() - puesto > DURA_NS)
			saliendo = true;
		if (titulo == null)
			return;
		float k = fundido.avanzar(!saliendo && !oculto && OpcionesCliente.aviso && !HudCliente.tapado(), !oculto && OpcionesCliente.aviso ? 1.5F : OCULTAR_SEG);
		if (saliendo && fundido.apagado()) {
			limpiar();
			return;
		}
		int centro = ctx.guiWidth() / 2;
		boolean choca = X + ANCHO > centro - tr.width("00:00") / 2 - 4;
		float y0 = (float) Y + 8.0F * bajada.avanzar(choca && TemporizadorHud.activo());
		int alfa = Math.round(k * 255.0F);
		if (alfa < 4)
			return;
		int alto = Math.max(ALTO, CUERPO + Math.round((float) lineas.size() * LINEA * escala) + 6);
		int x = X + MARGEN;
		int color = alfa << 24 | 16777215;
		PoseStack m = ctx.pose();
		m.pushPose();
		m.translate(0.0F, y0 + COORDS, 2951.0F);
		ctx.fill(X, 0, X + ANCHO, alto, Math.round((float) FONDO * k) << 24);
		m.translate(0.0F, 0.0F, 1.0F);
		tituloMetalico(ctx, tr, titulo, x, TITULO, alfa);
		m.pushPose();
		m.translate((float) x, (float) CUERPO, 0.0F);
		m.scale(escala, escala, 1.0F);
		for (FormattedCharSequence linea : lineas) {
			ctx.drawString(tr, linea, 0, 0, color);
			m.translate(0.0F, LINEA, 0.0F);
		}
		m.popPose();
		if (!OCULTAR.isUnbound()) {
			Component pista = Component.literal("[Presiona ").append(OCULTAR.getTranslatedKeyMessage()).append("] para ocultar aviso").withStyle(ChatFormatting.GRAY);
			ctx.drawString(tr, pista, x, alto + PISTA, color);
		}
		m.popPose();
	}

	static void coordenadas(GuiGraphics ctx, Font tr) {
		coordenadas(ctx, tr, 255);
	}

	private static void coordenadas(GuiGraphics ctx, Font tr, int alfa) {
		LocalPlayer p = Minecraft.getInstance().player;
		if (p == null || !OpcionesCliente.coordenadas)
			return;
		String txt = String.format(Locale.ROOT, "X: %.2f, Y: %.2f, Z: %.2f", p.getX(), p.getY(), p.getZ());
		ctx.drawString(tr, txt, X, Y, alfa << 24 | 16777215, true);
	}

	/** Título con un brillo que lo recorre (los iconos de la fuente privada se dibujan tal cual). */
	private static void tituloMetalico(GuiGraphics ctx, Font tr, Component t, int x, int y, int alfa) {
		int[] base = {-1};
		List<Object[]> trozos = new ArrayList<>();
		t.visit((estilo, texto) -> {
			trozos.add(new Object[]{estilo, texto});
			if (base[0] < 0 && estilo.getColor() != null && !texto.isBlank() && texto.codePointAt(0) < 57344)
				base[0] = estilo.getColor().getValue();
			return Optional.empty();
		}, Style.EMPTY);
		if (base[0] < 0) {
			ctx.drawString(tr, t, x, y, alfa << 24 | 16777215);
			return;
		}
		int claro = Mensajes.claro(base[0]);
		int oscuro = mezcla(claro, 0, 0.3F);
		float ancho = (float) Math.max(1, tr.width(t));
		double s = (double) System.nanoTime() / 1.0E9D % 4.4D;
		float pasada = (float) (s < 2.2D ? s / 2.2D : 2.0D - s / 2.2D);
		float centro = -0.2F + 1.4F * (pasada * pasada * (3.0F - 2.0F * pasada));
		int cx = x;
		for (Object[] tr0 : trozos) {
			Style estilo = (Style) tr0[0];
			String texto = (String) tr0[1];
			int k = 0;
			while (k < texto.length()) {
				int cp = texto.codePointAt(k);
				k += Character.charCount(cp);
				Component letra = Component.literal(new String(Character.toChars(cp)));
				int col;
				if (cp >= 57344 && cp <= 63743) {
					letra = letra.copy().setStyle(estilo);
					col = 16777215;
				} else {
					float u = (float) (cx - x) / ancho;
					float onda = 0.5F + 0.5F * (float) Math.cos((double) (u - centro) * Math.PI * 2.2D);
					col = mezcla(oscuro, claro, onda);
					float brillo = (float) Math.exp(-Math.pow((double) ((u - centro) / 0.12F), 2.0D));
					col = mezcla(col, 16774898, brillo * 0.8F);
					letra = letra.copy().setStyle(estilo.withColor(col));
				}
				ctx.drawString(tr, letra, cx, y, alfa << 24 | col);
				cx += tr.width(letra);
			}
		}
	}

	private static int mezcla(int a, int b, float k) {
		k = Math.max(0.0F, Math.min(1.0F, k));
		int r = Math.round((float) (a >> 16 & 255) + (float) ((b >> 16 & 255) - (a >> 16 & 255)) * k);
		int g = Math.round((float) (a >> 8 & 255) + (float) ((b >> 8 & 255) - (a >> 8 & 255)) * k);
		int bl = Math.round((float) (a & 255) + (float) ((b & 255) - (a & 255)) * k);
		return r << 16 | g << 8 | bl;
	}
}
