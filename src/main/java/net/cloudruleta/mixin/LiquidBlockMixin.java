package net.cloudruleta.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.cloudruleta.compat.Bus;
import net.cloudruleta.compat.event.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** BlockEvent.FluidPlaceBlockEvent cuando la lava y el agua forman obsidiana, roca o basalto. */
@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {
	@WrapOperation(method = "shouldSpreadLiquid", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean cloudruleta$fluido(Level nivel, BlockPos pos, BlockState estado, Operation<Boolean> original) {
		BlockEvent.FluidPlaceBlockEvent e = new BlockEvent.FluidPlaceBlockEvent(nivel, pos, pos, estado);
		if (Bus.post(e))
			return false;
		return original.call(nivel, pos, e.getNewState());
	}
}
