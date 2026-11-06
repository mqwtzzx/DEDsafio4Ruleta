package net.ruleta.client.ruleta;

import net.ruleta.RuletaColor;
import net.ruleta.RuletaPayload;

/** Una tirada de la ruleta: todas las curvas de la animación en función del tiempo. */
final class Tirada {
	static final float DESFASE = 0.12F;
	static final float FIN = 13.93F;
	private static final float ASIENTO = 5.0F;
	private static final Pista VELOCIDAD = new Pista(new float[]{
			0.62F, 0.0F, 0.85F, 250.0F, 1.1F, 480.0F, 1.5F, 600.0F, 2.0F, 760.0F, 2.5F, 930.0F, 3.0F, 1150.0F, 3.5F, 1500.0F,
			4.0F, 1850.0F, 4.3F, 1950.0F, 4.7F, 1700.0F, 5.2F, 1450.0F, 5.7F, 1100.0F, 6.2F, 780.0F, 6.7F, 600.0F, 7.2F, 500.0F,
			7.7F, 420.0F, 8.2F, 330.0F, 8.6F, 170.0F, 9.0F, 55.0F, 9.3F, 8.0F, 9.4F, 0.0F});
	private static final Pista ESCALA = new Pista(new float[]{
			0.12F, 0.0F, 0.133F, 0.69F, 0.167F, 1.31F, 0.25F, 1.4F, 0.33F, 1.4F, 0.37F, 1.31F, 0.43F, 0.9F, 0.53F, 0.9F,
			0.58F, 0.94F, 0.65F, 1.04F, 0.73F, 1.03F, 0.82F, 1.01F, 0.95F, 1.0F, 10.17F, 1.0F, 10.2F, 1.07F, 10.27F, 1.03F,
			10.33F, 1.02F, 10.4F, 1.03F, 10.47F, 1.05F, 10.53F, 1.03F, 10.58F, 0.985F, 10.7F, 0.99F, 10.8F, 1.0F, 13.07F, 1.0F,
			13.13F, 0.99F, 13.17F, 0.9F, 13.2F, 0.79F, 13.23F, 0.72F, 13.27F, 0.67F, 13.37F, 0.68F, 13.4F, 0.71F, 13.43F, 0.77F,
			13.47F, 0.94F, 13.5F, 1.15F, 13.53F, 1.23F, 13.57F, 1.29F, 13.63F, 1.34F, 13.73F, 1.34F, 13.77F, 1.45F, 13.8F, 1.16F,
			13.87F, 0.64F, 13.93F, 0.3F});
	private static final Pista BLANCO = new Pista(new float[]{
			0.12F, 1.0F, 0.37F, 0.95F, 0.43F, 0.8F, 0.5F, 0.62F, 0.57F, 0.43F, 0.63F, 0.3F, 0.73F, 0.2F, 0.8F, 0.05F,
			0.87F, 0.0F, 10.18F, 0.0F, 10.2F, 1.0F, 10.28F, 1.0F, 10.32F, 0.8F, 10.37F, 0.55F, 10.45F, 0.3F, 10.53F, 0.12F,
			10.62F, 0.0F, 13.07F, 0.0F, 13.25F, 0.12F, 13.4F, 0.25F, 13.55F, 0.45F, 13.62F, 0.65F, 13.67F, 0.92F, 13.7F, 1.0F});
	private static final Pista RESPLANDOR = new Pista(new float[]{
			10.19F, 0.0F, 10.22F, 0.55F, 10.36F, 0.0F, 13.37F, 0.0F, 13.45F, 0.9F, 13.75F, 0.9F, 13.86F, 0.0F});
	private static final Pista PUNTERO_ESCALA = new Pista(new float[]{0.36F, 0.0F, 0.4F, 1.15F, 0.47F, 0.9F, 0.55F, 1.05F, 0.65F, 1.0F});
	private static final Pista PUNTERO_BLANCO = new Pista(new float[]{0.36F, 1.0F, 0.52F, 1.0F, 0.6F, 0.0F, 10.1F, 0.0F, 10.13F, 1.0F, 10.2F, 1.0F, 10.27F, 0.0F});
	private static final Pista PUNTERO_SUBE = new Pista(new float[]{13.25F, 0.0F, 13.33F, 35.0F, 13.43F, 90.0F, 13.5F, 45.0F, 13.6F, 0.0F});
	private static final Pista PUNTERO_ALFA = new Pista(new float[]{13.62F, 1.0F, 13.7F, 0.0F});
	private static final float[] RELLENO = {10.93F, 11.07F, 11.2F, 11.27F, 11.33F, 11.5F, 11.63F};
	private static final Pista[][] TONO = {
			tono(new float[]{0.0F, -15.0F, 0.06F, 1.0F, 0.133F, -6.0F, 0.02F, 1.0F, 0.2F, -3.0F, 0.0F, 1.0F, 0.5F, 0.0F, 0.0F, 1.0F}),
			tono(new float[]{0.0F, -20.7F, -0.043F, 1.02F, 0.067F, -14.4F, -0.049F, 1.01F, 0.133F, -4.6F, -0.067F, 0.97F, 0.5F, 0.0F, 0.0F, 1.0F}),
			tono(new float[]{0.0F, -44.7F, -0.08F, 0.81F, 0.067F, -11.0F, -0.051F, 1.02F, 0.1F, -5.6F, -0.051F, 1.0F, 0.167F, -3.9F, -0.053F, 1.0F,
					0.5F, 0.0F, 0.0F, 1.0F}),
			tono(new float[]{0.0F, -5.3F, 0.171F, 1.01F, 0.067F, -5.9F, 0.124F, 1.01F, 0.133F, -5.9F, 0.071F, 1.01F, 0.2F, -6.0F, 0.043F, 1.02F,
					0.3F, -6.0F, 0.031F, 1.01F, 0.5F, 0.0F, 0.0F, 1.0F}),
			tono(new float[]{0.0F, -23.6F, 0.096F, 1.0F, 0.067F, -19.1F, 0.075F, 1.0F, 0.133F, -11.8F, 0.025F, 0.97F, 0.2F, -5.3F, -0.022F, 0.93F,
					0.367F, -4.9F, -0.035F, 0.88F, 0.5F, 0.0F, 0.0F, 1.0F}),
			tono(new float[]{0.0F, -21.9F, 0.029F, 1.01F, 0.067F, -14.5F, 0.006F, 1.01F, 0.133F, -9.0F, -0.018F, 0.99F, 0.2F, -5.2F, -0.059F, 0.89F,
					0.5F, 0.0F, 0.0F, 1.0F}),
			tono(new float[]{0.0F, -10.8F, 0.278F, 0.96F, 0.067F, -10.8F, 0.388F, 0.88F, 0.133F, -4.9F, 0.251F, 0.99F, 0.2F, -1.5F, 0.165F, 0.95F,
					0.5F, 0.0F, 0.0F, 1.0F}),
			tono(new float[]{0.0F, -11.3F, 0.261F, 1.01F, 0.067F, -2.8F, 0.155F, 1.02F, 0.1F, 0.7F, 0.094F, 1.02F, 0.2F, 1.6F, 0.071F, 1.01F,
					0.5F, 0.0F, 0.0F, 1.0F})
	};
	private static final float GEMA_VUELTA = 0.7F;
	private static final float GEMA_INICIO = 9.57F;
	private static final float GEMA_FLUJO = 0.9F;
	private static final float PASO = 0.002F;
	private static final float INICIO_GIRO = 0.62F;
	private static final float FIN_GIRO = 9.4F;
	private static final float[] AVANCE;
	private static final float RECORRIDO_TIPICO;

