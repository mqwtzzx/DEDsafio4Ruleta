package net.cloudruleta.client.reviil;

import java.util.function.Supplier;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Sonido sin posición cuyo volumen y tono se leen cada tick ({volumen, tono}; null = parar). */
final class SonidoReviil extends AbstractTickableSoundInstance {
	private final Supplier<float[]> estado;

	SonidoReviil(SoundEvent evento, boolean repetir, Supplier<float[]> estado) {
		super(evento, SoundSource.MASTER, RandomSource.create());
		this.estado = estado;
		this.looping = repetir;
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
		this.aplicar();
	}

	@Override
	public void tick() {
		this.aplicar();
	}

	private void aplicar() {
		float[] e = this.estado.get();
		if (e == null) {
			this.stop();
			return;
		}
		this.volume = Math.max(1.0E-4F, e[0]);
		this.pitch = e[1];
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}
}
