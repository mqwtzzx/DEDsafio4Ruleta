package net.ruleta.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.ruleta.compat.Bus;
import net.ruleta.compat.Ganchos;
import net.ruleta.compat.event.ExplosionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** ExplosionEvent.Start: tras crear la explosión y antes de calcularla. Cancelada no hace nada. */
@Mixin(Level.class)
public abstract class LevelMixin {
	@WrapOperation(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;"
				+ "Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;Z"
				+ "Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)"
				+ "Lnet/minecraft/world/level/Explosion;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Explosion;explode()V"))
	private void ruleta$empezar(Explosion explosion, Operation<Void> original) {
		if (Bus.post(new ExplosionEvent.Start((Level) (Object) this, explosion))) {
			((Ganchos.ExplosionCancelable) explosion).ruleta$cancelar();
			return;
		}
		original.call(explosion);
	}

	@WrapWithCondition(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;"
				+ "Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;Z"
				+ "Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)"
				+ "Lnet/minecraft/world/level/Explosion;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Explosion;finalizeExplosion(Z)V"))
	private boolean ruleta$finalizar(Explosion explosion, boolean particulas) {
		return !((Ganchos.ExplosionCancelable) explosion).ruleta$cancelada();
	}
}
