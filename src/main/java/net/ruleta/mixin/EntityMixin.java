package net.ruleta.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.portal.DimensionTransition;
import net.ruleta.compat.DatosPersistentes;
import net.ruleta.compat.Ganchos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Datos persistentes (getPersistentData de Forge) y EntityTravelToDimensionEvent. */
@Mixin(Entity.class)
public abstract class EntityMixin implements DatosPersistentes {
	@Unique
	private CompoundTag ruleta$datos;

	@Override
	public CompoundTag ruleta$datosPersistentes() {
		if (this.ruleta$datos == null)
			this.ruleta$datos = new CompoundTag();
		return this.ruleta$datos;
	}

	@Inject(method = "saveWithoutId", at = @At("RETURN"))
	private void ruleta$guardarDatos(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
		if (this.ruleta$datos != null && !this.ruleta$datos.isEmpty())
			tag.put(DatosPersistentes.CLAVE, this.ruleta$datos.copy());
	}

	@Inject(method = "load", at = @At("RETURN"))
	private void ruleta$cargarDatos(CompoundTag tag, CallbackInfo ci) {
		if (tag.contains(DatosPersistentes.CLAVE, 10))
			this.ruleta$datos = tag.getCompound(DatosPersistentes.CLAVE);
	}

	/** Forge comprobaba el viaje al empezar changeDimension (ServerPlayer tiene su propio mixin). */
	@Inject(method = "changeDimension", at = @At("HEAD"), cancellable = true)
	private void ruleta$viajar(DimensionTransition transicion, CallbackInfoReturnable<Entity> cir) {
		if ((Object) this instanceof ServerPlayer)
			return;
		if (Ganchos.alViajar((Entity) (Object) this, transicion.newLevel().dimension()))
			cir.setReturnValue(null);
	}

	/**
	 * En 1.21.1 el portal de destino se busca/genera antes de changeDimension; Forge avisaba antes de generarlo,
	 * así que el aviso de portales del Nether y del End se adelanta aquí.
	 */
	@WrapOperation(method = "handlePortal", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/PortalProcessor;getPortalDestination(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
	private DimensionTransition ruleta$portal(PortalProcessor portal, ServerLevel nivel, Entity entidad, Operation<DimensionTransition> original) {
		ResourceKey<Level> destino = null;
		if (portal.isSamePortal((Portal) Blocks.NETHER_PORTAL))
			destino = nivel.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
		else if (portal.isSamePortal((Portal) Blocks.END_PORTAL))
			destino = nivel.dimension() == Level.END ? Level.OVERWORLD : Level.END;
		if (destino != null && Ganchos.alEntrarEnPortal(entidad, destino))
			return null;
		return original.call(portal, nivel, entidad);
	}

	@Inject(method = "handlePortal", at = @At("RETURN"))
	private void ruleta$finPortal(CallbackInfo ci) {
		Ganchos.finPortal();
	}
}
