package net.cloudruleta.nuclear.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AbsorptionHeartItem extends Item {
	public static final int DURATION_TICKS = 6000;
	public static final int AMPLIFIER = 4;

	public AbsorptionHeartItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (!level.isClientSide)
			entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, DURATION_TICKS, AMPLIFIER));
		return super.finishUsingItem(stack, level, entity);
	}
}
