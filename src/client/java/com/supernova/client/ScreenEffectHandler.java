package com.supernova.client;

import com.supernova.SupernovaState;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.ruleta.client.reviil.CieloReviil;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.ViewportEvent;

public class ScreenEffectHandler {
	public static float skyMix;
	public static float skyBright;
	public static float skyDark;
	public static boolean haySupernova;
	public static float mulR = 1.0F;
	public static float mulG = 1.0F;
	public static float mulB = 1.0F;
	public static float addR;
	public static float addG;
	public static float addB;
	public static float flash;
	public static float inside;
	public static float fogAmt;
	public static float fogR;
	public static float fogG;
	public static float fogB;
	public static float starX;
	public static float starY = 1.0F;
	public static float starZ;
	public static float aparece = 99.0F;

	private static float sat(float v) {
		return Mth.clamp(v, 0.0F, 1.0F);
	}

	private static float smooth(float v) {
		return SupernovaState.smooth(v);
	}

	private static void reset() {
		skyDark = skyBright = skyMix = 0.0F;
		mulB = mulG = mulR = 1.0F;
		addB = addG = addR = 0.0F;
		inside = flash = 0.0F;
		fogB = fogG = fogR = fogAmt = 0.0F;
	}

	public static void compute(float pt) {
		reset();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null)
			return;
		Vec3 eye = mc.player.getEyePosition(pt);
		float now = (float) mc.level.getGameTime() + pt;

		SupernovaState best = null;
		float bestNear = 0.0F;
		float edad = 0.0F;
		float queda = 0.0F;
		for (SupernovaState s : SupernovaGhosts.active(now)) {
			edad = Math.max(edad, s.age(now));
			queda = Math.max(queda, s.fade(now));
		}
		haySupernova = false;
		for (SupernovaState s : SupernovaGhosts.active(now)) {
			float d = (float) eye.distanceTo(s.center());
			float near = 1.0F + 1.0F / (1.0F + d);
			if (near > bestNear) {
				bestNear = near;
				best = s;
			}
		}
		if (best == null || bestNear <= 0.001F)
			return;

		haySupernova = true;
		SupernovaState s = best;
		aparece = s.age(now) / 20.0F;
		Vec3 haciaEstrella = s.center().subtract(eye);
		if (haciaEstrella.lengthSqr() > 1.0E-6D) {
			haciaEstrella = haciaEstrella.normalize();
			starX = (float) haciaEstrella.x;
			starY = (float) haciaEstrella.y;
			starZ = (float) haciaEstrella.z;
		}

		float d = (float) eye.distanceTo(s.center());
		float amount = Math.min(1.0F, bestNear) * smooth(edad / 100.0F) * queda;
		float mr = 1.0F;
		float mg = 0.64F;
		float mb = 0.42F;
		float ar = 0.1F;
		float ag = 0.035F;
		float ab = 0.0F;
		float fr = 0.82F;
		float fg = 0.36F;
		float fb = 0.12F;
		float bright = 0.0F;
		float darkSky = 0.0F;
		float fl = 0.0F;
		if (s.isDetonating() && !s.isBlasting(now)) {
			float k = s.charge(now);
			float kk = smooth(k / 0.12F);
			mr = Mth.lerp(kk, mr, 1.0F);
			mg = Mth.lerp(kk, mg, 0.8F);
			mb = Mth.lerp(kk, mb, 0.55F);
			float lit = kk * (0.7F + 0.3F * k);
			ar = 0.1F + 0.32F * lit;
			ag = 0.035F + 0.18F * lit;
			ab = 0.04F * lit;
			float white = smooth((k - 0.88F) / 0.12F);
			ar += 0.5F * white;
			ag += 0.48F * white;
			ab += 0.5F * white;
			fr = Mth.lerp(kk, fr, 1.0F);
			fg = Mth.lerp(kk, fg, 0.62F);
			fb = Mth.lerp(kk, fb, 0.3F);
			bright = kk;
		} else if (s.isBlasting(now)) {
			float bt = s.blastTicks(now);
			fl = bt < 5.0F ? 1.0F : (float) Math.exp((double) (-(bt - 5.0F) / 16.0F));
			mr = 1.0F;
			mg = 0.52F;
			mb = 0.36F;
			ar = 0.08F;
			ag = 0.02F;
			fr = 0.62F;
			fg = 0.2F;
			fb = 0.07F;
			darkSky = 1.0F;
			float r = s.radius(now);
			if (d < r)
				inside = sat((r - d) / 15.0F) * s.fade(now);
		}

		skyMix = amount;
		skyBright = bright;
		skyDark = darkSky;
		mulR = Mth.lerp(amount, 1.0F, mr);
		mulG = Mth.lerp(amount, 1.0F, mg);
		mulB = Mth.lerp(amount, 1.0F, mb);
		addR = ar * amount;
		addG = ag * amount;
		addB = ab * amount;
		flash = fl;
		fogAmt = amount;
		fogR = fr;
		fogG = fg;
		fogB = fb;
		if (CieloReviil.visible()) {
			skyMix = 0.0F;
			skyDark = 0.0F;
			fogAmt = 0.0F;
		}
	}

	@SubscribeEvent
	public static void onFogColor(ViewportEvent.ComputeFogColor e) {
		compute((float) e.getPartialTick());
		if (fogAmt <= 0.001F)
			return;
		e.setRed(Mth.lerp(fogAmt, e.getRed(), fogR));
		e.setGreen(Mth.lerp(fogAmt, e.getGreen(), fogG));
		e.setBlue(Mth.lerp(fogAmt, e.getBlue(), fogB));
	}
}
