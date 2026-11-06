package net.ruleta;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.ruleta.compat.EventPriority;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.LivingEvents.LivingDeathEvent;

/** Manda a todos el mensaje de muerte de un jugador (para la animación de muerte en pantalla). */
public final class Muertes {
	private Muertes() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void alMorir(LivingDeathEvent e) {
		if (e.isCanceled() || !(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null)
			return;
		Component mensaje = p.getCombatTracker().getDeathMessage();
		Red.enviarATodos(p.getServer(), new ReviilPayload("muerte", 1, mensaje, Component.empty()));
	}
}
