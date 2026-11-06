package net.ruleta.puerta;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.ruleta.compat.DeferredRegister;
import net.ruleta.compat.RegistryObject;

/** Ladrillo, escalera, piso y bloque de peligro del búnker. */
public final class BloquesBunker {
	public static final DeferredRegister<Block> BLOQUES = DeferredRegister.create(BuiltInRegistries.BLOCK, "ruleta");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, "ruleta");
	public static final RegistryObject<Block> LADRILLO = BLOQUES.register("ladrillo_bunker", () -> new LadrilloBunkerBloque(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GRAY).strength(3.0F, 12.0F).sound(SoundType.DEEPSLATE_BRICKS).requiresCorrectToolForDrops()));
	// en 1.21.1 el constructor de StairBlock es protegido: subclase anónima, mismo comportamiento
	public static final RegistryObject<Block> ESCALERA = BLOQUES.register("escalera_bunker", () -> new StairBlock(LADRILLO.get().defaultBlockState(),
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 12.0F).sound(SoundType.DEEPSLATE_BRICKS).requiresCorrectToolForDrops()) {
	});
	public static final RegistryObject<Block> PISO = BLOQUES.register("piso_bunker", () -> new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BROWN).strength(2.0F, 8.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
	public static final RegistryObject<Block> PELIGRO = BLOQUES.register("peligro_bunker", () -> new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_YELLOW).strength(3.0F, 12.0F).sound(SoundType.DEEPSLATE_BRICKS).requiresCorrectToolForDrops()));
	public static final RegistryObject<Item> ITEM_LADRILLO = ITEMS.register("ladrillo_bunker", () -> new BlockItem(LADRILLO.get(), new Item.Properties()));
	public static final RegistryObject<Item> ITEM_ESCALERA = ITEMS.register("escalera_bunker", () -> new BlockItem(ESCALERA.get(), new Item.Properties()));
	public static final RegistryObject<Item> ITEM_PISO = ITEMS.register("piso_bunker", () -> new BlockItem(PISO.get(), new Item.Properties()));
	public static final RegistryObject<Item> ITEM_PELIGRO = ITEMS.register("peligro_bunker", () -> new BlockItem(PELIGRO.get(), new Item.Properties()));

	private BloquesBunker() {
	}

	public static void init() {
		BLOQUES.register();
		ITEMS.register();
	}
}
