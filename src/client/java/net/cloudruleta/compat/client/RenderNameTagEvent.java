package net.cloudruleta.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.cloudruleta.compat.Event;

/** RenderNameTagEvent de Forge: al dibujar una entidad, antes de su etiqueta de nombre (lo lanza EntityRendererMixin). */
public final class RenderNameTagEvent extends Event {
	private final Entity entity;
	private Component content;
	private final EntityRenderer<?> entityRenderer;
	private final PoseStack poseStack;
	private final MultiBufferSource multiBufferSource;
	private final int packedLight;
	private final float partialTick;

	public RenderNameTagEvent(Entity entity, Component content, EntityRenderer<?> entityRenderer, PoseStack poseStack, MultiBufferSource multiBufferSource,
							  int packedLight, float partialTick) {
		this.entity = entity;
		this.content = content;
		this.entityRenderer = entityRenderer;
		this.poseStack = poseStack;
		this.multiBufferSource = multiBufferSource;
		this.packedLight = packedLight;
		this.partialTick = partialTick;
	}

	public Entity getEntity() {
		return entity;
	}

	public Component getContent() {
		return content;
	}

	public void setContent(Component content) {
		this.content = content;
	}

	public EntityRenderer<?> getEntityRenderer() {
		return entityRenderer;
	}

	public PoseStack getPoseStack() {
		return poseStack;
	}

	public MultiBufferSource getMultiBufferSource() {
		return multiBufferSource;
	}

	public int getPackedLight() {
		return packedLight;
	}

	public float getPartialTick() {
		return partialTick;
	}
}
