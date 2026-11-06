package net.ruleta.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.ruleta.Efectos;
import net.ruleta.client.reviil.AnimacionReviil;
import net.ruleta.client.reviil.CieloReviil;
import net.ruleta.compat.client.ViewportEvent;

import java.util.HashMap;
import java.util.Map;

/** Temblor de cámara, golpe de cámara y tinte rojo de pantalla. */
public final class EfectosCliente {
	private static final float TEMBLOR = 0.28F;
	private static final int TINTE_ALFA = 62;
	private static final int TINTE_COLOR = 14231592;
	private static final float ENTRA = 10.0F;
	private static final float SALE = 40.0F;
	private static final Map<MobEffect, Long> VISTO = new HashMap<>();
	private static long golpeDesde = -1L;
	private static float golpeFuerza;
	private static final float GOLPE_APAGA = 0.45F;
	private static final float TINTE_ENTRA = 0.5F;
	private static final float TINTE_APAGA = 3.0F;
	private static long tinteDesde = -1L;
	private static long tinteApaga = -1L;

	private EfectosCliente() {
	}

	/** En 1.21.1 los efectos se consultan por Holder. */
	private static float fuerza(Holder<MobEffect> efecto, float parcial) {
		LocalPlayer p = Minecraft.getInstance().player;
		if (p == null)
			return 0.0F;
		MobEffectInstance e = p.getEffect(efecto);
		if (e == null) {
			VISTO.remove(efecto.value());
			return 0.0F;
		}
		long ahora = System.nanoTime();
		long desde = VISTO.computeIfAbsent(efecto.value(), k -> ahora);
		float queda = (float) e.getDuration() - parcial;
		float pasado = (float) (ahora - desde) / 1.0E9F * 20.0F;
		return Math.min(1.0F, Math.max(0.0F, pasado / ENTRA)) * Math.min(1.0F, Math.max(0.0F, queda / SALE));
	}

	public static void golpe(float grados) {
		long ahora = System.nanoTime();
		if (golpeDesde < 0L || grados >= golpeAhora(ahora)) {
			golpeDesde = ahora;
			golpeFuerza = grados;
		}
	}

	private static float golpeAhora(long ahora) {
		if (golpeDesde < 0L)
			return 0.0F;
		float dt = (float) (ahora - golpeDesde) / 1.0E9F;
		if (dt > 1.8F) {
			golpeDesde = -1L;
			return 0.0F;
		}
		return golpeFuerza * (float) Math.exp((double) (-dt / GOLPE_APAGA));
	}

	public static void camara(ViewportEvent.ComputeCameraAngles e) {
		long ahora = System.nanoTime();
		float g = golpeAhora(ahora);
		if (g > 0.0F) {
			double tg = (double) (ahora - golpeDesde) / 1.0E9D;
			e.setYaw(e.getYaw() + (float) (Math.sin(tg * 31.0D) * 0.7D + Math.sin(tg * 47.0D + 1.1D) * 0.3D) * g);
			e.setPitch(e.getPitch() + (float) (Math.sin(tg * 37.0D + 0.6D) * 0.7D + Math.sin(tg * 53.0D + 2.0D) * 0.3D) * g);
			e.setRoll(e.getRoll() + (float) Math.sin(tg * 23.0D + 0.3D) * g * 0.6F);
		}
		float f = fuerza(Efectos.TEMBLOR.holder(), (float) e.getPartialTick());
		if (f <= 0.0F)
			return;
		double t = (double) System.nanoTime() / 1.0E9D;
		float a = TEMBLOR * f;
		float yaw = (float) (Math.sin(t * 9.0D) * 0.6D + Math.sin(t * 14.0D + 1.3D) * 0.4D) * a;
		float pitch = (float) (Math.sin(t * 11.0D + 2.1D) * 0.6D + Math.sin(t * 17.0D + 0.4D) * 0.4D) * a;
		float roll = (float) (Math.sin(t * 7.0D + 0.7D) * 0.5D + Math.sin(t * 12.0D + 2.6D) * 0.5D) * a * 0.6F;
		e.setYaw(e.getYaw() + yaw);
		e.setPitch(e.getPitch() + pitch);
		e.setRoll(e.getRoll() + roll);
	}

	public static void tinte(GuiGraphics ctx, float parcial) {
		long ahora = System.nanoTime();
		boolean efecto = fuerza(Efectos.RULETA_ROJA.holder(), parcial) > 0.0F;
		if (tinteDesde < 0L) {
			if (!efecto)
				return;
			tinteDesde = ahora;
			tinteApaga = -1L;
		}
		boolean sigue = efecto || AnimacionReviil.musicaSonando() || CieloReviil.musicaMomentoSonando();
		if (sigue)
			tinteApaga = -1L;
		else if (tinteApaga < 0L)
			tinteApaga = ahora;
		float f = Math.min(1.0F, (float) (ahora - tinteDesde) / 1.0E9F / TINTE_ENTRA);
		if (tinteApaga >= 0L) {
			float x = Math.min(1.0F, (float) (ahora - tinteApaga) / 1.0E9F / TINTE_APAGA);
			if (x >= 1.0F) {
				tinteDesde = -1L;
				tinteApaga = -1L;
				return;
			}
			f *= 1.0F - x * x * (3.0F - 2.0F * x);
		}
		int a = Math.round((float) TINTE_ALFA * f);
		if (a > 0)
			ctx.fill(0, 0, ctx.guiWidth(), ctx.guiHeight(), a << 24 | TINTE_COLOR);
	}

	public static void limpiarTinte() {
		tinteDesde = -1L;
		tinteApaga = -1L;
	}
}
