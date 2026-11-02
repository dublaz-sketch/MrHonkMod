package com.mrhonk;

import com.mrhonk.entity.MrHonkEntity;
import com.mrhonk.registry.ModEntities;
import com.mrhonk.registry.ModItems;
import com.mrhonk.registry.ModSounds;
import com.mrhonk.util.HonkConfig;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MrHonkMod.MODID)
public class MrHonkMod {
    public static final String MODID = "mrhonk";

    public MrHonkMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModSounds.SOUNDS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);

        bus.addListener(this::onAttributes);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HonkConfig.SPEC);
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MR_HONK.get(), MrHonkEntity.createAttributes().build());
    }
}
