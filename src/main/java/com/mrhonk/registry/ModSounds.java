package com.mrhonk.registry;

import com.mrhonk.MrHonkMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MrHonkMod.MODID);

    /** Buzina de palhaço (pista para achar o Mr. Honk). */
    public static final RegistryObject<SoundEvent> HONK = register("honk");
    /** Risada distorcida. */
    public static final RegistryObject<SoundEvent> LAUGH = register("laugh");
    /** Jingle de "achou!". */
    public static final RegistryObject<SoundEvent> FOUND = register("found");
    /** Susto (jumpscare / mudança de fase). */
    public static final RegistryObject<SoundEvent> STINGER = register("stinger");
    /** Apito de borracha (marreta e dano). */
    public static final RegistryObject<SoundEvent> SQUEAK = register("squeak");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MrHonkMod.MODID, name)));
    }

    private ModSounds() {}
}
