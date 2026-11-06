package net.ruleta.puerta;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/** Ladrillo con textura en mosaico de 4×4×4: la posición en el mosaico va en el estado. */
public class LadrilloBunkerBloque extends Block {
	public static final MapCodec<LadrilloBunkerBloque> CODEC = simpleCodec(LadrilloBunkerBloque::new);
	public static final IntegerProperty PX = IntegerProperty.create("px", 0, 3);
	public static final IntegerProperty PY = IntegerProperty.create("py", 0, 3);
	public static final IntegerProperty PZ = IntegerProperty.create("pz", 0, 3);

	public LadrilloBunkerBloque(BlockBehaviour.Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(PX, 0).setValue(PY, 0).setValue(PZ, 0));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PX, PY, PZ);
	}

	private BlockState conPosicion(BlockState s, BlockPos pos) {
		return s.setValue(PX, Math.floorMod(pos.getX(), 4)).setValue(PY, Math.floorMod(pos.getY(), 4)).setValue(PZ, Math.floorMod(pos.getZ(), 4));
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return this.conPosicion(this.defaultBlockState(), ctx.getClickedPos());
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState anterior, boolean moviendo) {
		if (!level.isClientSide) {
			BlockState correcto = this.conPosicion(state, pos);
			if (correcto != state)
				level.setBlock(pos, correcto, 2);
		}
		super.onPlace(state, level, pos, anterior, moviendo);
	}
}
