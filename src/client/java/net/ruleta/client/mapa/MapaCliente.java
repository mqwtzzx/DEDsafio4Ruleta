package net.ruleta.client.mapa;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.TickEvent;

/** Tecla M: abre el mapa. */
public final class MapaCliente {
	/** Se registra con KeyBindingHelper en RuletaClient. */
	public static final KeyMapping TECLA = new KeyMapping("key.ruleta.mapa", InputConstants.Type.KEYSYM, 77, "key.categories.ruleta");

	private MapaCliente() {
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ClientTickEvent e) {
		if (e.phase != TickEvent.Phase.END)
			return;
		Minecraft mc = Minecraft.getInstance();
		while (TECLA.consumeClick()) {
			if (mc.level != null && mc.player != null && mc.screen == null)
				mc.setScreen(new PantallaMapa());
		}
	}
}
