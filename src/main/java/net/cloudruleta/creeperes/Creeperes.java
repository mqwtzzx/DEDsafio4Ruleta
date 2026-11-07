package net.cloudruleta.creeperes;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.cloudruleta.compat.DeferredRegister;
import net.cloudruleta.compat.RegistryObject;

/** Creeper de césped (sale al romper hierba con el cambio «mob-creeper-pasto»). */
public final class Creeperes {
	public static final DeferredRegister<EntityType<?>> ENTIDADES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, "cloudruleta");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, "cloudruleta");
	public static final RegistryObject<EntityType<CreeperCespedEntity>> CESPED = ENTIDADES.register("creeper_cesped",
			() -> EntityType.Builder.of(CreeperCespedEntity::new, MobCategory.MONSTER).sized(0.3F, 0.5F).clientTrackingRange(8).build("creeper_cesped"));
	// ForgeSpawnEggItem recibía un Supplier; el SpawnEggItem de vanilla recibe el tipo ya registrado (mismos colores)
	public static final RegistryObject<Item> HUEVO_CESPED = ITEMS.register("creeper_cesped_spawn_egg",
			() -> new SpawnEggItem(CESPED.get(), 4160042, 1784340, new Item.Properties()));

	private Creeperes() {
	}

	public static void init() {
		ENTIDADES.register();
		ITEMS.register();
		atributos();
	}

	/** Antes EntityAttributeCreationEvent. */
	private static void atributos() {
		FabricDefaultAttributeRegistry.register(CESPED.get(), Creeper.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.42D).build());
	}
}
