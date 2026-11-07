package net.cloudruleta.nuclear.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.cloudruleta.nuclear.NuclearCreeperMod;
import net.cloudruleta.nuclear.entity.NuclearCreeperEntity;

public class RadiationSoundInstance extends AbstractTickableSoundInstance {
	private final NuclearCreeperEntity creeper;

	public RadiationSoundInstance(NuclearCreeperEntity creeper) {
		super(NuclearCreeperMod.RADIATION_LOOP.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
		this.creeper = creeper;
		this.looping = true;
		this.delay = 0;
		this.volume = 1.0F;
		this.x = creeper.getX();
		this.y = creeper.getY() + 0.6D;
		this.z = creeper.getZ();
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (this.creeper.isRemoved() || !this.creeper.isAlive() || mc.player == null || this.creeper.distanceTo(mc.player) > 10.0F) {
			this.stop();
			return;
		}
		this.x = this.creeper.getX();
		this.y = this.creeper.getY() + 0.6D;
		this.z = this.creeper.getZ();
	}
}
