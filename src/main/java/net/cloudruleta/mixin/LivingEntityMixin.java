package net.cloudruleta.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.cloudruleta.compat.Ganchos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Eventos de seres vivos de Forge: muerte, daño, botín, tótem, tick, visibilidad y fin de usar un objeto. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** LivingDeathEvent (los jugadores lo lanzan desde ServerPlayerMixin, que tiene su propio die). */
	@Inject(method = "die", at = @At("HEAD"), cancellable = true)
	private void cloudruleta$morir(DamageSource fuente, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer)
			return;
		if (Ganchos.alMorir((LivingEntity) (Object) this, fuente))
			ci.cancel();
	}

	/** LivingHurtEvent: al empezar actuallyHurt, antes de armadura y absorción. */
	@ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
	private float cloudruleta$herir(float cantidad, @Local(argsOnly = true) DamageSource fuente) {
		return Ganchos.alHerir((LivingEntity) (Object) this, fuente, cantidad);
	}

	/** LivingDamageEvent: el daño final, justo antes de restarlo de la vida. */
	@WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
	private void cloudruleta$danar(LivingEntity yo, float vidaNueva, Operation<Void> original, @Local(argsOnly = true) DamageSource fuente) {
		float calculado = yo.getHealth() - vidaNueva;
		float dano = Ganchos.alDanar(yo, fuente, calculado);
		if (dano == calculado)
			original.call(yo, vidaNueva);
		else if (dano != 0.0F)
			original.call(yo, yo.getHealth() - dano);
	}

	@Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
	private void cloudruleta$soltar(ServerLevel nivel, DamageSource fuente, CallbackInfo ci) {
		Ganchos.alSoltar((LivingEntity) (Object) this, fuente);
	}

	/** LivingUseTotemEvent: Forge lo comprobaba junto a «es un tótem de la inmortalidad». */
	@WrapOperation(method = "checkTotemDeathProtection", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
	private boolean cloudruleta$totem(ItemStack pila, Item item, Operation<Boolean> original, @Local(argsOnly = true) DamageSource fuente) {
		return original.call(pila, item) && Ganchos.alUsarTotem((LivingEntity) (Object) this, fuente, pila);
	}

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void cloudruleta$tick(CallbackInfo ci) {
		if (Ganchos.alTickVivo((LivingEntity) (Object) this))
			ci.cancel();
	}

	@Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
	private void cloudruleta$visibilidad(Entity mira, CallbackInfoReturnable<Double> cir) {
		cir.setReturnValue(Ganchos.visibilidad((LivingEntity) (Object) this, mira, cir.getReturnValueD()));
	}

	/** LivingEntityUseItemEvent.Finish: después de finishUsingItem, con una copia del objeto de antes. */
	@WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack cloudruleta$terminarDeUsar(ItemStack pila, Level nivel, LivingEntity quien, Operation<ItemStack> original) {
		ItemStack copia = pila.copy();
		ItemStack resultado = original.call(pila, nivel, quien);
		return Ganchos.alTerminarDeUsar(quien, copia, resultado);
	}
}
