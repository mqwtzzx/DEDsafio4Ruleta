package net.ruleta.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.ruleta.compat.Bus;
import net.ruleta.compat.client.PlaySoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * PlaySoundEvent de Forge 1.20.1: en SoundEngine.play, si el motor está cargado, el sonido pasa por el evento; si el
 * evento lo deja en null no suena.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Shadow
	private boolean loaded;

	@WrapMethod(method = "play")
	private void ruleta$alSonar(SoundInstance sonido, Operation<Void> original) {
		if (this.loaded && Bus.hasListeners(PlaySoundEvent.class)) {
			PlaySoundEvent e = new PlaySoundEvent((SoundEngine) (Object) this, sonido);
			Bus.post(e);
			sonido = e.getSound();
			if (sonido == null)
				return;
		}
		original.call(sonido);
	}
}
