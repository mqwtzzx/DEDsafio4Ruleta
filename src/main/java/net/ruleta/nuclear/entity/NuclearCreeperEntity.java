package net.ruleta.nuclear.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.ruleta.bunker.Bunkers;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/** Creeper nuclear: brilla, cava hacia el jugador si no puede llegar y solo explota cara a cara. */
public class NuclearCreeperEntity extends Creeper {
	public static final int OUTLINE_COLOR = 16777215;
	public static final double FACE_TO_FACE_GAP = 1.0D;
	private static final int PROGRESS_INTERVAL = 6;
	private static final double MIN_PROGRESS = 0.3D;
	private static final double DIG_REACH = 1.8D;
	private static final int MAX_BLOCKS_PER_DIG = 12;
	private boolean tunneling = false;
	private double lastDist = Double.MAX_VALUE;

	public NuclearCreeperEntity(EntityType<? extends Creeper> type, Level level) {
		super(type, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new NuclearSwellGoal(this));
		this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0D, 1.2D));
		this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0D, 1.2D));
		this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, null) {
			{
				this.targetConditions = this.targetConditions.ignoreLineOfSight();
			}
		});
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
	}

	public boolean isCharging() {
		return this.getSwellDir() > 0 || this.isIgnited();
	}

	public boolean isFaceToFace(LivingEntity other) {
		return this.getBoundingBox().inflate(FACE_TO_FACE_GAP).intersects(other.getBoundingBox());
	}

	@Override
	public void setSwellDir(int dir) {
		if (dir <= 0 && this.getSwellDir() > 0)
			return;
		super.setSwellDir(dir);
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		if (this.isCharging() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return true;
		return super.isInvulnerableTo(source);
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (this.isCharging()) {
			this.tunneling = false;
			this.getNavigation().stop();
			this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0D);
			return;
		}
		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive() || !this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
			this.tunneling = false;
			this.lastDist = Double.MAX_VALUE;
			return;
		}
		if (this.isFaceToFace(target)) {
			this.tunneling = false;
			return;
		}
		if (this.tickCount % PROGRESS_INTERVAL == 0) {
			double dist = this.distanceTo(target);
			boolean noProgress = this.lastDist - dist < MIN_PROGRESS;
			this.lastDist = dist;
			if (noProgress || this.tunneling) {
				Path path = this.getNavigation().createPath(target, 0);
				this.tunneling = path == null || !path.canReach();
			}
		}
		if (this.tunneling) {
			this.getNavigation().stop();
			this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
			this.getLookControl().setLookAt(target, 30.0F, 30.0F);
			if (this.tickCount % 2 == 0)
				this.digAhead(target);
		}
	}

	private void digAhead(LivingEntity target) {
		Vec3 from = new Vec3(this.getX(), this.getY() + 0.6D, this.getZ());
		Vec3 to = new Vec3(target.getX(), target.getY() + (double) target.getBbHeight() * 0.5D, target.getZ());
		Vec3 dir = to.subtract(from);
		if (dir.lengthSqr() < 1.0E-4D)
			return;
		dir = dir.normalize();
		Set<BlockPos> cells = new LinkedHashSet<>();
		for (double t = 0.4D; t <= DIG_REACH; t += 0.4D) {
			BlockPos p = BlockPos.containing(from.add(dir.scale(t)));
			cells.add(p);
			cells.add(p.above());
		}
		int broken = 0;
		for (BlockPos p : cells) {
			if (broken >= MAX_BLOCKS_PER_DIG)
				break;
			BlockState state = this.level().getBlockState(p);
			if (state.isAir() || state.getBlock() instanceof LiquidBlock || Bunkers.protegido(this.level(), p, state))
				continue;
			if (this.level().destroyBlock(p, false, this))
				broken++;
		}
	}

	@Override
	public boolean isCurrentlyGlowing() {
		return true;
	}

	@Override
	public int getTeamColor() {
		return OUTLINE_COLOR;
	}

	private static class NuclearSwellGoal extends Goal {
		private final NuclearCreeperEntity creeper;

		NuclearSwellGoal(NuclearCreeperEntity creeper) {
			this.creeper = creeper;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = this.creeper.getTarget();
			return this.creeper.isCharging() || target != null && target.isAlive() && this.creeper.isFaceToFace(target);
		}

		@Override
		public void start() {
			this.creeper.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (this.creeper.isCharging())
				return;
			LivingEntity target = this.creeper.getTarget();
			if (target != null && target.isAlive() && this.creeper.isFaceToFace(target))
				this.creeper.setSwellDir(1);
			else
				this.creeper.setSwellDir(-1);
		}
	}
}
