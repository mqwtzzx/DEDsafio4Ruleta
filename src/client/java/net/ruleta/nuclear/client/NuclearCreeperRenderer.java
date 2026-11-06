package net.ruleta.nuclear.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.ruleta.nuclear.entity.NuclearCreeperEntity;

public class NuclearCreeperRenderer extends MobRenderer<NuclearCreeperEntity, NuclearCreeperModel<NuclearCreeperEntity>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("nuclearcreeper", "textures/entity/nuclear_creeper.png");
	private static final float BASE_SCALE = 0.85F;

	public NuclearCreeperRenderer(EntityRendererProvider.Context context) {
		super(context, new NuclearCreeperModel<>(context.bakeLayer(NuclearCreeperModel.LAYER_LOCATION)), 0.5F);
	}

	@Override
	protected void scale(NuclearCreeperEntity entity, PoseStack pose, float partialTick) {
		float swell = entity.getSwelling(partialTick);
		float wobble = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
		swell = Mth.clamp(swell, 0.0F, 1.0F);
		swell *= swell;
		swell *= swell;
		float sxz = (1.0F + swell * 0.4F) * wobble;
		float sy = (1.0F + swell * 0.1F) / wobble;
		pose.scale(sxz * BASE_SCALE, sy * BASE_SCALE, sxz * BASE_SCALE);
	}

	@Override
	protected float getWhiteOverlayProgress(NuclearCreeperEntity entity, float partialTick) {
		float swell = entity.getSwelling(partialTick);
		return (int) (swell * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(swell, 0.5F, 1.0F);
	}

	@Override
	public ResourceLocation getTextureLocation(NuclearCreeperEntity entity) {
		return TEXTURE;
	}
}
