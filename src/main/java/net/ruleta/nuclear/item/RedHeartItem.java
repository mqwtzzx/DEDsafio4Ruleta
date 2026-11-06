package net.ruleta.nuclear.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ruleta.nuclear.HeartEvents;

public class RedHeartItem extends Item {
	public RedHeartItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide && entity instanceof ServerPlayer player)
			HeartEvents.addHeart(player);
		return super.finishUsingItem(stack, level, entity);
	}
}
