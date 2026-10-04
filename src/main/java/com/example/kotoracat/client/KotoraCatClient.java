package com.example.kotoracat.client;

import com.example.kotoracat.KotoraCatMod;
import com.example.kotoracat.entity.KotoraCatEntity;
import com.example.kotoracat.registry.KotoraCatEntities;
import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
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
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KotoraCatEntities.KOTORA_CAT.get(), KotoraCatRenderer::new);
    }

    public static final class KotoraCatRenderer extends MobRenderer<KotoraCatEntity, CatModel<KotoraCatEntity>> {
        private static final ResourceLocation TEXTURE =
                new ResourceLocation(KotoraCatMod.MODID, "textures/entity/kotora_cat.png");

        public KotoraCatRenderer(EntityRendererProvider.Context context) {
            super(context, new CatModel<>(context.bakeLayer(ModelLayers.CAT)), 0.4F);
        }

        @Override
        public ResourceLocation getTextureLocation(KotoraCatEntity entity) {
            return TEXTURE;
        }
    }
}
