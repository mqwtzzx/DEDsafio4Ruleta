package net.cloudruleta.compat.event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.cloudruleta.compat.Event;

import java.util.Collection;

/** Eventos de seres vivos de Forge (LivingDeathEvent, LivingHurtEvent…), publicados desde mixins. */
public final class LivingEvents {
	private LivingEvents() {}

	/** Base con la entidad (como LivingEvent de Forge). */
	public abstract static class LivingEvent extends Event {
		private final LivingEntity entity;

		protected LivingEvent(LivingEntity entity) {
			this.entity = entity;
		}

		public LivingEntity getEntity() {
			return entity;
		}
	}

	/** Al principio de LivingEntity.die (cancelable: la entidad no muere). */
	public static final class LivingDeathEvent extends LivingEvent {
		private final DamageSource source;

		public LivingDeathEvent(LivingEntity entity, DamageSource source) {
			super(entity);
			this.source = source;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public DamageSource getSource() {
			return source;
		}
	}

	/** Al empezar a aplicar daño (LivingEntity.actuallyHurt, antes de armadura): se puede cambiar la cantidad. */
	public static final class LivingHurtEvent extends LivingEvent {
		private final DamageSource source;
		private float amount;

		public LivingHurtEvent(LivingEntity entity, DamageSource source, float amount) {
			super(entity);
			this.source = source;
			this.amount = amount;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public DamageSource getSource() {
			return source;
		}

		public float getAmount() {
			return amount;
		}

		public void setAmount(float amount) {
			this.amount = amount;
		}
	}

	/** Justo antes de restar la vida (tras armadura y absorción). Cancelable: no se resta. */
	public static final class LivingDamageEvent extends LivingEvent {
		private final DamageSource source;
		private float amount;

		public LivingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
			super(entity);
			this.source = source;
			this.amount = amount;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public DamageSource getSource() {
			return source;
		}

		public float getAmount() {
			return amount;
		}

		public void setAmount(float amount) {
			this.amount = amount;
		}
	}

	/** Botín al morir: la lista se puede ampliar o vaciar. */
	public static final class LivingDropsEvent extends LivingEvent {
		private final DamageSource source;
		private final Collection<ItemEntity> drops;

		public LivingDropsEvent(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops) {
			super(entity);
			this.source = source;
			this.drops = drops;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public DamageSource getSource() {
			return source;
		}

		public Collection<ItemEntity> getDrops() {
			return drops;
		}
	}

	/** Un tótem de la inmortalidad salva a la entidad. */
	public static final class LivingUseTotemEvent extends LivingEvent {
		private final DamageSource source;
		private final ItemStack totem;
		private final InteractionHand hand;

		public LivingUseTotemEvent(LivingEntity entity, DamageSource source, ItemStack totem, InteractionHand hand) {
			super(entity);
			this.source = source;
			this.totem = totem;
			this.hand = hand;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public DamageSource getSource() {
			return source;
		}

		public ItemStack getTotem() {
			return totem;
		}

		public InteractionHand getHandHolding() {
			return hand;
		}
	}

	/** Al principio del tick de cada ser vivo (LivingEvent.LivingTickEvent). */
	public static final class LivingTickEvent extends LivingEvent {
		public LivingTickEvent(LivingEntity entity) {
			super(entity);
		}

		@Override
		public boolean isCancelable() {
			return true;
		}
	}

	/** Visibilidad de un ser vivo para otro (multiplicador). */
	public static final class LivingVisibilityEvent extends LivingEvent {
		private final Entity lookingEntity;
		private double visibilityModifier;

		public LivingVisibilityEvent(LivingEntity entity, Entity lookingEntity, double original) {
			super(entity);
			this.lookingEntity = lookingEntity;
			this.visibilityModifier = original;
		}

		public void modifyVisibility(double mod) {
			visibilityModifier *= mod;
		}

		public double getVisibilityModifier() {
			return visibilityModifier;
		}

		public Entity getLookingEntity() {
			return lookingEntity;
		}
	}

	/** Al terminar de usar un objeto (comer, beber…). */
	public static final class LivingUseItemFinish extends LivingEvent {
		private final ItemStack item;
		private ItemStack result;

		public LivingUseItemFinish(LivingEntity entity, ItemStack item, ItemStack result) {
			super(entity);
			this.item = item;
			this.result = result;
		}

		public ItemStack getItem() {
			return item;
		}

		public ItemStack getResultStack() {
			return result;
		}

		public void setResultStack(ItemStack result) {
			this.result = result;
		}
	}
}
