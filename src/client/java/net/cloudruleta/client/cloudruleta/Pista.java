package net.cloudruleta.client.cloudruleta;

/** Curva por tramos (t, valor) con interpolación suave. */
final class Pista {
	private final float[] t;
	private final float[] v;

	Pista(float[] puntos) {
		int n = puntos.length / 2;
		this.t = new float[n];
		this.v = new float[n];
		for (int i = 0; i < n; i++) {
			this.t[i] = puntos[2 * i];
			this.v[i] = puntos[2 * i + 1];
		}
	}

	float en(float x) {
		int n = this.t.length;
		if (x <= this.t[0])
			return this.v[0];
		if (x >= this.t[n - 1])
			return this.v[n - 1];
		int i = 1;
		while (this.t[i] < x)
			i++;
		float u = (x - this.t[i - 1]) / (this.t[i] - this.t[i - 1]);
		return this.v[i - 1] + (this.v[i] - this.v[i - 1]) * Mates.suave(u);
	}
}
