package net.ruleta.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.ruleta.compat.Bus;
import net.ruleta.compat.Event;
import net.ruleta.compat.event.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PlayerInteractEvent.RightClickBlock (en el servidor): al empezar useItemOn. Cancelado devuelve su resultado;
 * setUseBlock(DENY) salta el uso del bloque y setUseItem(DENY) el del objeto (p. ej. colocar), como en Forge.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
	@Unique
	private PlayerInteractEvent.RightClickBlock ruleta$clic;

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void ruleta$clicDerecho(ServerPlayer jugador, Level nivel, ItemStack pila, InteractionHand mano, BlockHitResult impacto,
									CallbackInfoReturnable<InteractionResult> cir) {
		this.ruleta$clic = null;
		PlayerInteractEvent.RightClickBlock e = new PlayerInteractEvent.RightClickBlock(jugador, mano, impacto);
		if (Bus.post(e)) {
			cir.setReturnValue(e.getCancellationResult());
			return;
		}
		this.ruleta$clic = e;
	}

	@WrapOperation(method = "useItemOn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/ItemInteractionResult;"))
	private ItemInteractionResult ruleta$usarBloqueConObjeto(BlockState estado, ItemStack pila, Level nivel, Player jugador, InteractionHand mano, BlockHitResult impacto,
															  Operation<ItemInteractionResult> original) {
		if (this.ruleta$clic != null && this.ruleta$clic.getUseBlock() == Event.Result.DENY)
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		return original.call(estado, pila, nivel, jugador, mano, impacto);
	}

	@WrapOperation(method = "useItemOn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;useWithoutItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
	private InteractionResult ruleta$usarBloque(BlockState estado, Level nivel, Player jugador, BlockHitResult impacto, Operation<InteractionResult> original) {
		if (this.ruleta$clic != null && this.ruleta$clic.getUseBlock() == Event.Result.DENY)
			return InteractionResult.PASS;
		return original.call(estado, nivel, jugador, impacto);
	}

	@WrapOperation(method = "useItemOn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
	private InteractionResult ruleta$usarObjeto(ItemStack pila, UseOnContext contexto, Operation<InteractionResult> original) {
		if (this.ruleta$clic != null && this.ruleta$clic.getUseItem() == Event.Result.DENY)
			return InteractionResult.PASS;
		return original.call(pila, contexto);
	}

	@Inject(method = "useItemOn", at = @At("RETURN"))
	private void ruleta$finClic(ServerPlayer jugador, Level nivel, ItemStack pila, InteractionHand mano, BlockHitResult impacto,
								CallbackInfoReturnable<InteractionResult> cir) {
		this.ruleta$clic = null;
	}
}
