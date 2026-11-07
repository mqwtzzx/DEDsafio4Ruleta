package net.cloudruleta.phora;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.Red;
import net.cloudruleta.ReviilPayload;
import net.cloudruleta.Ruleta;

import java.util.List;
import java.util.Locale;

/** Meteorito que cae en diagonal, explota, abre un cráter, empuja y quema lo que haya cerca. */
public class MeteoritoEntity extends Entity {
	private static final float POTENCIA = 10.0F;
	private static final double RADIO_ONDA = 48.0D;
	private static final double RADIO_DANO = 26.0D;
	private static final float DANO_EXTRA = 30.0F;
	private static final float DESGASTE = 0.45F;
	private static final int VIDA_MAX = 600;
	private int edad;
	private float tamano = 1.0F;

	static float tamanoAlAzar(RandomSource r) {
		float x = r.nextFloat();
		return x < 0.45F ? 0.4F + r.nextFloat() * 0.2F : x < 0.8F ? 0.9F + r.nextFloat() * 0.3F : 2.0F + r.nextFloat() * 0.5F;
	}

	public MeteoritoEntity(EntityType<? extends MeteoritoEntity> tipo, Level nivel) {
		super(tipo, nivel);
		this.noPhysics = true;
	}

	public static MeteoritoEntity lanzar(ServerLevel nivel, Vec3 objetivo, RandomSource r) {
		return lanzar(nivel, objetivo, r, null);
	}

	public static MeteoritoEntity lanzar(ServerLevel nivel, Vec3 objetivo, RandomSource r, Vec3 cerca) {
		MeteoritoEntity m = new MeteoritoEntity(PhoraMod.METEORITO.get(), nivel);
		double az = r.nextDouble() * Math.PI * 2.0D;
		if (cerca != null) {
			double hacia = Math.atan2(cerca.z - objetivo.z, cerca.x - objetivo.x);
			az = hacia + (r.nextDouble() - 0.5D) * Math.toRadians(80.0D);
		}
		double alto = 230.0D + r.nextDouble() * 70.0D;
		double lado = 75.0D + r.nextDouble() * 30.0D;
		Vec3 desde = objetivo.add(Math.cos(az) * lado, alto, Math.sin(az) * lado);
		double velocidad = 3.8D + r.nextDouble() * 1.0D;
		Vec3 v = objetivo.subtract(desde).normalize().scale(velocidad);
		m.moveTo(desde.x, desde.y, desde.z, 0.0F, 0.0F);
		m.setDeltaMovement(v);
		m.tamano = tamanoAlAzar(r);
		nivel.addFreshEntity(m);
		return m;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder datos) {
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag t) {
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag t) {
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double d) {
		return true;
	}

