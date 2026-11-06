package net.ruleta.nuclear;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.ruleta.compat.DatosPersistentes;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.PlayerEvent;

/** Corazones rojos: cada uno suma 1 corazón de vida máxima, guardado en los datos persistentes del jugador. */
public final class HeartEvents {
	private static final String KEY = "nuclearcreeper_extra_hearts";
	/** En 1.21.1 los modificadores llevan un ResourceLocation (antes UUID 5b0f6a52-3c1e-4a7d-9c64-1d2e8f7a4b10, nombre «Corazon Rojo»). */
	private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(NuclearCreeperMod.MODID, "corazon_rojo");

	private HeartEvents() {
	}

	public static int getHearts(Player player) {
		return DatosPersistentes.de(player).getInt(KEY);
	}

	public static void addHeart(ServerPlayer player) {
		DatosPersistentes.de(player).putInt(KEY, getHearts(player) + 1);
		apply(player);
		player.heal(2.0F);
	}

	public static void apply(Player player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null)
			return;
		maxHealth.removeModifier(MODIFIER_ID);
		int hearts = getHearts(player);
		if (hearts > 0)
			maxHealth.addTransientModifier(new AttributeModifier(MODIFIER_ID, (double) hearts * 2.0D, AttributeModifier.Operation.ADD_VALUE));
		if (player.getHealth() > player.getMaxHealth())
			player.setHealth(player.getMaxHealth());
	}

	@SubscribeEvent
	public static void onClone(PlayerEvent.Clone event) {
		int hearts = DatosPersistentes.de(event.getOriginal()).getInt(KEY);
		DatosPersistentes.de(event.getEntity()).putInt(KEY, hearts);
		apply(event.getEntity());
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		apply(event.getEntity());
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		apply(event.getEntity());
		if (!event.isEndConquered())
			event.getEntity().setHealth(event.getEntity().getMaxHealth());
	}
}
