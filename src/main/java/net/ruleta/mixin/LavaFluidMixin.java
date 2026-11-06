package net.ruleta.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.LavaFluid;
import net.ruleta.compat.Bus;
import net.ruleta.compat.event.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** BlockEvent.FluidPlaceBlockEvent cuando la lava cae sobre agua y forma piedra. */
@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {
	@WrapOperation(method = "spreadTo", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private boolean ruleta$piedra(LevelAccessor nivel, BlockPos pos, BlockState estado, int banderas, Operation<Boolean> original) {
		BlockEvent.FluidPlaceBlockEvent e = new BlockEvent.FluidPlaceBlockEvent(nivel, pos, pos, estado);
		if (Bus.post(e))
			return false;
		return original.call(nivel, pos, e.getNewState(), banderas);
	}
}
