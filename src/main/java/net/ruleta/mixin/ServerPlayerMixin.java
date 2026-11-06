package net.ruleta.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.ruleta.compat.Ganchos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ServerPlayer tiene su propio die y changeDimension: LivingDeathEvent y EntityTravelToDimensionEvent. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(method = "die", at = @At("HEAD"), cancellable = true)
	private void ruleta$morir(DamageSource fuente, CallbackInfo ci) {
		if (Ganchos.alMorir((ServerPlayer) (Object) this, fuente))
			ci.cancel();
	}

	@Inject(method = "changeDimension", at = @At("HEAD"), cancellable = true)
	private void ruleta$viajar(DimensionTransition transicion, CallbackInfoReturnable<Entity> cir) {
		if (Ganchos.alViajar((ServerPlayer) (Object) this, transicion.newLevel().dimension()))
			cir.setReturnValue(null);
	}
}
