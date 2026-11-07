package net.cloudruleta.herobrine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.Ruleta;
import net.cloudruleta.bunker.Bunkers;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/** Herobrine: quieto hasta que alguien le mira a los ojos; entonces lo persigue y teletransporta tras él. */
public class HerobrineEntity extends Monster {
	private static final float DANO = 4.0F;
	@Nullable
	private UUID objetivo;
	private int proximoTp = 200;
	private int proximoGiro;
	private float yawQuieto;
	private float pitchQuieto;

	public HerobrineEntity(EntityType<? extends Monster> tipo, Level nivel) {
		super(tipo, nivel);
		// setMaxUpStep(1.0F) de 1.20.1: en 1.21.1 la altura de paso es el atributo STEP_HEIGHT
		AttributeInstance paso = this.getAttribute(Attributes.STEP_HEIGHT);
		if (paso != null)
			paso.setBaseValue(1.0D);
		this.xpReward = 20;
	}

	public static AttributeSupplier.Builder atributos() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60.0D).add(Attributes.MOVEMENT_SPEED, 0.32D).add(Attributes.ATTACK_DAMAGE, 4.0D)
				.add(Attributes.FOLLOW_RANGE, 96.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.45D, true));
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (!(this.level() instanceof ServerLevel nivel))
			return;
		ServerPlayer presa = this.objetivo == null ? null : (ServerPlayer) nivel.getPlayerByUUID(this.objetivo);
		if (this.objetivo != null && (presa == null || !presa.isAlive() || presa.isSpectator() || presa.isCreative())) {
			this.desaparecer();
			return;
		}
		if (presa == null)
			this.quieto(nivel);
		else
			this.cazar(nivel, presa);
	}

	private void quieto(ServerLevel nivel) {
		this.setSprinting(false);
		this.getNavigation().stop();
		this.setTarget(null);
		for (ServerPlayer p : this.tickCount % 2 == 0 ? nivel.players() : List.<ServerPlayer>of()) {
			if (p.isAlive() && !p.isSpectator() && !p.isCreative() && this.distanceToSqr(p) < 4096.0D && this.meMiraALosOjos(p)) {
				this.elegir(p);
				return;
			}
		}
		if (--this.proximoGiro <= 0) {
			this.proximoGiro = 40 + this.random.nextInt(120);
			this.yawQuieto = this.random.nextFloat() * 360.0F;
			this.pitchQuieto = (this.random.nextFloat() - 0.5F) * 40.0F;
		}
		this.setYRot(this.yawQuieto);
		this.yBodyRot = this.yawQuieto;
		this.yHeadRot = this.yawQuieto;
		this.setXRot(this.pitchQuieto);
		if (--this.proximoTp <= 0) {
			this.proximoTp = 300 + this.random.nextInt(600);
			Player cerca = nivel.getNearestPlayer(this, 128.0D);
			if (cerca != null && !cerca.isSpectator())
				this.tpCercaDe(cerca, 10.0D, 24.0D);
			else
				this.tpAlAzar(16.0D);
		}
	}

	private void cazar(ServerLevel nivel, ServerPlayer presa) {
		if (this.getTarget() != presa)
			this.setTarget(presa);
		this.setSprinting(true);
		if (--this.proximoTp <= 0) {
			this.proximoTp = 60;
			double d = this.distanceToSqr(presa);
			if (d > 576.0D || d > 36.0D && this.getNavigation().isDone())
				this.tpCercaDe(presa, 3.0D, 7.0D);
		}
	}

	public boolean meMiraALosOjos(Player p) {
		Vec3 vista = p.getViewVector(1.0F).normalize();
		Vec3 hacia = new Vec3(this.getX() - p.getX(), this.getEyeY() - p.getEyeY(), this.getZ() - p.getZ());
		double d = hacia.length();
		hacia = hacia.normalize();
		return vista.dot(hacia) > 1.0D - 0.025D / d && p.hasLineOfSight(this);
	}

	private void elegir(ServerPlayer p) {
		this.objetivo = p.getUUID();
		this.setTarget(p);
		this.setPersistenceRequired();
		this.proximoTp = 60;
		this.playSound(SoundEvents.ENDERMAN_SCREAM, 1.0F, 0.6F);
	}

	@Override
	public void setTarget(@Nullable LivingEntity nuevo) {
		if (this.objetivo != null && (nuevo == null || !this.objetivo.equals(nuevo.getUUID()))
				&& this.level() instanceof ServerLevel nivel && nivel.getPlayerByUUID(this.objetivo) instanceof ServerPlayer presa && presa.isAlive()) {
			super.setTarget(presa);
			return;
		}
		super.setTarget(nuevo);
	}

	@Override
	public boolean hurt(DamageSource fuente, float cantidad) {
		boolean r = super.hurt(fuente, cantidad);
		if (r && this.objetivo == null && fuente.getEntity() instanceof ServerPlayer p && !p.isCreative())
			this.elegir(p);
		return r;
	}

	@Override
	public boolean doHurtTarget(Entity victima) {
		if (victima instanceof ServerPlayer p) {
			this.swing(InteractionHand.MAIN_HAND);
			return p.hurt(Ruleta.fuenteLetalPublica(p, "cloudruleta.herobrine", " fue asesinado por Herobrine"), DANO);
		}
		return super.doHurtTarget(victima);
	}

	private boolean tpCercaDe(Entity e, double min, double max) {
		for (int i = 0; i < 24; i++) {
			double ang = this.random.nextDouble() * Math.PI * 2.0D;
			double r = min + this.random.nextDouble() * (max - min);
			double x = e.getX() + Math.cos(ang) * r;
			double z = e.getZ() + Math.sin(ang) * r;
			double y = e.getY() + (double) (this.random.nextInt(9) - 4);
			if (this.teletransportar(x, y, z))
				return true;
		}
		return false;
	}

	private boolean tpAlAzar(double alcance) {
		for (int i = 0; i < 16; i++) {
			double x = this.getX() + (this.random.nextDouble() - 0.5D) * 2.0D * alcance;
			double y = this.getY() + (double) (this.random.nextInt(17) - 8);
			double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 2.0D * alcance;
			if (this.teletransportar(x, y, z))
				return true;
		}
		return false;
	}

	private boolean teletransportar(double x, double y, double z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
		while (pos.getY() > this.level().getMinBuildHeight() && !this.level().getBlockState(pos).blocksMotion())
			pos.move(0, -1, 0);
		if (!this.level().getBlockState(pos).blocksMotion() || this.level().getBlockState(pos.above()).liquid())
			return false;
		if (this.level() instanceof ServerLevel sl && Bunkers.interiorDe(sl, pos.above()) != null)
			return false;
		double ox = this.getX();
		double oy = this.getY();
		double oz = this.getZ();
		boolean ok = this.randomTeleport(x, pos.getY() + 1, z, true);
		if (ok) {
			this.level().playSound(null, ox, oy, oz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.5F);
			this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.5F);
		}
		return ok;
	}

	public void desaparecer() {
		if (this.level() instanceof ServerLevel nivel) {
			nivel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(), 30, 0.3D, 0.8D, 0.3D, 0.02D);
			nivel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.4F);
		}
		this.discard();
	}

	@Override
	public boolean removeWhenFarAway(double distancia) {
		return this.objetivo == null && distancia > 16384.0D;
	}

	@Override
	public boolean causeFallDamage(float distancia, float mult, DamageSource fuente) {
		return false;
	}

	@Nullable
	@Override
	protected SoundEvent getAmbientSound() {
		return null;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource fuente) {
		return SoundEvents.PLAYER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PLAYER_DEATH;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (this.objetivo != null)
			tag.putUUID("Objetivo", this.objetivo);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.objetivo = tag.hasUUID("Objetivo") ? tag.getUUID("Objetivo") : null;
	}

	@Override
	public int getMaxHeadYRot() {
		return this.objetivo == null ? 0 : super.getMaxHeadYRot();
	}

	static float limpiar(float a) {
		return Mth.wrapDegrees(a);
	}
}
