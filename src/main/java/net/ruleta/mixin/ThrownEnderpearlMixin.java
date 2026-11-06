package net.ruleta.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.portal.DimensionTransition;
import net.ruleta.compat.Bus;
import net.ruleta.compat.event.EntityEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * EntityTeleportEvent.EnderPearl de Forge: al aterrizar la perla de un jugador, antes de teletransportarlo.
 * Cancelado: ni viaje ni daño. Si cambian el daño, se usa ese en lugar de los 5 de vanilla.
 */
@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin {
	@Unique
	private float ruleta$dano = 5.0F;
	@Unique
	private boolean ruleta$cancelada;

	@WrapOperation(method = "onHit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;"))
	private Entity ruleta$viaje(Entity entidad, DimensionTransition transicion, Operation<Entity> original) {
		this.ruleta$cancelada = false;
		this.ruleta$dano = 5.0F;
		if (entidad instanceof ServerPlayer jugador) {
			EntityEvents.EnderPearlEvent e = new EntityEvents.EnderPearlEvent(jugador, (ThrownEnderpearl) (Object) this,
					transicion.pos().x, transicion.pos().y, transicion.pos().z, 5.0F);
			if (Bus.post(e)) {
				this.ruleta$cancelada = true;
				return entidad;
			}
			this.ruleta$dano = e.getAttackDamage();
		}
		return original.call(entidad, transicion);
	}

	@WrapOperation(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean ruleta$dano(Entity entidad, DamageSource fuente, float cantidad, Operation<Boolean> original) {
		if (this.ruleta$cancelada)
			return false;
		return original.call(entidad, fuente, this.ruleta$dano);
	}
}
