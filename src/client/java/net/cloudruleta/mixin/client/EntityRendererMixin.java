package net.cloudruleta.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.cloudruleta.compat.Bus;
import net.cloudruleta.compat.Event;
import net.cloudruleta.compat.client.RenderNameTagEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RenderNameTagEvent de Forge 1.20.1: se lanzaba al principio de EntityRenderer.render; con resultado DENY no se dibuja
 * el nombre, con ALLOW se dibuja aunque shouldShowName diga que no, y el texto puede cambiarse.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Unique
	private RenderNameTagEvent cloudruleta$evento;

	@Inject(method = "render", at = @At("HEAD"))
	private void cloudruleta$nombre(Entity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
							   CallbackInfo ci) {
		if (!Bus.hasListeners(RenderNameTagEvent.class)) {
			cloudruleta$evento = null;
			return;
		}
		RenderNameTagEvent e = new RenderNameTagEvent(entity, entity.getDisplayName(), (EntityRenderer<?>) (Object) this, poseStack, bufferSource,
				packedLight, partialTick);
		Bus.post(e);
		cloudruleta$evento = e;
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;shouldShowName(Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean cloudruleta$mostrarNombre(EntityRenderer<?> renderer, Entity entity, Operation<Boolean> original) {
		RenderNameTagEvent e = cloudruleta$evento;
		if (e != null && e.getResult() == Event.Result.DENY)
			return false;
		if (e != null && e.getResult() == Event.Result.ALLOW)
			return true;
		return original.call(renderer, entity);
	}

	@ModifyArg(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V"),
			index = 1)
	private Component cloudruleta$textoNombre(Component texto) {
		RenderNameTagEvent e = cloudruleta$evento;
		return e != null && e.getContent() != null ? e.getContent() : texto;
	}
}
