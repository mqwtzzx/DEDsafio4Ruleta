package com.supernova.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.supernova.SupernovaMod;
import com.supernova.SupernovaState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.TickEvent;

public class SupernovaSounds {
	private static final Set<Integer> CHARGE = new HashSet<>();
	private static final float CARGA_TICKS = 400.0F;
	private static final Set<Integer> BLAST = new HashSet<>();
	private static final Set<Integer> WAVE = new HashSet<>();
	private static final Map<Integer, Integer> BEAT = new HashMap<>();
	private static final Map<Integer, Hum> HUMS = new HashMap<>();
	private static final Map<Integer, Musica> MUSICAS = new HashMap<>();

	private static float near(SupernovaState s, double d) {
		return 1.0F - SupernovaState.smooth((float) ((d - (double) s.maxRadius * 6.0D) / ((double) s.maxRadius * 6.0D)));
	}

	private static void play(SoundEvent ev, float vol, float pitch) {
		if (vol <= 0.01F)
			return;
		Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(ev.getLocation(), SoundSource.MASTER, Math.min(1.0F, vol), pitch, RandomSource.create(), false, 0,
				SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true));
	}

	@SubscribeEvent
	public static void onTick(TickEvent.ClientTickEvent e) {
		if (e.phase != TickEvent.Phase.END)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			clear();
			return;
		}

		float now = (float) mc.level.getGameTime();
		Vec3 eye = mc.player.getEyePosition();
		Set<Integer> seen = new HashSet<>();
		for (SupernovaState s : SupernovaGhosts.active(now)) {
			int id = s.id;
			seen.add(id);
			double d = eye.distanceTo(s.center());
			float near = near(s, d);
			if (!s.isBlasting(now) && d < 400.0D)
				particulas(mc, s, now);

			if (s.charge(now) <= 0.0F && near > 0.01F) {
				Hum h = HUMS.get(id);
				if (h == null || h.isStopped() || (now - h.startedAt > 40.0F && !mc.getSoundManager().isActive(h))) {
					h = new Hum(id, now);
					HUMS.put(id, h);
					mc.getSoundManager().play(h);
				}
			}

			if (s.isDetonating() && !s.isBlasting(now)) {
				float ct = s.countTicks(now) - CARGA_TICKS;
				float cc = s.countTicks(now) - 200.0F;
				if (cc >= 0.0F && CHARGE.add(id) && cc < 30.0F)
					play(SupernovaMod.SND_CHARGE.get(), 0.35F + 0.65F * near, 1.0F);
				int sec = (int) (ct / 20.0F);
				Integer last = BEAT.put(id, sec);
				if (ct >= 0.0F && last != null && last != sec) {
					float k = s.charge(now);
					play(SupernovaMod.SND_HEARTBEAT.get(), (0.45F + 0.55F * k) * (0.4F + 0.6F * near), 0.85F + 0.35F * k);
				}
			}

			if (s.isBlasting(now)) {
				float bt = s.blastTicks(now);
				if (BLAST.add(id) && bt < 40.0F) {
					play(SupernovaMod.SND_BLAST.get(), Math.max(near, 0.45F), 1.0F);
					boolean yaSuena = false;
					for (Musica otra : MUSICAS.values()) {
						if (!otra.isStopped() && otra.apagando < 0.0F) {
							yaSuena = true;
							break;
						}
					}
					if (!yaSuena) {
						Musica m = new Musica(id, Math.max(near, 0.7F));
						MUSICAS.put(id, m);
						mc.getSoundManager().play(m);
					}
				}

				if (!WAVE.contains(id)) {
					float r = s.shockRadius(now);
					float soon = s.shockRadius(now + 60.0F);
					if (d <= (double) r) {
						WAVE.add(id);
					} else if (d <= (double) soon) {
						WAVE.add(id);
						play(SupernovaMod.SND_WAVE.get(), 1.0F, 1.0F);
					}
				}
			}
		}

		CHARGE.retainAll(seen);
		BLAST.retainAll(seen);
		WAVE.retainAll(seen);
		BEAT.keySet().retainAll(seen);
		HUMS.keySet().retainAll(seen);
		MUSICAS.values().removeIf(AbstractTickableSoundInstance::isStopped);
	}

	private static void particulas(Minecraft mc, SupernovaState s, float now) {
		RandomSource r = mc.level.random;
		Vec3 c = s.center();
		float R = s.starRadius(now) * s.breath(now);
		float act = s.activation(now);
		int cuantas = act > 0.0F ? 3 + Math.round(6.0F * act) : 3;
		for (int i = 0; i < cuantas; i++) {
			double u = r.nextDouble() * 2.0D - 1.0D;
			double a = r.nextDouble() * Math.PI * 2.0D;
			double h = Math.sqrt(1.0D - u * u);
			Vec3 dir = new Vec3(h * Math.cos(a), u, h * Math.sin(a));
			double dist = (double) R * (1.3D + r.nextDouble() * 0.9D);
			Vec3 p = c.add(dir.scale(dist));
			Vec3 giro = new Vec3(-dir.z, 0.0D, dir.x);
			if (giro.lengthSqr() < 1.0E-4D)
				giro = new Vec3(1.0D, 0.0D, 0.0D);
			giro = giro.normalize();
			boolean blanca = act > 0.0F && r.nextFloat() < 0.35F + 0.65F * act;
			double vel = blanca ? 0.25D + 0.35D * (double) act : 0.12D;
			Vec3 v = giro.scale(vel).add(dir.scale(blanca ? -0.18D - 0.3D * (double) act : 0.05D));
			mc.level.addAlwaysVisibleParticle(blanca ? ParticleTypes.END_ROD : ParticleTypes.FLAME, true, p.x, p.y, p.z, v.x, v.y, v.z);
		}
	}

	private static void clear() {
		for (Musica m : MUSICAS.values())
			Minecraft.getInstance().getSoundManager().stop(m);
		MUSICAS.clear();
		CHARGE.clear();
		BLAST.clear();
		WAVE.clear();
		BEAT.clear();
		HUMS.clear();
	}

	private static class Musica extends AbstractTickableSoundInstance {
		private static final float DESVANECE = 60.0F;
		private final int id;
		private float base;
		private float apagando = -1.0F;

		Musica(int id, float volumen) {
			super(SupernovaMod.SND_MUSIC.get(), SoundSource.MASTER, RandomSource.create());
			this.id = id;
			this.base = Math.min(1.0F, volumen);
			this.looping = true;
			this.delay = 0;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.relative = true;
			this.x = 0.0D;
			this.y = 0.0D;
			this.z = 0.0D;
			this.volume = this.base;
			this.pitch = 1.0F;
		}

		@Override
		public void tick() {
			Minecraft mc = Minecraft.getInstance();
			SupernovaState s = SupernovaGhosts.get(this.id);
			boolean sigue = s != null && mc.level != null && s.dim.equals(mc.level.dimension().location().toString()) && !s.expired((float) mc.level.getGameTime());
			if (sigue) {
				float f = s.fade((float) mc.level.getGameTime());
				if (f <= 0.01F)
					sigue = false;
				else if (this.apagando < 0.0F)
					this.volume = Math.max(0.001F, this.base * f);
			}
			if (!sigue && this.apagando < 0.0F) {
				this.apagando = 0.0F;
				this.base = Math.max(0.001F, this.volume);
			}
			if (this.apagando >= 0.0F) {
				this.apagando++;
				float k = 1.0F - this.apagando / DESVANECE;
				if (k <= 0.0F) {
					this.volume = 0.0F;
					this.stop();
					return;
				}
				this.volume = Math.max(0.001F, this.base * k * k);
			}
		}
	}

	private static class Hum extends AbstractTickableSoundInstance {
		private final int id;
		final float startedAt;

		Hum(int id, float startedAt) {
			super(SupernovaMod.SND_IDLE.get(), SoundSource.MASTER, RandomSource.create());
			this.id = id;
			this.startedAt = startedAt;
			this.looping = true;
			this.delay = 0;
			this.attenuation = SoundInstance.Attenuation.NONE;
			this.relative = true;
			this.x = 0.0D;
			this.y = 0.0D;
			this.z = 0.0D;
			this.volume = 0.01F;
			this.pitch = 1.0F;
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		@Override
		public void tick() {
			Minecraft mc = Minecraft.getInstance();
			SupernovaState s = SupernovaGhosts.get(this.id);
			if (s == null || mc.level == null || mc.player == null || s.charge((float) mc.level.getGameTime()) > 0.0F
					|| !s.dim.equals(mc.level.dimension().location().toString())) {
				this.stop();
				return;
			}
			double d = mc.player.getEyePosition().distanceTo(s.center());
			float close = (float) Math.max(0.0D, 1.0D - d / ((double) s.maxRadius * 3.0D));
			float target = 0.12F * near(s, d) + 0.6F * close * close;
			float fadeIn = SupernovaState.smooth(s.age((float) mc.level.getGameTime()) / 60.0F);
			this.volume = Math.max(0.001F, target * fadeIn);
		}
	}
}
