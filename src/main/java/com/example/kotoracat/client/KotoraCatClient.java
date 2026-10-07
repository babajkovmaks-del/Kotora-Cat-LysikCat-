package com.example.kotoracat.client;

import com.example.kotoracat.KotoraCatMod;
import com.example.kotoracat.entity.KotoraCatEntity;
import com.example.kotoracat.registry.KotoraCatEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KotoraCatMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KotoraCatClient {
    private KotoraCatClient() {}

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(KotoraCatModel.LAYER_LOCATION, KotoraCatModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KotoraCatEntities.KOTORA_CAT.get(), KotoraCatRenderer::new);
    }

    public static final class KotoraCatRenderer extends MobRenderer<KotoraCatEntity, KotoraCatModel> {
        private static final ResourceLocation TEXTURE =
                new ResourceLocation(KotoraCatMod.MODID, "textures/entity/kotora_cat.png");

        public KotoraCatRenderer(EntityRendererProvider.Context context) {
            super(context, new KotoraCatModel(context.bakeLayer(KotoraCatModel.LAYER_LOCATION)), 0.35F);
        }

        private static final ResourceLocation GOOD_TEXTURE =
                new ResourceLocation(KotoraCatMod.MODID, "textures/entity/kotora_cat_good.png");
        private static final ResourceLocation EVIL_TEXTURE =
                new ResourceLocation(KotoraCatMod.MODID, "textures/entity/kotora_cat_evil.png");

        @Override
        public ResourceLocation getTextureLocation(KotoraCatEntity entity) {
            return entity.isEvil() ? EVIL_TEXTURE : GOOD_TEXTURE;
        }
    }
}
