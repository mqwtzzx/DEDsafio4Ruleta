package net.cloudruleta.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Temporizador arriba en el centro (rojo en los últimos 10 segundos). */
public final class TemporizadorHud {
	static final int Y = 6;
	private static long finNanos = -1L;
	private static boolean saliendo;
	private static final Fundido fundido = new Fundido(0.2F, 1.5F);

	private TemporizadorHud() {
	}

	public static void poner(int segundos) {
		if (segundos < 0) {
			if (finNanos >= 0L)
				saliendo = true;
		} else {
			finNanos = System.nanoTime() + (long) segundos * 1000000000L;
			saliendo = false;
		}
	}

	static void limpiar() {
		finNanos = -1L;
		saliendo = false;
		fundido.cero();
	}

	public static boolean activo() {
		return finNanos >= 0L;
	}

	public static boolean saliendo() {
		return saliendo;
	}

	static void render(GuiGraphics ctx, Font tr) {
		if (finNanos < 0L)
			return;
		if (!saliendo && System.nanoTime() >= finNanos)
			saliendo = true;
		float k = fundido.avanzar(!saliendo && !HudCliente.tapado() && OpcionesCliente.temporizador);
		if (saliendo && fundido.apagado()) {
			limpiar();
			return;
		}
		int alfa = Math.round(k * 255.0F);
		if (alfa < 4)
			return;
		long quedanNs = finNanos - System.nanoTime();
		int quedan = (int) Math.max(0L, (quedanNs + 999999999L) / 1000000000L);
		String txt = quedan >= 3600 ? String.format("%d:%02d:%02d", quedan / 3600, quedan / 60 % 60, quedan % 60)
				: String.format("%02d:%02d", quedan / 60, quedan % 60);
		PoseStack m = ctx.pose();
		m.pushPose();
		m.translate(0.0F, 0.0F, 2951.0F);
		ctx.drawCenteredString(tr, txt, ctx.guiWidth() / 2, Y, alfa << 24 | (quedan <= 10 ? 16733525 : 16777215));
		m.popPose();
	}
}
