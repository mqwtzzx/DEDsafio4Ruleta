package net.cloudruleta.compat.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.cloudruleta.compat.Event;
import org.jetbrains.annotations.Nullable;

/** FillBucketEvent de Forge: al usar un cubo (lo publica el mixin de BucketItem.use). Cancelable. */
public final class FillBucketEvent extends Event {
	private final Player player;
	private final ItemStack current;
	private final Level level;
	@Nullable
	private final HitResult target;

	public FillBucketEvent(Player player, ItemStack current, Level level, @Nullable HitResult target) {
		this.player = player;
		this.current = current;
		this.level = level;
		this.target = target;
	}

	@Override
	public boolean isCancelable() {
		return true;
	}

	public Player getEntity() {
		return player;
	}

	public ItemStack getEmptyBucket() {
		return current;
	}

	public Level getLevel() {
		return level;
	}

	@Nullable
	public HitResult getTarget() {
		return target;
	}
}
