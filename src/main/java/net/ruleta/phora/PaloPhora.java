package net.ruleta.phora;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Clic derecho: un meteorito de los Phora cae donde miras (hasta 160 bloques). */
public class PaloPhora extends Item {
	private static final double ALCANCE = 160.0D;

	public PaloPhora(Item.Properties p) {
		super(p);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
		ItemStack palo = jugador.getItemInHand(mano);
		if (!nivel.isClientSide && nivel instanceof ServerLevel sl) {
			Vec3 ojos = jugador.getEyePosition();
			Vec3 fin = ojos.add(jugador.getViewVector(1.0F).scale(ALCANCE));
			BlockHitResult hit = nivel.clip(new ClipContext(ojos, fin, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, jugador));
			Vec3 objetivo = hit.getType() == HitResult.Type.MISS ? fin : hit.getLocation();
			MeteoritoEntity.lanzar(sl, objetivo, jugador.getRandom(), jugador.position());
			jugador.getCooldowns().addCooldown(this, 20);
		}
		return InteractionResultHolder.sidedSuccess(palo, nivel.isClientSide);
	}

	@Override
	public boolean isFoil(ItemStack s) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack s, Item.TooltipContext contexto, List<Component> lineas, TooltipFlag f) {
		lineas.add(Component.literal("Clic derecho: un meteorito de los Phora cae donde miras").withStyle(ChatFormatting.GOLD));
	}
}
