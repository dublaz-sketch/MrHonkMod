package com.mrhonk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrhonk.MrHonkMod;
import com.mrhonk.entity.MrHonkEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Olhos e sorriso que brilham no escuro a partir da fase 2. */
public class HonkEyesLayer extends EyesLayer<MrHonkEntity, HumanoidModel<MrHonkEntity>> {
    private static final RenderType EYES =
            RenderType.eyes(new ResourceLocation(MrHonkMod.MODID, "textures/entity/mr_honk_eyes.png"));

    public HonkEyesLayer(RenderLayerParent<MrHonkEntity, HumanoidModel<MrHonkEntity>> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, MrHonkEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.getStage() >= 2) {
            super.render(pose, buffer, light, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
        }
    }
}
