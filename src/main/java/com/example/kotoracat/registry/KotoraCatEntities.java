package com.example.kotoracat.registry;

import com.example.kotoracat.KotoraCatMod;
import com.example.kotoracat.entity.KotoraCatEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class KotoraCatEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KotoraCatMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KotoraCatMod.MODID);

    public static final RegistryObject<EntityType<KotoraCatEntity>> KOTORA_CAT = ENTITY_TYPES.register("kotora_cat",
            () -> EntityType.Builder.of(KotoraCatEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.7F)
                    .clientTrackingRange(10)
                    .build("kotora_cat"));

    public static final RegistryObject<Item> KOTORA_CAT_SPAWN_EGG = ITEMS.register("kotora_cat_spawn_egg",
            () -> new SpawnEggItem(KOTORA_CAT, 0x8C6F65, 0xE8D8CF, new Item.Properties()));

    private KotoraCatEntities() {}
}
