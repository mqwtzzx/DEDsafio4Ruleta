package net.cloudruleta.nuclear;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.cloudruleta.Ruleta;
import net.cloudruleta.compat.DatosPersistentes;
import net.cloudruleta.compat.EventPriority;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.LivingEvents;
import net.cloudruleta.compat.event.TickEvent;
import net.cloudruleta.nuclear.item.TotemItem;
import net.cloudruleta.nuclear.network.ModNetwork;
import net.cloudruleta.nuclear.network.TotemActivatePacket;

/** Tótems del mod: salvan de la muerte (en la mano; el de Eón también desde el inventario). */
public final class TotemEvents {
	/** Antes UUID c3a1e7b4-6d52-4f0a-8b1e-2f9d7a5c4e21 («Totem Golem»). */
	private static final ResourceLocation GOLEM_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(NuclearCreeperMod.MODID, "totem_golem");
	public static final double GOLEM_BONUS_HEALTH = 4.0D;
	private static final String EON_HASTA = "cloudruleta_eon_inmune_hasta";
	private static final long EON_TICKS = 60L;

	private TotemEvents() {
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onDeath(LivingEvents.LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return;
		if (eonInmune(player)) {
			event.setCanceled(true);
			player.setHealth(1.0F);
			return;
		}
		ItemStack used = findTotemInHands(player);
		if (used.isEmpty())
			used = findEon(player);
		if (used.isEmpty())
			return;
		TotemItem.Kind kind = ((TotemItem) used.getItem()).getKind();
		ItemStack shown = used.copy();
		used.shrink(1);
		event.setCanceled(true);
		player.setHealth(1.0F);
		player.removeAllEffects();
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
		if (kind == TotemItem.Kind.EON)
			DatosPersistentes.de(player).putLong(EON_HASTA, player.level().getGameTime() + EON_TICKS);
		if (kind == TotemItem.Kind.YUNQUE)
			repairArmor(player);
		player.awardStat(Stats.ITEM_USED.get(shown.getItem()));
		player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.5D, 0.6D, 0.5D, 0.4D);
		player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
		ModNetwork.enviar(player, new TotemActivatePacket(shown));
		Ruleta.avisarTotem(player, event.getSource(), Component.literal(kind.displayName));
	}

	private static boolean eonInmune(ServerPlayer p) {
		return DatosPersistentes.de(p).getLong(EON_HASTA) >= p.level().getGameTime();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDamage(LivingEvents.LivingDamageEvent event) {
		if (event.getEntity() instanceof ServerPlayer p && eonInmune(p) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
				&& event.getAmount() >= p.getHealth() + p.getAbsorptionAmount())
			event.setCanceled(true);
	}

	private static ItemStack findTotemInHands(ServerPlayer player) {
		ItemStack golem = ItemStack.EMPTY;
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() instanceof TotemItem totem && totem.getKind().revives) {
				if (totem.getKind() != TotemItem.Kind.GOLEM)
					return stack;
				if (golem.isEmpty())
					golem = stack;
			}
		}
		return golem;
	}

	private static boolean hasAnyReviveTotem(ServerPlayer player) {
		int size = player.getInventory().getContainerSize();
		for (int i = 0; i < size; i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(Items.TOTEM_OF_UNDYING))
				return true;
			if (stack.getItem() instanceof TotemItem totem && totem.getKind().revives && totem.getKind() != TotemItem.Kind.EON)
				return true;
		}
		return false;
	}

	private static ItemStack findEon(ServerPlayer player) {
		int size = player.getInventory().getContainerSize();
		for (int i = 0; i < size; i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.getItem() instanceof TotemItem totem && totem.getKind() == TotemItem.Kind.EON)
				return stack;
		}
		return ItemStack.EMPTY;
	}

	private static void repairArmor(ServerPlayer player) {
		for (ItemStack armor : player.getArmorSlots())
			if (!armor.isEmpty() && armor.isDamageableItem())
				armor.setDamageValue(0);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide)
			return;
		Player player = event.player;
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null)
			return;
		boolean holdingGolem = isGolem(player.getMainHandItem()) || isGolem(player.getOffhandItem());
		boolean applied = maxHealth.getModifier(GOLEM_MODIFIER_ID) != null;
		if (holdingGolem && !applied) {
			maxHealth.addTransientModifier(new AttributeModifier(GOLEM_MODIFIER_ID, GOLEM_BONUS_HEALTH, AttributeModifier.Operation.ADD_VALUE));
		} else if (!holdingGolem && applied) {
			maxHealth.removeModifier(GOLEM_MODIFIER_ID);
			if (player.getHealth() > player.getMaxHealth())
				player.setHealth(player.getMaxHealth());
		}
	}

	private static boolean isGolem(ItemStack stack) {
		return stack.getItem() instanceof TotemItem totem && totem.getKind() == TotemItem.Kind.GOLEM;
	}
}
