package net.ruleta.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.ruleta.compat.Ganchos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;
import java.util.UUID;

/**
 * EntityJoinLevelEvent en el servidor: al añadir una entidad al gestor de entidades (nueva o cargada del disco),
 * justo después de comprobar que su UUID no está repetido, como hacía Forge.
 */
@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin<T extends EntityAccess> {
	@Shadow
	@Final
	Set<UUID> knownUuids;

	@WrapOperation(method = "addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addEntityUuid(Lnet/minecraft/world/level/entity/EntityAccess;)Z"))
	private boolean ruleta$unirse(PersistentEntitySectionManager<T> yo, T entidad, Operation<Boolean> original,
								  @Local(argsOnly = true) boolean cargada) {
		if (!original.call(yo, entidad))
			return false;
		if (entidad instanceof Entity e && Ganchos.alUnirse(e, cargada)) {
			this.knownUuids.remove(e.getUUID());
			return false;
		}
		return true;
	}
}
