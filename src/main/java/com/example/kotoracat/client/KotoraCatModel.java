package com.example.kotoracat.client;

import com.example.kotoracat.KotoraCatMod;
import com.example.kotoracat.entity.KotoraCatEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Stable custom cat-shaped model with a full 64x64 atlas. */
public class KotoraCatModel extends EntityModel<KotoraCatEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(KotoraCatMod.MODID, "kotora_cat"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart tail;

    public KotoraCatModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -5, -4, 8, 8, 8)
                .texOffs(0, 16).addBox(-2, -1, -5, 4, 3, 1)
                .texOffs(16, 0).addBox(-3, -6, -2, 2, 2, 2)
                .texOffs(20, 0).addBox(1, -6, -2, 2, 2, 2),
                PartPose.offset(0, 14, -5));

        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4, -5, -3, 8, 10, 6),
                PartPose.offsetAndRotation(0, 16, 1, (float)Math.PI / 2F, 0, 0));

        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-1.5F, 0, -1.5F, 3, 7, 3), PartPose.offset(-2.5F, 17, -3));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                .texOffs(12, 24).addBox(-1.5F, 0, -1.5F, 3, 7, 3), PartPose.offset(2.5F, 17, -3));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                .texOffs(24, 24).addBox(-1.5F, 0, -1.5F, 3, 7, 3), PartPose.offset(-2.5F, 17, 4));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                .texOffs(36, 24).addBox(-1.5F, 0, -1.5F, 3, 7, 3), PartPose.offset(2.5F, 17, 4));

        root.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-1, -1, 0, 2, 2, 9),
                PartPose.offsetAndRotation(0, 14, 6, -0.35F, 0, 0));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        rightFrontLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        leftFrontLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        rightHindLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        leftHindLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        tail.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void setupAnim(KotoraCatEntity cat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * ((float)Math.PI / 180F);
        head.xRot = headPitch * ((float)Math.PI / 180F);
        float walk = Mth.cos(limbSwing * 0.6662F) * 0.9F * limbSwingAmount;
        rightFrontLeg.xRot = walk;
        leftHindLeg.xRot = walk;
        leftFrontLeg.xRot = -walk;
        rightHindLeg.xRot = -walk;
        tail.yRot = Mth.sin(ageInTicks * 0.15F) * (cat.isEnraged() ? 0.38F : 0.22F);
        tail.xRot = cat.isEnraged() ? -0.7F : -0.35F;
    }
}
