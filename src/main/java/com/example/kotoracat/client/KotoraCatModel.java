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
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.RenderType;

/** A deliberately simple, self-contained sphynx-style model with its own UV atlas. */
public class KotoraCatModel extends EntityModel<KotoraCatEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new net.minecraft.resources.ResourceLocation(KotoraCatMod.MODID, "kotora_cat"), "main");

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
                .texOffs(0, 16).addBox(-2, -1, -5, 4, 3, 1) // muzzle
                .texOffs(16, 0).addBox(-3, -6, -2, 2, 2, 2) // right ear
                .texOffs(16, 0).addBox(1, -6, -2, 2, 2, 2), // left ear
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
    public void setupAnim(KotoraCatEntity cat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);
        this.head.xRot = headPitch * ((float)Math.PI / 180F);
        float walk = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
        this.rightFrontLeg.xRot = walk;
        this.leftHindLeg.xRot = walk;
        this.leftFrontLeg.xRot = -walk;
        this.rightHindLeg.xRot = -walk;
        this.tail.yRot = Mth.sin(ageInTicks * 0.15F) * 0.25F;
        if (cat.isEnraged()) {
            this.tail.xRot = -0.65F;
        } else {
            this.tail.xRot = -0.35F;
        }
    }
}
