package net.ruleta.client;

/** Fundido suave (0..1) que sube y baja con el tiempo real. */
final class Fundido {
	private final float subir;
	private final float bajar;
	private float k;
	private long ultimo;

	Fundido(float subir, float bajar) {
		this.subir = subir;
		this.bajar = bajar;
	}

	float avanzar(boolean visible) {
		return this.avanzar(visible, this.bajar);
	}

	float avanzar(boolean visible, float bajarAhora) {
		long ahora = System.nanoTime();
		float dt = (float) (ahora - this.ultimo) / 1.0E9F;
		if (this.ultimo == 0L || dt > 0.5F)
			dt = 0.0F;
		this.ultimo = ahora;
		this.k = visible ? Math.min(1.0F, this.k + dt / this.subir) : Math.max(0.0F, this.k - dt / bajarAhora);
		return this.k * this.k * (3.0F - 2.0F * this.k);
	}

	boolean apagado() {
		return this.k == 0.0F;
	}

	void cero() {
		this.k = 0.0F;
	}
}
