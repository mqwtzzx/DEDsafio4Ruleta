package net.ruleta.compat.client;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.ruleta.compat.Event;

/**
 * PlaySoundEvent de Forge. Lo publica mixin.client.SoundEngineMixin al principio de SoundEngine.play (si el motor está
 * cargado), como Forge: si un manejador pone el sonido a null, no suena.
 */
public final class PlaySoundEvent extends Event {
	private final SoundEngine engine;
	private final SoundInstance original;
	@Nullable
	private SoundInstance sound;

	public PlaySoundEvent(SoundEngine engine, SoundInstance sound) {
		this.engine = engine;
		this.original = sound;
		this.sound = sound;
	}

	public SoundEngine getEngine() {
		return this.engine;
	}

	public String getName() {
		return this.original.getLocation().getPath();
	}

	public SoundInstance getOriginalSound() {
		return this.original;
	}

	@Nullable
	public SoundInstance getSound() {
		return this.sound;
	}

	public void setSound(@Nullable SoundInstance newSound) {
		this.sound = newSound;
	}
}
