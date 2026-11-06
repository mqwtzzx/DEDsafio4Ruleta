package net.ruleta.agujero;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.ruleta.compat.DeferredRegister;
import net.ruleta.compat.RegistryObject;

public final class AgujeroNegro {
	public static final DeferredRegister<EntityType<?>> ENTIDADES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, "ruleta");
	public static final RegistryObject<EntityType<AgujeroNegroEntity>> TIPO = ENTIDADES.register("agujero_negro",
			() -> EntityType.Builder.<AgujeroNegroEntity>of(AgujeroNegroEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).clientTrackingRange(32).updateInterval(20)
					.fireImmune().build("agujero_negro"));

	private AgujeroNegro() {
	}

	public static void init() {
		ENTIDADES.register();
	}
}
