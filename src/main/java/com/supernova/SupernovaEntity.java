package com.supernova;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * La estrella. Cuenta atrás de 30 s al detonar y luego la explosión: dentro del radio mata (y rompe la armadura),
 * fuera, la onda expansiva quema, empuja y desgasta la armadura. (En Forge, getAddEntityPacket usaba NetworkHooks;
 * en Fabric basta el paquete de aparición de vanilla.)
 */
public class SupernovaEntity extends Entity {
	private static final int FIRE_SECONDS = 15;
	private static final float SHOCK_ARMOR_PERCENT_PER_SECOND = 0.025F;
	private static final float LETHAL_DAMAGE = 1000000.0F;
	private static final double MESSAGE_RANGE = 3000.0D;
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(SupernovaEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> BORN = SynchedEntityData.defineId(SupernovaEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DETONATED = SynchedEntityData.defineId(SupernovaEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HOLD = SynchedEntityData.defineId(SupernovaEntity.class, EntityDataSerializers.INT);
	private final Set<UUID> hit = new HashSet<>();
	private final Set<UUID> shockHit = new HashSet<>();
	private static final int SHOCK_FIRE_SECONDS = 5;
	private boolean blastStarted;
	private SupernovaRomper.Trabajo romper;

	public SupernovaEntity(EntityType<?> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		if (!level.isClientSide) {
			this.entityData.set(BORN, (int) level.getGameTime());
			this.entityData.set(MAX_RADIUS, SupernovaAjustes.limitarTamano(SupernovaAjustes.tamano));
			this.entityData.set(HOLD, SupernovaAjustes.limitarTicks(SupernovaAjustes.tiempoTicks));
		}
	}

	public float getMaxRadius() {
		return this.entityData.get(MAX_RADIUS);
	}

	public void setMaxRadius(float r) {
		this.entityData.set(MAX_RADIUS, SupernovaAjustes.limitarTamano(r));
	}

	public int getHoldTicks() {
		return this.entityData.get(HOLD);
	}

	public void setHoldTicks(int ticks) {
		this.entityData.set(HOLD, SupernovaAjustes.limitarTicks(ticks));
	}

	public boolean isDetonating() {
		return this.entityData.get(DETONATED) >= 0;
	}

	public int bornTick() {
		return this.entityData.get(BORN);
	}

	public int detonatedTick() {
		return this.entityData.get(DETONATED);
	}

	public SupernovaState state() {
		return new SupernovaState(this.getId(), this.level().dimension().location().toString(), this.getX(), this.getY(), this.getZ(), this.getMaxRadius(),
				this.bornTick(), this.detonatedTick(), this.getHoldTicks());
	}

	public boolean detonate() {
		if (this.level().isClientSide || this.isDetonating())
			return false;
		ServerLevel sl = (ServerLevel) this.level();
		this.entityData.set(DETONATED, (int) sl.getGameTime());
		this.countdownMessage(sl, 30);
		SupernovaNet.broadcast(this, false);
		return true;
	}

	private void tell(ServerLevel sl, Component msg) {
		for (ServerPlayer p : sl.players())
			if (p.distanceToSqr(this) < MESSAGE_RANGE * MESSAGE_RANGE)
				p.displayClientMessage(msg, true);
	}

	private void countdownMessage(ServerLevel sl, int seconds) {
		this.tell(sl, Component.literal("La estrella colapsará en " + seconds + " segundos. ¡Aléjate todo lo que puedas!").withStyle(ChatFormatting.WHITE));
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide)
			return;
		ServerLevel sl = (ServerLevel) this.level();
		if (!this.isDetonating())
			return;
		int c = (int) (sl.getGameTime() - (long) this.entityData.get(DETONATED));
		if (c < SupernovaState.COUNTDOWN) {
			int left = (SupernovaState.COUNTDOWN - c) / 20;
			if (c > 0 && c % 20 == 0 && left <= 10)
				this.countdownMessage(sl, left);
			return;
		}
		int b = c - SupernovaState.COUNTDOWN;
		if (!this.blastStarted) {
			this.blastStarted = true;
			this.tell(sl, Component.literal("¡Supernova! La onda expansiva se acerca.").withStyle(ChatFormatting.WHITE));
			if (SupernovaRomper.activo) {
				Vec3 centro = this.state().center();
				this.romper = SupernovaRomper.empezar(sl, centro.x, centro.z, this.state().maxRadius);
			}
		}
		if (this.romper != null)
			this.romper.limite = Math.max(this.romper.limite, this.state().radius((float) sl.getGameTime()));
		this.sweep(sl);
		if (b >= SupernovaState.BLAST + this.getHoldTicks()) {
			if (this.romper != null)
				this.romper.limite = this.state().maxRadius;
			this.discard();
		}
	}

	private static boolean afectable(LivingEntity e) {
		if (!e.isAlive())
			return false;
		if (e instanceof AbstractVillager)
			return false;
		if (e instanceof TamableAnimal t && t.isTame())
			return false;
		if (e instanceof AbstractHorse h && h.isTamed())
			return false;
		return !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
	}

	private DamageSource lethalSource(ServerLevel sl) {
		return new DamageSource(sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SupernovaMod.SUPERNOVA_DAMAGE), this);
	}

	private DamageSource shockSource(ServerLevel sl) {
		return new DamageSource(sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SupernovaMod.SUPERNOVA_DAMAGE), this) {
			@Override
			public String getMsgId() {
				return "supernova.onda";
			}

			@Override
			public Component getLocalizedDeathMessage(LivingEntity victima) {
				return Component.empty().append(victima.getDisplayName()).append(Component.literal(" fue alcanzado por la Onda Expansiva"));
			}
		};
	}