	@Override
	public boolean isOnFire() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		this.edad++;
		Vec3 v = this.getDeltaMovement();
		Vec3 p = this.position();
		Vec3 sig = p.add(v);
		Level nivel = this.level();
		if (!nivel.isClientSide) {
			if (this.edad > VIDA_MAX || p.y < (double) (nivel.getMinBuildHeight() - 16)) {
				this.discard();
				return;
			}
			BlockHitResult hit = nivel.clip(new ClipContext(p, sig, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
			Vec3 donde = hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
			if (donde == null) {
				AABB caja = this.getBoundingBox().expandTowards(v).inflate(0.6D);
				for (Entity e : nivel.getEntities(this, caja, e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
					donde = e.position().add(0.0D, (double) e.getBbHeight() * 0.5D, 0.0D);
					break;
				}
			}
			if (donde != null) {
				this.reventar((ServerLevel) nivel, donde);
				return;
			}
		} else {
			for (int i = 0; i < 3; i++) {
				double k = this.random.nextDouble();
				nivel.addAlwaysVisibleParticle(i == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, true,
						p.x - v.x * k + (this.random.nextDouble() - 0.5D) * 1.2D, p.y - v.y * k + (this.random.nextDouble() - 0.5D) * 1.2D,
						p.z - v.z * k + (this.random.nextDouble() - 0.5D) * 1.2D, -v.x * 0.05D, -v.y * 0.05D + 0.02D, -v.z * 0.05D);
			}
		}
		this.setPos(sig.x, sig.y, sig.z);
	}

	private void reventar(ServerLevel nivel, Vec3 c) {
		this.discard();
		float t = this.tamano;
		double radioOnda = RADIO_ONDA * Math.sqrt(t);
		double radioDano = RADIO_DANO * Math.sqrt(t);
		nivel.explode(this, c.x, c.y, c.z, POTENCIA * Math.min(1.2F, 0.6F + 0.4F * t), true, Level.ExplosionInteraction.TNT);
		Crater.abrir(nivel, BlockPos.containing(c), this.random, t);
		for (Entity e : nivel.getEntities(this, new AABB(c, c).inflate(radioOnda), x -> x.isAlive() && !x.isSpectator())) {
			Vec3 d = e.position().subtract(c);
			double dist = d.length();
			if (dist > radioOnda || dist < 0.001D)
				continue;
			double k = 1.0D - dist / radioOnda;
			Vec3 empuje = d.normalize().scale(2.6D * k).add(0.0D, 0.9D * k, 0.0D);
			e.setDeltaMovement(e.getDeltaMovement().add(empuje));
			e.hurtMarked = true;
			if (!(e instanceof ServerPlayer sp) || !sp.isCreative())
				e.igniteForSeconds(3 + (int) Math.round(9.0D * k)); // setSecondsOnFire en 1.20.1
		}
		BlockPos centro = BlockPos.containing(c);
		for (int i = 0; i < 70; i++) {
			BlockPos q = centro.offset(this.random.nextInt(27) - 13, this.random.nextInt(9) - 4, this.random.nextInt(27) - 13);
			if (nivel.isEmptyBlock(q) && BaseFireBlock.canBePlacedAt(nivel, q, Direction.UP))
				nivel.setBlockAndUpdate(q, BaseFireBlock.getState(nivel, q));
		}
		List<LivingEntity> cerca = nivel.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radioDano));
		for (LivingEntity e : cerca) {
			double d = e.position().add(0.0D, (double) e.getBbHeight() * 0.5D, 0.0D).distanceTo(c);
			if (d > radioDano || !e.isAlive())
				continue;
			float k = (float) (1.0D - d / radioDano);
			for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				ItemStack pieza = e.getItemBySlot(s);
				if (!pieza.isEmpty() && pieza.isDamageableItem()) {
					int dano = Math.max(1, Math.round((float) pieza.getMaxDamage() * DESGASTE * k));
					// 1.21.1: hurtAndBreak(cantidad, entidad, hueco) ya avisa de la rotura (antes broadcastBreakEvent)
					pieza.hurtAndBreak(dano, e, s);
				}
			}
			e.igniteForSeconds(Math.round(4.0F + 6.0F * k));
			if (e instanceof ServerPlayer p) {
				if (p.isCreative() || Ruleta.totemGastadoAhora(p))
					continue;
				p.invulnerableTime = 0;
				p.hurt(Ruleta.fuenteLetalPublica(p, "cloudruleta.meteorito", " fue aplastado por un meteorito de los Phora"), DANO_EXTRA * k);
			} else {
				e.invulnerableTime = 0;
				e.hurt(nivel.damageSources().explosion(this, null), DANO_EXTRA * k);
			}
		}
		ReviilPayload aviso = new ReviilPayload("impacto", Math.round(t * 100.0F),
				Component.literal(String.format(Locale.ROOT, "%.2f %.2f %.2f", c.x, c.y, c.z)), Component.empty());
		for (ServerPlayer p : nivel.players())
			if (p.position().distanceToSqr(c) < 102400.0D)
				Red.enviar(p, aviso);
	}
}
