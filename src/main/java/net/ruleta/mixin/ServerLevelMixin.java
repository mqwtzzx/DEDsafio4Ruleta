package net.ruleta.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.ruleta.compat.Ganchos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Si ExplosionEvent.Start la canceló, no se manda el paquete de explosión a los clientes (ni sonido ni partículas). */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;"
			+ "Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;"
			+ "Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)"
			+ "Lnet/minecraft/world/level/Explosion;",
			at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;"
					+ "Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZ"
					+ "Lnet/minecraft/world/level/Level$ExplosionInteraction;ZLnet/minecraft/core/particles/ParticleOptions;"
					+ "Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/level/Explosion;"),
			cancellable = true)
	private void ruleta$sinPaquete(CallbackInfoReturnable<Explosion> cir, @Local Explosion explosion) {
		if (((Ganchos.ExplosionCancelable) explosion).ruleta$cancelada())
			cir.setReturnValue(explosion);
	}
}
