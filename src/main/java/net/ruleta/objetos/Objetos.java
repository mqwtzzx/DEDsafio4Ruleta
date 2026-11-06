package net.ruleta.objetos;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.ruleta.compat.DeferredRegister;
import net.ruleta.compat.RegistryObject;

/** Palo de regreso, linterna y batería. */
public final class Objetos {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, "ruleta");
	public static final DeferredRegister<SoundEvent> SONIDOS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "ruleta");
	public static final RegistryObject<Item> PALO_REGRESO = ITEMS.register("palo_regreso", () -> new PaloRegreso(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
	public static final RegistryObject<Item> LINTERNA = ITEMS.register("linterna", () -> new Linterna(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> BATERIA = ITEMS.register("bateria", () -> new Item(new Item.Properties().stacksTo(16)));
	public static final RegistryObject<SoundEvent> SONIDO_REGRESO = SONIDOS.register("palo.regreso",
			() -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("ruleta", "palo.regreso")));

	private Objetos() {
	}

	public static void init() {
		ITEMS.register();
		SONIDOS.register();
	}
}
