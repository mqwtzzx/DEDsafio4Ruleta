package net.cloudruleta.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.cloudruleta.compat.Bus;
import net.cloudruleta.compat.event.FillBucketEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FillBucketEvent: con el bloque al que apunta el cubo ya calculado. Cancelado: el cubo no hace nada. */
@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
	@Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/BlockHitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;", ordinal = 0),
			cancellable = true)
	private void cloudruleta$cubo(Level nivel, Player jugador, InteractionHand mano, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir,
							 @Local BlockHitResult impacto) {
		ItemStack cubo = jugador.getItemInHand(mano);
		if (Bus.post(new FillBucketEvent(jugador, cubo, nivel, impacto)))
			cir.setReturnValue(InteractionResultHolder.fail(cubo));
	}
}
