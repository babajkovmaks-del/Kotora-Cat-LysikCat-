package com.example.kotoracat;

import com.example.kotoracat.registry.KotoraCatEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KotoraCatMod.MODID)
public class KotoraCatMod {
    public static final String MODID = "kotoracat";

    public KotoraCatMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        KotoraCatEntities.ENTITY_TYPES.register(bus);
        KotoraCatEntities.ITEMS.register(bus);
        bus.addListener(KotoraCatMod::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(KotoraCatEntities.KOTORA_CAT.get(), com.example.kotoracat.entity.KotoraCatEntity.createAttributes().build());
    }
}
