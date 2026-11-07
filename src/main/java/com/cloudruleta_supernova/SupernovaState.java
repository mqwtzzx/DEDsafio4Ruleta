package com.cloudruleta_supernova;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Estado de una estrella (compartido por servidor y cliente) y las curvas de tamaño, carga y onda. */
public final class SupernovaState {
	public static final int COUNTDOWN = 600;
	public static final int COLLAPSE = 200;
	public static final int EXPAND = 360;
	public static final int BLAST = 480;
	public static final float FADE_START = 0.8F;
	public static final int SKY_FADE_IN = 100;
	public static final int SHOCK_TICKS = 300;
	public static final float SHOCK_FACTOR = 4.0F;
	public static final float DEFAULT_RADIUS = 250.0F;
	public static final float IDLE_MIN_RADIUS = 6.0F;
	public static final float IDLE_MAX_RADIUS = 14.0F;
	public static final float IDLE_GROW_TICKS = 6000.0F;
	public static final float COUNTDOWN_RADIUS = 2.5F;
	public static final float HOVER = 12.0F;
	public final int id;
	public final String dim;
	public final double x;
	public final double y;
	public final double z;
	public final float maxRadius;
	public final int born;
	public final int detonated;
	public final int hold;

	public SupernovaState(int id, String dim, double x, double y, double z, float maxRadius, int born, int detonated, int hold) {
		this.id = id;
		this.dim = dim;
		this.x = x;
		this.y = y;
		this.z = z;
		this.maxRadius = maxRadius;
		this.born = born;
		this.detonated = detonated;
		this.hold = Math.max(0, hold);
	}

	public static float sat(float v) {
		return Mth.clamp(v, 0.0F, 1.0F);
	}

	public static float smooth(float v) {
		v = sat(v);
		return v * v * (3.0F - 2.0F * v);
	}

	public Vec3 position() {
		return new Vec3(this.x, this.y, this.z);
	}

	public Vec3 center() {
		return new Vec3(this.x, this.y + (double) HOVER, this.z);
	}

	public boolean isDetonating() {
		return this.detonated >= 0;
	}

	public float age(float now) {
		return Math.max(0.0F, now - (float) this.born);
	}

	public float countTicks(float now) {
		return this.isDetonating() ? Math.max(0.0F, now - (float) this.detonated) : 0.0F;
	}

	private float idleAge(float now) {
		float end = this.isDetonating() ? (float) this.detonated : now;
		return Math.max(0.0F, end - (float) this.born);
	}

	public float charge(float now) {
		return this.isDetonating() ? sat((this.countTicks(now) - 400.0F) / 200.0F) : 0.0F;
	}

	public boolean isBlasting(float now) {
		return this.isDetonating() && this.countTicks(now) >= 600.0F;
	}

	public float blastTicks(float now) {
		return this.isDetonating() ? Math.max(0.0F, this.countTicks(now) - 600.0F) : 0.0F;
	}

	public int total() {
		return BLAST + this.hold;
	}

	public float progress(float now) {
		return sat(this.blastTicks(now) / (float) this.total());
	}

	public float expand(float now) {
		return sat(this.blastTicks(now) / 360.0F);
	}

	public float fade(float now) {
		float fadeLen = 95.99999F;
		float k = sat((this.blastTicks(now) - (384.0F + (float) this.hold)) / fadeLen);
		return 1.0F - k * k * (3.0F - 2.0F * k);
	}

	public float radius(float now) {
		if (!this.isBlasting(now))
			return 0.0F;
		float q = 1.0F - this.expand(now);
		return Mth.lerp(1.0F - q * q, Math.min(COUNTDOWN_RADIUS, this.maxRadius), this.maxRadius);
	}

	public float shockRadius(float now) {
		if (!this.isBlasting(now))
			return 0.0F;
		float q = 1.0F - sat(this.blastTicks(now) / 300.0F);
		return Math.max(this.radius(now), this.maxRadius * SHOCK_FACTOR * (1.0F - q * q));
	}

	public float shockMax() {
		return this.maxRadius * SHOCK_FACTOR;
	}

	public float starRadius(float now) {
		float idle = IDLE_MIN_RADIUS + 8.0F * (1.0F - (float) Math.exp((double) (-this.idleAge(now) / IDLE_GROW_TICKS)));
		if (!this.isDetonating())
			return idle;
		float k = smooth(this.charge(now));
		return Mth.lerp(k, idle, Math.min(COUNTDOWN_RADIUS, idle));
	}

	public float breath(float now) {
		float secs = now / 20.0F;
		if (!this.isDetonating() || this.isBlasting(now))
			return 1.0F + 0.05F * Mth.sin(secs * 6.2831855F / 3.5F);
		float T = 30.0F;
		float t = this.countTicks(now) / 20.0F;
		float f0 = 0.2857143F;
		float f1 = 3.125F;
		float fase = f0 * t + (f1 - f0) * t * t * t / (3.0F * T * T);
		float p = Math.min(1.0F, t / T);
		float amp = 0.05F + 0.05F * p;
		float encoge = 1.0F - 0.22F * smooth(p);
		return encoge * (1.0F + amp * Mth.sin(fase * 6.2831855F));
	}

	public float activation(float now) {
		return this.isDetonating() ? sat(this.countTicks(now) / 210.0F) : 0.0F;
	}

	public boolean expired(float now) {
		return this.isDetonating() && this.countTicks(now) > (float) (COUNTDOWN + this.total() + 60);
	}
}
