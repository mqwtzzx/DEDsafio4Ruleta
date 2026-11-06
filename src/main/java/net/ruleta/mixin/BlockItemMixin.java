package net.ruleta.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.ruleta.compat.Bus;
import net.ruleta.compat.event.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** BlockEvent.EntityPlaceEvent al colocar un bloque desde un objeto. Cancelado: no se coloca (y el cliente se resincroniza). */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@WrapOperation(method = "place", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean ruleta$colocar(BlockItem item, BlockPlaceContext contexto, BlockState estado, Operation<Boolean> original) {
		if (Bus.post(new BlockEvent.EntityPlaceEvent(contexto.getLevel(), contexto.getClickedPos(), estado, contexto.getPlayer())))
			return false;
		return original.call(item, contexto, estado);
	}
}
