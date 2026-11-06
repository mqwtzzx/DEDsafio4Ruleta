package net.ruleta.nuclear;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;

/** Antes suscriptor del bus del mod (EntityAttributeCreationEvent). */
public class ModEvents {
	public static void registrar() {
		registerAttributes();
	}

	public static void registerAttributes() {
		FabricDefaultAttributeRegistry.register(NuclearCreeperMod.NUCLEAR_CREEPER.get(),
				Creeper.createAttributes().add(Attributes.MAX_HEALTH, NuclearCreeperMod.MAX_HEALTH).add(Attributes.FOLLOW_RANGE, NuclearCreeperMod.DETECTION_RANGE).build());
	}
}
