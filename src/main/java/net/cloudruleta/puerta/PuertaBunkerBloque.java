package net.cloudruleta.puerta;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.cloudruleta.bunker.Bunkers;
import org.jetbrains.annotations.Nullable;

/**
 * Puerta de 8 de ancho × 11 de alto. Cada celda guarda su desplazamiento (DX, DY) respecto al origen;
 * el origen (0,0) lleva la entidad de bloque. Abierta, las columnas 1..6 quedan huecas.
 */
public class PuertaBunkerBloque extends BaseEntityBlock {
	public static final MapCodec<PuertaBunkerBloque> CODEC = simpleCodec(PuertaBunkerBloque::new);
	public static final int ANCHO = 8;
	public static final int ALTO = 11;
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 7);
	public static final IntegerProperty DY = IntegerProperty.create("dy", 0, 10);
	public static final boolean ABRE_HACIA_ATRAS = true;
	private static final int LARGO_ABIERTA = 3;
	private static final VoxelShape LOSA_Z = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
	private static final VoxelShape LOSA_X = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
	private static boolean desmontando = false;

	public PuertaBunkerBloque(BlockBehaviour.Properties props) {
		super(props);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, Boolean.FALSE).setValue(DX, 0).setValue(DY, 0));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, DX, DY);
	}

	public static Direction derecha(Direction frente) {
		return frente.getCounterClockWise();
	}

	public static BlockPos origen(BlockPos pos, BlockState s) {
		return pos.relative(derecha(s.getValue(FACING)).getOpposite(), s.getValue(DX)).below(s.getValue(DY));
	}

	public static BlockPos celda(BlockPos origen, Direction frente, int dx, int dy) {
		return origen.relative(derecha(frente), dx).above(dy);
	}

	private boolean esDeEstaPuerta(BlockState s, Direction frente, int dx, int dy) {
		return s.is(this) && s.getValue(FACING) == frente && s.getValue(DX) == dx && s.getValue(DY) == dy;
	}

	private static boolean esHueco(BlockState s) {
		int dx = s.getValue(DX);
		return s.getValue(OPEN) && dx >= 1 && dx <= 6;
	}

	public static Direction haciaAbierta(Direction frente) {
		return frente.getOpposite();
	}

	private boolean colocarHojas(Level level, BlockPos o, Direction frente) {
		Direction hacia = haciaAbierta(frente);
		BlockState hoja = PuertaBunker.BLOQUE_HOJA.get().defaultBlockState();
		boolean pendiente = false;
		for (int dx = 0; dx < ANCHO; dx += 7) {
			for (int k = 1; k <= LARGO_ABIERTA; k++) {
				for (int dy = 0; dy < ALTO; dy++) {
					BlockPos p = celda(o, frente, dx, dy).relative(hacia, k);
					if (!level.isLoaded(p) || !level.getBlockState(p).canBeReplaced())
						continue;
					if (!level.isUnobstructed(hoja, p, CollisionContext.empty())) {
						pendiente = true;
						continue;
					}
					level.setBlock(p, hoja, 2);
				}
			}
		}
		return pendiente;
	}

	private void quitarHojas(Level level, BlockPos o, Direction frente) {
		Direction hacia = haciaAbierta(frente);
		for (int dx = 0; dx < ANCHO; dx += 7)
			for (int k = 1; k <= LARGO_ABIERTA; k++)
				for (int dy = 0; dy < ALTO; dy++) {
					BlockPos p = celda(o, frente, dx, dy).relative(hacia, k);
					if (level.isLoaded(p) && level.getBlockState(p).is(PuertaBunker.BLOQUE_HOJA.get()))
						level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
				}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(OPEN) && state.getValue(DX) == 0 && state.getValue(DY) == 0 && this.colocarHojas(level, pos, state.getValue(FACING)))
			level.scheduleTick(pos, this, 10);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Direction frente = ctx.getHorizontalDirection().getOpposite();
		BlockPos o = ctx.getClickedPos();
		Level level = ctx.getLevel();
		if (o.getY() + ALTO > level.getMaxBuildHeight())
			return null;
		for (int dx = 0; dx < ANCHO; dx++) {
			for (int dy = 0; dy < ALTO; dy++) {
				if (dx == 0 && dy == 0)
					continue;
				BlockPos p = celda(o, frente, dx, dy);
				if (!level.isInWorldBounds(p) || !level.getBlockState(p).canBeReplaced(ctx))
					return null;
			}
		}
		return this.defaultBlockState().setValue(FACING, frente).setValue(OPEN, Boolean.FALSE).setValue(DX, 0).setValue(DY, 0);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity quien, ItemStack stack) {
		if (level.isClientSide)
			return;
		Direction frente = state.getValue(FACING);
		for (int dx = 0; dx < ANCHO; dx++)
			for (int dy = 0; dy < ALTO; dy++) {
				if (dx == 0 && dy == 0)
					continue;
				level.setBlock(celda(pos, frente, dx, dy), state.setValue(DX, dx).setValue(DY, dy), 2);
			}
		if (level.getBlockEntity(pos) instanceof PuertaBunkerBE be && this.hayRedstone(level, pos, frente)) {
			be.setAlimentada(true);
			this.poner(level, pos, frente, true);
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide && !player.isCreative())
			Block.popResource(level, pos, new ItemStack(PuertaBunker.ITEM.get()));
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock()) && !desmontando && !level.isClientSide) {
			desmontando = true;
			try {
				Direction frente = state.getValue(FACING);
				BlockPos o = origen(pos, state);
				this.quitarHojas(level, o, frente);
				for (int dx = 0; dx < ANCHO; dx++)
					for (int dy = 0; dy < ALTO; dy++) {
						BlockPos p = celda(o, frente, dx, dy);
						if (!p.equals(pos) && this.esDeEstaPuerta(level.getBlockState(p), frente, dx, dy))
							level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
					}
			} finally {
				desmontando = false;
			}
		}
		super.onRemove(state, level, pos, newState, moving);
	}

	/** En 1.20.1 era use(...); en 1.21.1 la interacción sin objeto es useWithoutItem (con objeto pasa aquí igual). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.isCreative() && !player.hasPermissions(2))
			return InteractionResult.PASS;
		if (!level.isClientSide) {
			BlockPos o = origen(pos, state);
			if (Bunkers.enCaja(level, o)) {
				player.displayClientMessage(Component.literal("La puerta del bunker solo se abre y se cierra con los Momentos Reviil.").withStyle(ChatFormatting.RED), true);
				return InteractionResult.CONSUME;
			}
			this.poner(level, o, state.getValue(FACING), !state.getValue(OPEN));
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block bloque, BlockPos desde, boolean moviendo) {
		if (level.isClientSide || desmontando)
			return;
		BlockPos o = origen(pos, state);
		Direction frente = state.getValue(FACING);
		if (!(level.getBlockEntity(o) instanceof PuertaBunkerBE be))
			return;
		if (Bunkers.enCaja(level, o))
			return;
		boolean alimentada = this.hayRedstone(level, o, frente);
		if (alimentada != be.estaAlimentada()) {
			be.setAlimentada(alimentada);
			this.poner(level, o, frente, alimentada);
		}
	}

	private boolean hayRedstone(Level level, BlockPos o, Direction frente) {
		for (int dx = 0; dx < ANCHO; dx++)
			for (int dy = 0; dy < ALTO; dy++)
				if (level.hasNeighborSignal(celda(o, frente, dx, dy)))
					return true;
		return false;
	}

	public static boolean ponerPuerta(Level level, BlockPos origen, boolean abierta) {
		BlockState s = level.getBlockState(origen);
		if (s.getBlock() instanceof PuertaBunkerBloque b) {
			b.poner(level, origen(origen, s), s.getValue(FACING), abierta);
			return true;
		}
		return false;
	}

	private void poner(Level level, BlockPos o, Direction frente, boolean abierta) {
		boolean cambio = false;
		for (int dx = 0; dx < ANCHO; dx++)
			for (int dy = 0; dy < ALTO; dy++) {
				BlockPos p = celda(o, frente, dx, dy);
				BlockState s = level.getBlockState(p);
				if (this.esDeEstaPuerta(s, frente, dx, dy) && s.getValue(OPEN) != abierta) {
					level.setBlock(p, s.setValue(OPEN, abierta), 2);
					cambio = true;
				}
			}
		if (!cambio)
			return;
		if (abierta)
			level.scheduleTick(o, this, 60);
		else
			this.quitarHojas(level, o, frente);
		BlockPos centro = celda(o, frente, 4, 5);
		level.playSound(null, centro, abierta ? PuertaBunker.SONIDO_ABRIR.get() : PuertaBunker.SONIDO_CERRAR.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
		level.gameEvent(null, abierta ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, centro);
	}

	private static VoxelShape losa(BlockState s) {
		return s.getValue(FACING).getAxis() == Direction.Axis.Z ? LOSA_Z : LOSA_X;
	}

	@Override
	protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return esHueco(s) ? Shapes.empty() : losa(s);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return esHueco(s) ? Shapes.empty() : losa(s);
	}

	@Override
	protected int getLightBlock(BlockState s, BlockGetter level, BlockPos pos) {
		return esHueco(s) ? 0 : 15;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState s, BlockGetter level, BlockPos pos) {
		return esHueco(s);
	}

	@Override
	protected RenderShape getRenderShape(BlockState s) {
		return RenderShape.INVISIBLE;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(DX) == 0 && state.getValue(DY) == 0 ? new PuertaBunkerBE(pos, state) : null;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? createTickerHelper(type, PuertaBunker.BE.get(), PuertaBunkerBE::tickCliente) : null;
	}
}
