package net.ruleta.mixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.ruleta.compat.Bus;
import net.ruleta.compat.Ganchos;
import net.ruleta.compat.event.MobSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** MobSpawnEvent.FinalizeSpawn al empezar Mob.finalizeSpawn, y la marca de «aparición anulada». */
@Mixin(Mob.class)
public abstract class MobMixin implements Ganchos.AparicionAnulable {
	@Unique
	private boolean ruleta$anulada;

	@Override
	public boolean ruleta$aparicionAnulada() {
		return this.ruleta$anulada;
	}

	@Override
	public void ruleta$anularAparicion(boolean valor) {
		this.ruleta$anulada = valor;
	}

	@Inject(method = "finalizeSpawn", at = @At("HEAD"), cancellable = true)
	private void ruleta$finalizar(ServerLevelAccessor nivel, DifficultyInstance dificultad, MobSpawnType razon, SpawnGroupData datos,
								   CallbackInfoReturnable<SpawnGroupData> cir) {
		if (!Bus.hasListeners(MobSpawnEvent.FinalizeSpawn.class))
			return;
		Mob yo = (Mob) (Object) this;
		MobSpawnEvent.FinalizeSpawn e = new MobSpawnEvent.FinalizeSpawn(yo, nivel, yo.getX(), yo.getY(), yo.getZ(), dificultad, razon);
		boolean cancelado = Bus.post(e);
		if (e.isSpawnCancelled())
			this.ruleta$anulada = true;
		if (cancelado)
			cir.setReturnValue(datos);
	}
}
