package net.ruleta.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.ruleta.compat.Bus;
import net.ruleta.compat.event.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge lanzaba BlockEvent.EntityMultiPlaceEvent con todos los bloques que cambian al formarse un gólem
 * (la calabaza y los bloques de hierro/nieve que desaparecen). Aquí se lanza al ir a formarse: cancelado,
 * no se forma el gólem (la calabaza queda puesta). El «jugador» del evento es el más cercano (8 bloques).
 */
@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
	@Shadow
	private BlockPattern getOrCreateSnowGolemFull() {
		throw new AssertionError();
	}

	@Shadow
	private BlockPattern getOrCreateIronGolemFull() {
		throw new AssertionError();
	}

	@Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
	private void ruleta$golem(Level nivel, BlockPos pos, CallbackInfo ci) {
		if (!Bus.hasListeners(BlockEvent.EntityMultiPlaceEvent.class))
			return;
		BlockPattern.BlockPatternMatch m = this.getOrCreateSnowGolemFull().find(nivel, pos);
		if (m == null)
			m = this.getOrCreateIronGolemFull().find(nivel, pos);
		if (m == null)
			return;
		List<BlockEvent.BlockSnapshot> fotos = new ArrayList<>();
		for (int i = 0; i < m.getWidth(); i++)
			for (int j = 0; j < m.getHeight(); j++)
				for (int k = 0; k < m.getDepth(); k++) {
					BlockInWorld b = m.getBlock(i, j, k);
					fotos.add(new BlockEvent.BlockSnapshot(b.getPos(), b.getState()));
				}
		Player cerca = nivel.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 8.0D, false);
		if (Bus.post(new BlockEvent.EntityMultiPlaceEvent(nivel, pos, nivel.getBlockState(pos), cerca, fotos)))
			ci.cancel();
	}
}
