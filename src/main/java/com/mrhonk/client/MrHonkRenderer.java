package com.mrhonk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrhonk.MrHonkMod;
import com.mrhonk.entity.MrHonkEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MrHonkRenderer extends HumanoidMobRenderer<MrHonkEntity, HumanoidModel<MrHonkEntity>> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(MrHonkMod.MODID, "mr_honk"), "main");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MrHonkMod.MODID, "textures/entity/mr_honk.png");

    public MrHonkRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(LAYER)), 0.5F);
        this.addLayer(new HonkEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(MrHonkEntity entity) {
        return TEXTURE;
    }

    /** Ele fica mais alto a cada fase... */
    @Override
    protected void scale(MrHonkEntity entity, PoseStack pose, float partialTicks) {
        float s = 1.0F + 0.06F * entity.getStage();
        pose.scale(s, s, s);
    }
}