	static {
		// (FIN_GIRO - INICIO_GIRO) / PASO calculado en float por el compilador del original: 4389.99951171875
		int n = (int) Math.ceil(4389.99951171875D) + 2;
		float[] acum = new float[n];
		float prev = 0.0F;
		for (int i = 1; i < n; i++) {
			float v = VELOCIDAD.en(INICIO_GIRO + (float) i * PASO);
			acum[i] = acum[i - 1] + (prev + v) * 0.5F * PASO;
			prev = v;
		}
		RECORRIDO_TIPICO = acum[n - 1];
		AVANCE = new float[n];
		for (int i = 0; i < n; i++)
			AVANCE[i] = acum[i] / RECORRIDO_TIPICO;
	}

	final RuletaColor resultado;
	final boolean mini;
	private final float angulo0;
	private final float recorrido;
	private final long nacio = System.nanoTime();
	private float punteroAng;
	private float punteroVel;
	private float simulado = -1.0F;
	private int ultimoSeparador;

	Tirada(RuletaPayload p) {
		this.resultado = RuletaColor.byIndex(p.result());
		this.mini = p.mini();
		this.angulo0 = p.startAngle();
		float parada = (float) (-this.resultado.ordinal()) * 45.0F + ASIENTO;
		float falta = Mates.modulo(parada - (this.angulo0 + RECORRIDO_TIPICO) + 180.0F, 360.0F) - 180.0F;
		this.recorrido = RECORRIDO_TIPICO + falta;
		this.ultimoSeparador = this.separador(this.angulo(INICIO_GIRO));
	}

