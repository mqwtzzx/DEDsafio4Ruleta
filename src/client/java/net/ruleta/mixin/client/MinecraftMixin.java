package net.ruleta.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.ruleta.compat.Bus;
import net.ruleta.compat.client.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * InputEvent.InteractionKeyMappingTriggered (botón 1 = usar) de Forge 1.20.1: en Minecraft.startUseItem, al principio
 * de cada vuelta del bucle de manos (antes de player.getItemInHand(mano)). Si se cancela se sale de startUseItem y,
 * si el evento lo pide, se mueve la mano.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "startUseItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;",
			ordinal = 0), cancellable = true)
	private void ruleta$alUsar(CallbackInfo ci, @Local InteractionHand mano) {
		Minecraft mc = (Minecraft) (Object) this;
		InputEvent.InteractionKeyMappingTriggered e = new InputEvent.InteractionKeyMappingTriggered(1, mc.options.keyUse, mano);
		if (Bus.post(e)) {
			if (e.shouldSwingHand() && mc.player != null)
				mc.player.swing(mano);
			ci.cancel();
		}
	}
}
