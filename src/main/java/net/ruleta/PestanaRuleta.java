package net.ruleta;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ruleta.compat.DeferredRegister;
import net.ruleta.compat.RegistryObject;
import net.ruleta.creeperes.Creeperes;
import net.ruleta.creeperrojo.CreeperRojo;
import net.ruleta.herobrine.Herobrine;
import net.ruleta.nuclear.NuclearCreeperMod;
import net.ruleta.objetos.Objetos;
import net.ruleta.phora.PhoraMod;
import net.ruleta.puerta.BloquesBunker;
import net.ruleta.puerta.PuertaBunker;

/** Pestaña «Ruleta» del inventario creativo. Fabric API la reparte en sus páginas como en Forge. */
public final class PestanaRuleta {
	public static final DeferredRegister<CreativeModeTab> PESTANAS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, "ruleta");
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, "ruleta");
	public static final RegistryObject<Item> ICONO = ITEMS.register("ruleta", () -> new Item(new Item.Properties()));
	public static final RegistryObject<CreativeModeTab> PESTANA = PESTANAS.register("ruleta", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
			.title(Component.translatable("itemGroup.ruleta"))
			.icon(() -> new ItemStack(ICONO.get()))
			.displayItems((parametros, salida) -> {
				salida.accept(PuertaBunker.ITEM.get());
				salida.accept(BloquesBunker.ITEM_LADRILLO.get());
				salida.accept(BloquesBunker.ITEM_ESCALERA.get());
				salida.accept(BloquesBunker.ITEM_PISO.get());
				salida.accept(BloquesBunker.ITEM_PELIGRO.get());
				salida.accept(PhoraMod.PALO.get());
				salida.accept(NuclearCreeperMod.GLEBANOIDE_TOTEM.get());
				salida.accept(NuclearCreeperMod.NUTRITOTEM.get());
				salida.accept(NuclearCreeperMod.EON_TOTEM.get());
				salida.accept(NuclearCreeperMod.YUNQUE_TOTEM.get());
				salida.accept(NuclearCreeperMod.GOLEM_TOTEM.get());
				salida.accept(NuclearCreeperMod.RED_HEART.get());
				salida.accept(NuclearCreeperMod.ABSORPTION_HEART.get());
				salida.accept(NuclearCreeperMod.NUCLEAR_CREEPER_SPAWN_EGG.get());
				salida.accept(CreeperRojo.HUEVO.get());
				salida.accept(Creeperes.HUEVO_CESPED.get());
				salida.accept(Herobrine.HUEVO.get());
				salida.accept(Objetos.LINTERNA.get());
				salida.accept(Objetos.BATERIA.get());
				salida.accept(Objetos.PALO_REGRESO.get());
			})
			.build());

	private PestanaRuleta() {
	}

	public static void init() {
		PESTANAS.register();
		ITEMS.register();
	}
}