	float tiempo() {
		return (float) (System.nanoTime() - this.nacio) / 1.0E9F + DESFASE;
	}

	boolean terminada() {
		return this.tiempo() > FIN;
	}

	float angulo(float t) {
		float u = Mates.limitar((t - INICIO_GIRO) / PASO, 0.0F, (float) AVANCE.length - 1.001F);
		int i = (int) u;
		float avance = AVANCE[i] + (AVANCE[i + 1] - AVANCE[i]) * (u - (float) i);
		return this.angulo0 + this.recorrido * avance - ASIENTO * Mates.entre(t, 9.35F, 9.95F);
	}

	float velocidad(float t) {
		return VELOCIDAD.en(t) * this.recorrido / RECORRIDO_TIPICO;
	}

	float desenfoque(float t) {
		return Mates.limitar(Math.abs(this.velocidad(t)) * 0.016F, 0.0F, 30.0F);
	}

	float escala(float t) {
		float s = ESCALA.en(t);
		for (float r : RELLENO) {
			float u = (t - r) / 0.08F;
			if (u > 0.0F && u < 1.0F)
				s += 0.04F * (float) Math.sin(Math.PI * (double) u);
		}
		return s;
	}

	float blanco(float t) {
		return BLANCO.en(t);
	}

	float resplandor(float t) {
		return RESPLANDOR.en(t);
	}

	float resplandorEscala(float t) {
		return t < 12.0F ? 0.75F : 1.35F;
	}

	float alfa(float t) {
		return 1.0F - Mates.entre(t, 13.88F, FIN);
	}

	float relleno(int i, float t) {
		int d = Math.floorMod(i - this.resultado.ordinal(), 8);
		return d == 0 ? 0.0F : Mates.entre(t, RELLENO[d - 1], RELLENO[d - 1] + 0.04F);
	}

	float[] tono(int i, float t) {
		int d = Math.floorMod(i - this.resultado.ordinal(), 8);
		if (d == 0)
			return null;
		float u = t - RELLENO[d - 1];
		if (u < 0.0F)
			return null;
		Pista[] p = TONO[Math.floorMod(i, 8)];
		return new float[]{p[0].en(u), p[1].en(u), p[2].en(u)};
	}

	private static Pista[] tono(float[] claves) {
		int n = claves.length / 4;
		float[] giro = new float[n * 2];
		float[] luz = new float[n * 2];
		float[] saturacion = new float[n * 2];
		for (int k = 0; k < n; k++) {
			float t = claves[4 * k];
			giro[2 * k] = t;
			giro[2 * k + 1] = claves[4 * k + 1];
			luz[2 * k] = t;
			luz[2 * k + 1] = claves[4 * k + 2];
			saturacion[2 * k] = t;
			saturacion[2 * k + 1] = claves[4 * k + 3];
		}
		return new Pista[]{new Pista(giro), new Pista(luz), new Pista(saturacion)};
	}

