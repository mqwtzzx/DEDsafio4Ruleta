package net.ruleta.agujero;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ruleta.Ruleta;

/**
 * Agujero negro de Reviil: atrae a los jugadores a 100 bloques y daña a los que entran.
 * (En Forge, getAddEntityPacket usaba NetworkHooks; en Fabric basta el paquete de aparición de vanilla.)
 */
public class AgujeroNegroEntity extends Entity {
	public static final float RADIO_DEFECTO = 15.0F;
	public static final float RADIO_MIN = 1.0F;
	public static final float RADIO_MAX = 64.0F;
	private static final EntityDataAccessor<Float> RADIO = SynchedEntityData.defineId(AgujeroNegroEntity.class, EntityDataSerializers.FLOAT);
	private static final double VELOCIDAD_MAX = 1.4D;
	private static final float DANO = 5.0F;

	public AgujeroNegroEntity(EntityType<?> tipo, Level nivel) {
		super(tipo, nivel);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		datos.define(RADIO, RADIO_DEFECTO);
	}

	public double radio() {
		return this.entityData.get(RADIO);
	}

	public void fijarRadio(float r) {
		this.entityData.set(RADIO, Mth.clamp(r, RADIO_MIN, RADIO_MAX));
	}

	private double dentro() {
		return this.radio() + 1.5D;
	}

	private double alcance() {
		return 100.0D;
	}

	@Override
	public void tick() {
		super.tick();
		this.setDeltaMovement(Vec3.ZERO);
		if (!(this.level() instanceof ServerLevel mundo))
			return;
		if (!ComandoAgujero.vivo(this.getUUID())) {
			this.discard();
			return;
		}
		for (ServerPlayer jugador : mundo.players())
			if (!jugador.isSpectator() && !jugador.isCreative() && jugador.isAlive())
				this.atraer(jugador);
	}

	private void atraer(ServerPlayer jugador) {
		Vec3 hacia = this.position().subtract(jugador.position().add(0.0D, (double) jugador.getBbHeight() * 0.5D, 0.0D));
		double d = hacia.length();
		double alcance = this.alcance();
		if (d > alcance || d < 0.001D)
			return;
		Vec3 dir = hacia.scale(1.0D / d);
		Vec3 v = jugador.getDeltaMovement();
		if (d < this.dentro()) {
			v = v.scale(0.6D).add(dir.scale(0.08D));
			if (this.tickCount % 20 == 0) {
				jugador.invulnerableTime = 0;
				jugador.hurt(Ruleta.fuenteLetalPublica(jugador, "ruleta.agujero", " fue absorbido por un Agujero Negro de Reviil"), DANO);
			}
		} else {
			double k = 1.0D - d / alcance;
			v = v.add(dir.scale(0.03D + 0.17D * Math.pow(k, 1.3D)));
		}
		if (v.length() > VELOCIDAD_MAX)
			v = v.normalize().scale(VELOCIDAD_MAX);
		jugador.setDeltaMovement(v);
		jugador.hurtMarked = true;
		jugador.fallDistance = 0.0F;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distancia) {
		return true;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.contains("Radio"))
			this.fijarRadio(tag.getFloat("Radio"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putFloat("Radio", (float) this.radio());
	}
}
