package net.ruleta.creeperrojo;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ruleta.Efectos;
import net.ruleta.compat.DeferredRegister;
import net.ruleta.compat.RegistryObject;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.ExplosionEvent;

/** Creeper rojo: su explosión no rompe nada, pero da Pantalla Azul a los jugadores a 12 bloques. */
public final class CreeperRojo {
	public static final double RADIO_EFECTO = 12.0D;
	public static final DeferredRegister<EntityType<?>> ENTIDADES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, "ruleta");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, "ruleta");
	public static final RegistryObject<EntityType<CreeperRojoEntity>> TIPO = ENTIDADES.register("creeper_rojo",
			() -> EntityType.Builder.of(CreeperRojoEntity::new, MobCategory.MONSTER).sized(0.6F, 1.0F).clientTrackingRange(8).build("creeper_rojo"));
	public static final RegistryObject<Item> HUEVO = ITEMS.register("creeper_rojo_spawn_egg",
			() -> new SpawnEggItem(TIPO.get(), 14687764, 5900805, new Item.Properties()));

	private CreeperRojo() {
	}

	public static void init() {
		ENTIDADES.register();
		ITEMS.register();
		atributos();
	}

	private static void atributos() {
		FabricDefaultAttributeRegistry.register(TIPO.get(), Creeper.createAttributes().build());
	}

	public static final class Eventos {
		private Eventos() {
		}

		@SubscribeEvent
		public static void alDetonar(ExplosionEvent.Detonate evento) {
			Explosion explosion = evento.getExplosion();
			// getExploder() de 1.20.1 es getDirectSourceEntity() en 1.21.1
			if (!(explosion.getDirectSourceEntity() instanceof CreeperRojoEntity))
				return;
			evento.getAffectedBlocks().clear();
			evento.getAffectedEntities().clear();
			Vec3 centro = explosion.center();
			AABB zona = new AABB(centro.x - RADIO_EFECTO, centro.y - RADIO_EFECTO, centro.z - RADIO_EFECTO, centro.x + RADIO_EFECTO, centro.y + RADIO_EFECTO, centro.z + RADIO_EFECTO);
			for (Player jugador : evento.getLevel().getEntitiesOfClass(Player.class, zona))
				jugador.addEffect(new MobEffectInstance(Efectos.PANTALLA_AZUL.holder(), 400, 0, false, false, false));
		}
	}
}
