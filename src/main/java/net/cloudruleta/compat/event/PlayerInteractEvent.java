package net.cloudruleta.compat.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.cloudruleta.compat.Event;

/** PlayerInteractEvent de Forge (lo publican UseBlockCallback, AttackBlockCallback y UseEntityCallback). */
public abstract class PlayerInteractEvent extends Event {
	private final Player player;
	private final InteractionHand hand;
	private final BlockPos pos;
	private final Direction face;
	private InteractionResult cancellationResult = InteractionResult.PASS;

	protected PlayerInteractEvent(Player player, InteractionHand hand, BlockPos pos, Direction face) {
		this.player = player;
		this.hand = hand;
		this.pos = pos;
		this.face = face;
	}

	@Override
	public boolean isCancelable() {
		return true;
	}

	public Player getEntity() {
		return player;
	}

	public InteractionHand getHand() {
		return hand;
	}

	public ItemStack getItemStack() {
		return hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
	}

	public BlockPos getPos() {
		return pos;
	}

	public Direction getFace() {
		return face;
	}

	public Level getLevel() {
		return player.level();
	}

	public InteractionResult getCancellationResult() {
		return cancellationResult;
	}

	public void setCancellationResult(InteractionResult result) {
		this.cancellationResult = result;
	}

	public static final class EntityInteract extends PlayerInteractEvent {
		private final Entity target;

		public EntityInteract(Player player, InteractionHand hand, Entity target) {
			super(player, hand, target.blockPosition(), null);
			this.target = target;
		}

		public Entity getTarget() {
			return target;
		}
	}

	public static final class EntityInteractSpecific extends PlayerInteractEvent {
		private final Entity target;

		public EntityInteractSpecific(Player player, InteractionHand hand, Entity target) {
			super(player, hand, target.blockPosition(), null);
			this.target = target;
		}

		public Entity getTarget() {
			return target;
		}
	}

	public static final class RightClickBlock extends PlayerInteractEvent {
		private final BlockHitResult hit;
		private Result useBlock = Result.DEFAULT;
		private Result useItem = Result.DEFAULT;

		public RightClickBlock(Player player, InteractionHand hand, BlockHitResult hit) {
			super(player, hand, hit.getBlockPos(), hit.getDirection());
			this.hit = hit;
		}

		public BlockHitResult getHitVec() {
			return hit;
		}

		public Result getUseBlock() {
			return useBlock;
		}

		public void setUseBlock(Result r) {
			this.useBlock = r;
		}

		public Result getUseItem() {
			return useItem;
		}

		public void setUseItem(Result r) {
			this.useItem = r;
		}
	}

	public static final class LeftClickBlock extends PlayerInteractEvent {
		public LeftClickBlock(Player player, BlockPos pos, Direction face) {
			super(player, InteractionHand.MAIN_HAND, pos, face);
		}
	}
}
