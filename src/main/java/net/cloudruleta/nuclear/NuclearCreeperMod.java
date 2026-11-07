package net.cloudruleta.nuclear;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.cloudruleta.compat.DeferredRegister;
import net.cloudruleta.compat.RegistryObject;
import net.cloudruleta.nuclear.entity.NuclearCreeperEntity;
import net.cloudruleta.nuclear.item.AbsorptionHeartItem;
import net.cloudruleta.nuclear.item.RedHeartItem;
import net.cloudruleta.nuclear.item.TotemItem;
import net.cloudruleta.nuclear.network.ModNetwork;

/** Creeper nuclear, corazones y tótems (espacio de nombres «cloudruleta_nuclearcreeper»). */
public class NuclearCreeperMod {
	public static final String MODID = "cloudruleta_nuclearcreeper";
	public static final double MAX_HEALTH = 25.0D;
	public static final double DETECTION_RANGE = 40.0D;
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MODID);
	public static final RegistryObject<EntityType<NuclearCreeperEntity>> NUCLEAR_CREEPER = ENTITIES.register("nuclear_creeper",
			() -> EntityType.Builder.of(NuclearCreeperEntity::new, MobCategory.MONSTER).sized(0.8F, 1.25F).clientTrackingRange(8).build("nuclear_creeper"));
	public static final RegistryObject<Item> NUCLEAR_CREEPER_SPAWN_EGG = ITEMS.register("nuclear_creeper_spawn_egg",
			() -> new SpawnEggItem(NUCLEAR_CREEPER.get(), 15255552, 12593176, new Item.Properties()));
	// 1.20.1: nutrition(0).saturationMod(0).alwaysEat() → 1.21.1: saturationModifier / alwaysEdible
	private static final FoodProperties HEART_FOOD = new FoodProperties.Builder().nutrition(0).saturationModifier(0.0F).alwaysEdible().build();
	public static final RegistryObject<Item> RED_HEART = ITEMS.register("red_heart",
			() -> new RedHeartItem(new Item.Properties().food(HEART_FOOD).rarity(Rarity.RARE)));
	public static final RegistryObject<Item> ABSORPTION_HEART = ITEMS.register("absorption_heart",
			() -> new AbsorptionHeartItem(new Item.Properties().food(HEART_FOOD).rarity(Rarity.RARE)));
	public static final RegistryObject<Item> GLEBANOIDE_TOTEM = ITEMS.register("glebanoide_totem",
			() -> new TotemItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON), TotemItem.Kind.GLEBANOIDE));
	public static final RegistryObject<Item> NUTRITOTEM = ITEMS.register("nutritotem",
			() -> new TotemItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON), TotemItem.Kind.NUTRITOTEM));
	public static final RegistryObject<Item> EON_TOTEM = ITEMS.register("eon_totem",
			() -> new TotemItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON), TotemItem.Kind.EON));
	public static final RegistryObject<Item> YUNQUE_TOTEM = ITEMS.register("yunque_totem",
			() -> new TotemItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON), TotemItem.Kind.YUNQUE));
	public static final RegistryObject<Item> GOLEM_TOTEM = ITEMS.register("golem_totem",
			() -> new TotemItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON), TotemItem.Kind.GOLEM));
	public static final RegistryObject<SoundEvent> RADIATION_LOOP = SOUNDS.register("radiation_loop",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "radiation_loop")));

	public static void init() {
		ENTITIES.register();
		ITEMS.register();
		SOUNDS.register();
		commonSetup();
	}

	/** Antes FMLCommonSetupEvent → enqueueWork(ModNetwork::register). */
	private static void commonSetup() {
		ModNetwork.register();
	}
}
