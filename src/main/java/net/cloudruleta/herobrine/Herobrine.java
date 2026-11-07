package net.cloudruleta.herobrine;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.cloudruleta.compat.DeferredRegister;
import net.cloudruleta.compat.RegistryObject;

public final class Herobrine {
	public static final DeferredRegister<EntityType<?>> ENTIDADES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, "cloudruleta");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, "cloudruleta");
	public static final RegistryObject<EntityType<HerobrineEntity>> HEROBRINE = ENTIDADES.register("herobrine",
			() -> EntityType.Builder.of(HerobrineEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).clientTrackingRange(10).build("herobrine"));
	public static final RegistryObject<Item> HUEVO = ITEMS.register("herobrine_spawn_egg",
			() -> new SpawnEggItem(HEROBRINE.get(), 43176, 3878812, new Item.Properties()));

	private Herobrine() {
	}

	public static void init() {
		ENTIDADES.register();
		ITEMS.register();
		atributos();
	}

	private static void atributos() {
		FabricDefaultAttributeRegistry.register(HEROBRINE.get(), HerobrineEntity.atributos().build());
	}
}
