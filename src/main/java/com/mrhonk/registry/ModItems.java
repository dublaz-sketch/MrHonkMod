package com.mrhonk.registry;

import com.mrhonk.MrHonkMod;
import com.mrhonk.item.ClownNoseItem;
import com.mrhonk.item.CreamPieItem;
import com.mrhonk.item.SqueakyMalletItem;
import com.mrhonk.item.ThrowingKnifeItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MrHonkMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MrHonkMod.MODID);

    // ---- Armas de palhaço ----
    public static final RegistryObject<Item> CREAM_PIE = ITEMS.register("cream_pie",
            () -> new CreamPieItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SQUEAKY_MALLET = ITEMS.register("squeaky_mallet",
            () -> new SqueakyMalletItem(Tiers.WOOD, 3, -2.6F, new Item.Properties()));

    // ---- Armas "de verdade" ----
    public static final RegistryObject<Item> THROWING_KNIFE = ITEMS.register("throwing_knife",
            () -> new ThrowingKnifeItem(new Item.Properties().stacksTo(16)));

    // ---- Troféu ----
    public static final RegistryObject<Item> CLOWN_NOSE = ITEMS.register("clown_nose",
            () -> new ClownNoseItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> MR_HONK_SPAWN_EGG = ITEMS.register("mr_honk_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MR_HONK, 0xF2F2F2, 0xD41E1E, new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("mrhonk",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mrhonk"))
                    .icon(() -> new ItemStack(CLOWN_NOSE.get()))
                    .displayItems((params, output) -> {
                        output.accept(CREAM_PIE.get());
                        output.accept(SQUEAKY_MALLET.get());
                        output.accept(THROWING_KNIFE.get());
                        output.accept(CLOWN_NOSE.get());
                        output.accept(MR_HONK_SPAWN_EGG.get());
                    })
                    .build());

    private ModItems() {}
}
