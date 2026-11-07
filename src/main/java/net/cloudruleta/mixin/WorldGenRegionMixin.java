package net.cloudruleta.mixin;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.cloudruleta.compat.Ganchos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Criaturas de la generación del mundo (CHUNK_GENERATION) con la aparición anulada en MobSpawnEvent: no se añaden. */
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
	private void cloudruleta$anulada(Entity entidad, CallbackInfoReturnable<Boolean> cir) {
		if (entidad instanceof Mob mob && ((Ganchos.AparicionAnulable) mob).cloudruleta$aparicionAnulada())
			cir.setReturnValue(false);
	}
}
