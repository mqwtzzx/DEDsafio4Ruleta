package net.ruleta.nuclear;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.ExplosionEvent;
import net.ruleta.compat.event.ServerLifecycle;
import net.ruleta.compat.event.TickEvent;
import net.ruleta.nuclear.entity.NuclearCreeperEntity;

/** La explosión del creeper nuclear se sustituye por la explosión nuclear por etapas. */
public class NuclearEvents {
	@SubscribeEvent
	public static void onExplosionStart(ExplosionEvent.Start event) {
		if (event.getLevel().isClientSide())
			return;
		Entity source = event.getExplosion().getDirectSourceEntity();
		if (source instanceof NuclearCreeperEntity creeper && event.getLevel() instanceof ServerLevel level) {
			event.setCanceled(true);
			NuclearExplosion.detonate(level, creeper);
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END)
			NuclearExplosion.tick();
	}

	@SubscribeEvent
	public static void onServerStopped(ServerLifecycle.ServerStoppedEvent event) {
		NuclearExplosion.clear();
	}
}
