package net.ruleta.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.ruleta.compat.Bus;
import net.ruleta.compat.Ganchos;
import net.ruleta.compat.event.ExplosionEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/** ExplosionEvent.Detonate: con los bloques y entidades afectados ya calculados, antes de dañar a nadie. */
@Mixin(Explosion.class)
public abstract class ExplosionMixin implements Ganchos.ExplosionCancelable {
	@Shadow
	@Final
	private Level level;
	@Unique
	private boolean ruleta$cancelada;

	@Override
	public boolean ruleta$cancelada() {
		return this.ruleta$cancelada;
	}

	@Override
	public void ruleta$cancelar() {
		this.ruleta$cancelada = true;
	}

	@ModifyExpressionValue(method = "explode", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
	private List<Entity> ruleta$detonar(List<Entity> afectadas) {
		if (!Bus.hasListeners(ExplosionEvent.Detonate.class))
			return afectadas;
		List<Entity> lista = new ArrayList<>(afectadas);
		Bus.post(new ExplosionEvent.Detonate(this.level, (Explosion) (Object) this, lista));
		return lista;
	}
}
