package net.ruleta.nuclear.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class ClientTotemHooks {
	private ClientTotemHooks() {
	}

	public static void showActivation(ItemStack stack) {
		Minecraft.getInstance().gameRenderer.displayItemActivation(stack);
	}
}
