package net.cloudruleta.nuclear.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

public class NuclearCreeperModel<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("cloudruleta_nuclearcreeper", "nuclear_creeper"), "main");
	private static final float SWELL_LEAN_BACK = 0.32F;
	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart leg0;
	private final ModelPart leg1;
	private final ModelPart leg2;
	private final ModelPart leg3;

	public NuclearCreeperModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.leg0 = root.getChild("leg0");
		this.leg1 = root.getChild("leg1");
		this.leg2 = root.getChild("leg2");
		this.leg3 = root.getChild("leg3");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition p = mesh.getRoot();
		p.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 19).addBox(-5.0F, -2.0F, -5.0F, 10.0F, 2.0F, 10.0F, new CubeDeformation(0.5F, 0.0F, 0.5F))
				.texOffs(0, 32).addBox(-6.0F, -4.0F, -6.0F, 12.0F, 2.0F, 12.0F)
				.texOffs(48, 0).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 2.0F, 12.0F)
				.texOffs(0, 0).addBox(-6.0F, -13.0F, -6.0F, 12.0F, 7.0F, 12.0F)
				.texOffs(48, 0).addBox(-6.0F, -15.0F, -6.0F, 12.0F, 2.0F, 12.0F)
				.texOffs(80, 19).addBox(-5.0F, -16.0F, -5.0F, 10.0F, 1.0F, 10.0F)
				.texOffs(40, 19).addBox(-5.0F, -18.0F, -5.0F, 10.0F, 2.0F, 10.0F), PartPose.offset(0.0F, 19.0F, 0.0F));
		addLeg(p, "leg0", 0, -5.5F, -5.5F);
		addLeg(p, "leg1", 16, 5.5F, -5.5F);
		addLeg(p, "leg2", 32, -5.5F, 5.5F);
		addLeg(p, "leg3", 48, 5.5F, 5.5F);
		return LayerDefinition.create(mesh, 128, 64);
	}

	private static void addLeg(PartDefinition parent, String name, int u, float x, float z) {
		parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F)
				.texOffs(64, 32).addBox(-2.0F, 3.0F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.offset(x, 19.0F, z));
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.leg0.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		this.leg1.xRot = Mth.cos(limbSwing * 0.6662F + 3.1415927F) * 1.4F * limbSwingAmount;
		this.leg2.xRot = Mth.cos(limbSwing * 0.6662F + 3.1415927F) * 1.4F * limbSwingAmount;
		this.leg3.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		float swell = 0.0F;
		if (entity instanceof Creeper creeper)
			swell = Mth.clamp(creeper.getSwelling(ageInTicks - (float) creeper.tickCount), 0.0F, 1.0F);
		float ease = swell * swell * (3.0F - 2.0F * swell);
		float calm = 1.0F - ease;
		float sway = Mth.sin(ageInTicks * 0.09F) * 0.07F;
		float waddle = Mth.cos(limbSwing * 0.6662F) * 0.06F * limbSwingAmount;
		this.body.zRot = (sway + waddle) * calm;
		this.body.xRot = -SWELL_LEAN_BACK * ease;
	}

	/** 1.21.1: el color llega como un int ARGB (antes cuatro floats). */
	@Override
	public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
		this.root.render(pose, buffer, light, overlay, color);
	}
}
