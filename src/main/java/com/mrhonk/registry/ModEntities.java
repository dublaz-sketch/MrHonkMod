package com.mrhonk.registry;

import com.mrhonk.MrHonkMod;
import com.mrhonk.entity.CreamPieEntity;
import com.mrhonk.entity.MrHonkEntity;
import com.mrhonk.entity.ThrowingKnifeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MrHonkMod.MODID);

    public static final RegistryObject<EntityType<MrHonkEntity>> MR_HONK = ENTITIES.register("mr_honk",
            () -> EntityType.Builder.of(MrHonkEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(12)
                    .build("mr_honk"));

    public static final RegistryObject<EntityType<CreamPieEntity>> CREAM_PIE = ENTITIES.register("cream_pie",
            () -> EntityType.Builder.<CreamPieEntity>of(CreamPieEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("cream_pie"));

    public static final RegistryObject<EntityType<ThrowingKnifeEntity>> THROWING_KNIFE = ENTITIES.register("throwing_knife",
            () -> EntityType.Builder.<ThrowingKnifeEntity>of(ThrowingKnifeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("throwing_knife"));

    private ModEntities() {}
}
