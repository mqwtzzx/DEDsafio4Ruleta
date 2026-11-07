package net.cloudruleta.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.cloudruleta.compat.Bus;
import net.cloudruleta.compat.Ganchos;
import net.cloudruleta.compat.event.PlayerEvent;
import net.cloudruleta.compat.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Player.actuallyHurt no llama al de LivingEntity: mismos eventos de daño. Más PlayerTickEvent y NameFormat. */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
	private float cloudruleta$herir(float cantidad, @Local(argsOnly = true) DamageSource fuente) {
		return Ganchos.alHerir((Player) (Object) this, fuente, cantidad);
	}

	@WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"))
	private void cloudruleta$danar(Player yo, float vidaNueva, Operation<Void> original, @Local(argsOnly = true) DamageSource fuente) {
		float calculado = yo.getHealth() - vidaNueva;
		float dano = Ganchos.alDanar(yo, fuente, calculado);
		if (dano == calculado)
			original.call(yo, vidaNueva);
		else if (dano != 0.0F)
			original.call(yo, yo.getHealth() - dano);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void cloudruleta$tickAntes(CallbackInfo ci) {
		Ganchos.tickJugador((Player) (Object) this, TickEvent.Phase.START);
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void cloudruleta$tickDespues(CallbackInfo ci) {
		Ganchos.tickJugador((Player) (Object) this, TickEvent.Phase.END);
	}

	/** PlayerEvent.NameFormat: el nombre base antes de aplicarle el equipo (como el displayname de Forge). */
	@ModifyArg(method = "getDisplayName", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/scores/PlayerTeam;formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"), index = 1)
	private Component cloudruleta$nombre(Component nombre) {
		if (!Bus.hasListeners(PlayerEvent.NameFormat.class))
			return nombre;
		PlayerEvent.NameFormat e = new PlayerEvent.NameFormat((Player) (Object) this, nombre);
		Bus.post(e);
		return e.getDisplayname();
	}
}
