package net.ruleta.herobrine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.ruleta.herobrine.HerobrineEntity;

/** Herobrine: la piel de Steve con ojos blancos que brillan. */
public class HerobrineRenderer extends HumanoidMobRenderer<HerobrineEntity, PlayerModel<HerobrineEntity>> {
	private static final ResourceLocation PIEL = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
	static final ResourceLocation OJOS = ResourceLocation.fromNamespaceAndPath("ruleta", "textures/entity/herobrine_ojos.png");

	public HerobrineRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
		this.addLayer(new Ojos(this));
	}

	@Override
	public ResourceLocation getTextureLocation(HerobrineEntity e) {
		return PIEL;
	}

	static final class Ojos extends RenderLayer<HerobrineEntity, PlayerModel<HerobrineEntity>> {
		Ojos(RenderLayerParent<HerobrineEntity, PlayerModel<HerobrineEntity>> padre) {
			super(padre);
		}

		@Override
		public void render(PoseStack pose, MultiBufferSource buffers, int luz, HerobrineEntity e, float lim, float limAmount, float parcial, float edad, float cabezaYaw, float cabezaPitch) {
			VertexConsumer vc = buffers.getBuffer(RenderType.eyes(OJOS));
			// 1.21.1: el color va en un solo int ARGB (antes cuatro floats 1, 1, 1, 1)
			this.getParentModel().renderToBuffer(pose, vc, 15728880, OverlayTexture.NO_OVERLAY, -1);
		}
	}
}
