package net.cloudruleta.phora;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.cloudruleta.compat.DeferredRegister;
import net.cloudruleta.compat.RegistryObject;

/** Meteoritos de los Phora y el palo que los llama. */
public final class PhoraMod {
	public static final DeferredRegister<EntityType<?>> ENTIDADES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, "cloudruleta");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, "cloudruleta");
	// setShouldReceiveVelocityUpdates(true) de Forge: en vanilla un tipo de entidad nuevo ya envía su velocidad (trackDeltas)
	public static final RegistryObject<EntityType<MeteoritoEntity>> METEORITO = ENTIDADES.register("meteorito",
			() -> EntityType.Builder.<MeteoritoEntity>of(MeteoritoEntity::new, MobCategory.MISC).sized(2.0F, 2.0F).clientTrackingRange(20).updateInterval(1)
					.fireImmune().noSave().build("cloudruleta:meteorito"));
	public static final RegistryObject<Item> PALO = ITEMS.register("palo_phora",
			() -> new PaloPhora(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

	private PhoraMod() {
	}

	public static void init() {
		ENTIDADES.register();
		ITEMS.register();
	}
}
