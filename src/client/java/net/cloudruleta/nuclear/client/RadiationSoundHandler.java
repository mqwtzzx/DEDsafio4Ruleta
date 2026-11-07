package net.cloudruleta.nuclear.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.world.phys.AABB;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.TickEvent;
import net.cloudruleta.nuclear.entity.NuclearCreeperEntity;

public class RadiationSoundHandler {
	public static final float RANGE = 6.0F;
	private static final Map<Integer, RadiationSoundInstance> ACTIVE = new HashMap<>();

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			ACTIVE.clear();
			return;
		}
		ACTIVE.values().removeIf(AbstractTickableSoundInstance::isStopped);
		AABB box = mc.player.getBoundingBox().inflate((double) RANGE);
		for (NuclearCreeperEntity creeper : mc.level.getEntitiesOfClass(NuclearCreeperEntity.class, box)) {
			if (!creeper.isAlive())
				continue;
			RadiationSoundInstance current = ACTIVE.get(creeper.getId());
			if (current == null || current.isStopped()) {
				RadiationSoundInstance instance = new RadiationSoundInstance(creeper);
				ACTIVE.put(creeper.getId(), instance);
				mc.getSoundManager().play(instance);
			}
		}
	}
}
