package net.cloudruleta;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.ExplosionEvent;
import net.cloudruleta.nuclear.entity.NuclearCreeperEntity;

/** Manda a los jugadores cercanos (96 bloques) la onda visual de cada explosión. */
public final class OndasExplosion {
	private static final double ALCANCE = 96.0D;

	private OndasExplosion() {
	}

	@SubscribeEvent
	public static void alDetonar(ExplosionEvent.Detonate evento) {
		if (evento.getLevel().isClientSide() || !(evento.getLevel() instanceof ServerLevel nivel))
			return;
		Explosion explosion = evento.getExplosion();
		Vec3 c = explosion.center();
		OndaPayload onda = new OndaPayload(c.x, c.y, c.z, radio(explosion.getDirectSourceEntity()));
		// antes PacketDistributor.NEAR: jugadores de la misma dimensión a menos de 96 bloques
		for (ServerPlayer p : nivel.players())
			if (p.distanceToSqr(c.x, c.y, c.z) < ALCANCE * ALCANCE)
				Red.enviar(p, onda);
	}

	private static float radio(Entity causa) {
		if (causa instanceof NuclearCreeperEntity)
			return 34.0F;
		if (causa instanceof Creeper creeper)
			return creeper.isPowered() ? 22.0F : 15.0F;
		return causa instanceof PrimedTnt || causa instanceof MinecartTNT ? 18.0F : 15.0F;
	}
}
