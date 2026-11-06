package com.supernova;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.ruleta.compat.DeferredRegister;
import net.ruleta.compat.RegistryObject;

/** Supernova: estrella que crece, se carga y explota con una onda expansiva (espacio de nombres «supernova»). */
public class SupernovaMod {
	public static final String MODID = "supernova";
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);
	public static final RegistryObject<EntityType<SupernovaEntity>> SUPERNOVA = ENTITIES.register("supernova",
			() -> EntityType.Builder.<SupernovaEntity>of(SupernovaEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(64).updateInterval(20)
					.fireImmune().noSave().build("supernova:supernova"));
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MODID);
	public static final RegistryObject<SoundEvent> SND_IDLE = sound("idle_hum");
	public static final RegistryObject<SoundEvent> SND_CHARGE = sound("charge");
	public static final RegistryObject<SoundEvent> SND_HEARTBEAT = sound("heartbeat");
	public static final RegistryObject<SoundEvent> SND_BLAST = sound("blast");
	public static final RegistryObject<SoundEvent> SND_WAVE = sound("wave");
	public static final RegistryObject<SoundEvent> SND_MUSIC = sound("music");
	public static final ResourceKey<DamageType> SUPERNOVA_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "supernova"));

	private static RegistryObject<SoundEvent> sound(String name) {
		return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, name)));
	}

	public static void init() {
		ENTITIES.register();
		SOUNDS.register();
		SupernovaNet.init();
	}
}