	float colorCasilla(int i, float t) {
		float ang = Mates.modulo(this.angulo(t) + (float) i * 45.0F, 360.0F);
		float orden = ang / 360.0F;
		return Mates.entre(t, 0.45F + orden * 0.25F, 0.62F + orden * 0.25F);
	}

	float baseBlanca(float t) {
		return t < DESFASE ? 0.0F : 1.0F - Mates.entre(t, 0.9F, 1.0F);
	}

	float linea(int k, float t) {
		float empieza = 0.17F + (float) k * 0.03F;
		return Mates.entre(t, empieza, empieza + 0.07F) * (1.0F - Mates.entre(t, 0.62F, 0.85F));
	}

	float aro(float t) {
		return Mates.entre(t, 0.58F, 0.78F);
	}

	float centro(float t) {
		return Mates.entre(t, 0.12F, 0.15F);
	}

	float punteroEscala(float t) {
		return PUNTERO_ESCALA.en(t);
	}

	float punteroBlanco(float t) {
		return PUNTERO_BLANCO.en(t);
	}

	float punteroSube(float t) {
		return PUNTERO_SUBE.en(t);
	}

	float punteroAlfa(float t) {
		return PUNTERO_ALFA.en(t);
	}

	private static float indiceGema(float t) {
		return GEMA_INICIO - GEMA_VUELTA * t;
	}

	private static int colorEn(float u) {
		int i = (int) Math.floor((double) u);
		return Mates.mezclar(RuletaColor.byIndex(i).rgb, RuletaColor.byIndex(i + 1).rgb, Mates.suave(u - (float) i));
	}

	int gema(float t) {
		return Mates.mezclar(colorEn(indiceGema(t)), this.resultado.rgb, this.revelada(t));
	}

	int gemaVeta(float t) {
		return colorEn(indiceGema(t) - 1.0F);
	}

	int gemaFranja(float t) {
		return Mates.mezclar(colorEn(indiceGema(t) + 1.0F), 16777215, 0.55F);
	}

	float veteado(float t) {
		return 1.0F - this.revelada(t);
	}

	float revelada(float t) {
		return Mates.entre(t, 10.13F, 10.2F);
	}

	static float vetaPeso(int k, float t) {
		float fase = Mates.modulo(t * GEMA_FLUJO, 3.0F);
		float d = Math.abs(Mates.modulo(fase - (float) k + 1.5F, 3.0F) - 1.5F);
		return Mates.suave(1.0F - d);
	}

	/** Muelle del puntero: se simula a pasos fijos y recibe un golpe cada vez que pasa un separador. */
	float puntero(float t) {
		if (this.simulado < 0.0F)
			this.simulado = INICIO_GIRO;
		if (t < this.simulado)
			return this.punteroAng;

		float h = 0.0020833334F;
		int pasos = 0;
		// en el original: pasos++ < 4000 (comprobado con javap)
		while (this.simulado + h <= t && pasos++ < 4000) {
			this.simulado += h;
			int s = this.separador(this.angulo(this.simulado));
			if (s != this.ultimoSeparador) {
				this.ultimoSeparador = s;
				this.punteroVel -= 135.0F + Math.abs(this.velocidad(this.simulado)) * 0.06F;
			}
			float acel = -490.0F * this.punteroAng - 10.5F * this.punteroVel;
			this.punteroVel += acel * h;
			this.punteroAng += this.punteroVel * h;
			if (this.punteroAng < -25.0F) {
				this.punteroAng = -25.0F;
				this.punteroVel = Math.max(0.0F, this.punteroVel);
			}
		}
		return this.punteroAng;
	}

	private int separador(float angulo) {
		return (int) Math.floor((double) ((angulo + 22.5F) / 45.0F));
	}
}
