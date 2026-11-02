package com.mrhonk.client;

import com.mrhonk.MrHonkMod;
import com.mrhonk.registry.ModEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MrHonkMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MrHonkRenderer.LAYER, ClientSetup::createHonkLayer);
    }

    /** Modelo humanoide + chapéu-coco (aba e copa) preso na cabeça. */
    private static LayerDefinition createHonkLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("bowler_hat", CubeListBuilder.create()
                        // aba: 11 x 1 x 11, logo acima do cabelo
                        .texOffs(0, 32).addBox(-5.5F, -9.5F, -5.5F, 11.0F, 1.0F, 11.0F)
                        // copa: 7 x 4 x 7
                        .texOffs(0, 44).addBox(-3.5F, -13.5F, -3.5F, 7.0F, 4.0F, 7.0F),
                PartPose.rotation(-0.08F, 0.0F, 0.06F)); // levemente torto, mais sinistro
        return LayerDefinition.create(mesh, 64, 64);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MR_HONK.get(), MrHonkRenderer::new);
        event.registerEntityRenderer(ModEntities.CREAM_PIE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWING_KNIFE.get(), ThrownItemRenderer::new);
    }

    private ClientSetup() {}
}
