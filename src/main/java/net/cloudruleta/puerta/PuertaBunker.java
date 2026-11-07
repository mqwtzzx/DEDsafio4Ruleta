package net.cloudruleta.puerta;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.cloudruleta.compat.DeferredRegister;
import net.cloudruleta.compat.RegistryObject;

/** Puerta de búnker de 8×11 bloques. */
public final class PuertaBunker {
	public static final DeferredRegister<Block> BLOQUES = DeferredRegister.create(BuiltInRegistries.BLOCK, "cloudruleta");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, "cloudruleta");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "cloudruleta");
	public static final String SONIDOS_ESPACIO = "cloudruleta_puerta";
	public static final DeferredRegister<SoundEvent> SONIDOS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "cloudruleta");
	public static final RegistryObject<SoundEvent> SONIDO_ABRIR = SONIDOS.register("puerta_bunker.abrir",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cloudruleta_puerta", "puerta_bunker.abrir")));
	public static final RegistryObject<SoundEvent> SONIDO_CERRAR = SONIDOS.register("puerta_bunker.cerrar",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cloudruleta_puerta", "puerta_bunker.cerrar")));
	public static final RegistryObject<Block> BLOQUE = BLOQUES.register("puerta_bunker", () -> new PuertaBunkerBloque(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GRAY).strength(20.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)));
	public static final RegistryObject<Block> BLOQUE_HOJA = BLOQUES.register("puerta_bunker_hoja", () -> new PuertaBunkerHoja(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GRAY).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion()
			.isViewBlocking((st, lv, p) -> false).isSuffocating((st, lv, p) -> false).pushReaction(PushReaction.BLOCK)));
	public static final RegistryObject<Item> ITEM = ITEMS.register("puerta_bunker", () -> new BlockItem(BLOQUE.get(), new Item.Properties()));
	public static final RegistryObject<BlockEntityType<PuertaBunkerBE>> BE = BLOCK_ENTITIES.register("puerta_bunker",
			() -> BlockEntityType.Builder.of(PuertaBunkerBE::new, BLOQUE.get()).build(null));

	private PuertaBunker() {
	}

	/** Antes recibía el bus del mod de Forge; ahora basta con cargar la clase (registra al momento). */
	public static void init() {
		BloquesBunker.init();
		SONIDOS.register();
		BLOQUES.register();
		ITEMS.register();
		BLOCK_ENTITIES.register();
	}
}