	private void sweep(ServerLevel sl) {
		SupernovaState s = this.state();
		float now = (float) sl.getGameTime();
		float r = s.radius(now);
		if (r <= 0.0F)
			return;
		Vec3 c = s.center();
		for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r)))
			if (afectable(e) && e.getBoundingBox().getCenter().distanceTo(c) <= (double) r)
				this.smite(sl, e, c, this.hit.add(e.getUUID()));
		float sr = s.shockRadius(now);
		if (sr <= r)
			return;
		boolean everySecond = sl.getGameTime() % 20L == 0L;
		for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(sr))) {
			if (!afectable(e))
				continue;
			double d = e.getBoundingBox().getCenter().distanceTo(c);
			if (d > (double) sr || d <= (double) r)
				continue;
			UUID id = e.getUUID();
			if (!this.hit.contains(id) && this.shockHit.add(id))
				this.shock(sl, e, c, (float) d, s);
			else if (everySecond)
				wearArmor(e);
		}
	}

	private void shock(ServerLevel sl, LivingEntity e, Vec3 center, float d, SupernovaState s) {
		float t = SupernovaState.sat((d - s.maxRadius) / Math.max(1.0F, s.shockMax() - s.maxRadius));
		e.igniteForSeconds(SHOCK_FIRE_SECONDS); // setSecondsOnFire en 1.20.1
		e.hurt(this.shockSource(sl), LETHAL_DAMAGE);
		if (e.isAlive()) {
			Vec3 dir = e.position().subtract(center);
			dir = dir.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 1.0D, 0.0D) : dir.multiply(1.0D, 0.0D, 1.0D).normalize();
			double k = 3.5D - 2.0D * (double) t;
			e.push(dir.x * k, 0.9D, dir.z * k);
			e.hurtMarked = true;
		}
	}

	private void smite(ServerLevel sl, LivingEntity e, Vec3 center, boolean first) {
		e.igniteForSeconds(FIRE_SECONDS);
		for (EquipmentSlot slot : ARMOR) {
			ItemStack st = e.getItemBySlot(slot);
			if (!st.isEmpty() && st.isDamageableItem()) {
				e.onEquippedItemBroken(st.getItem(), slot); // broadcastBreakEvent en 1.20.1
				e.setItemSlot(slot, ItemStack.EMPTY);
			}
		}
		e.hurt(this.lethalSource(sl), LETHAL_DAMAGE);
		if (first && e.isAlive()) {
			Vec3 dir = e.position().subtract(center);
			dir = dir.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 1.0D, 0.0D) : dir.normalize();
			e.push(dir.x * 3.0D, 1.0D, dir.z * 3.0D);
			e.hurtMarked = true;
		}
	}

	private static void wearArmor(LivingEntity e) {
		for (EquipmentSlot slot : ARMOR) {
			ItemStack st = e.getItemBySlot(slot);
			if (st.isEmpty() || !st.isDamageableItem())
				continue;
			int max = st.getMaxDamage();
			int loss = Math.max(1, Math.round((float) max * SHOCK_ARMOR_PERCENT_PER_SECOND));
			int now = st.getDamageValue() + loss;
			if (now >= max) {
				e.onEquippedItemBroken(st.getItem(), slot);
				e.setItemSlot(slot, ItemStack.EMPTY);
			} else {
				st.setDamageValue(now);
			}
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
		datos.define(MAX_RADIUS, SupernovaState.DEFAULT_RADIUS);
		datos.define(BORN, 0);
		datos.define(DETONATED, -1);
		datos.define(HOLD, 0);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.contains("MaxRadius"))
			this.setMaxRadius(tag.getFloat("MaxRadius"));
		if (tag.contains("HoldTicks"))
			this.setHoldTicks(tag.getInt("HoldTicks"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putFloat("MaxRadius", this.getMaxRadius());
		tag.putInt("HoldTicks", this.getHoldTicks());
	}
}
