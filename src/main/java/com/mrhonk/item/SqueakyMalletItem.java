package com.mrhonk.item;

import com.mrhonk.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** Marreta de palhaço: dano baixo, empurrão enorme e um "PIII" a cada golpe. */
public class SqueakyMalletItem extends SwordItem {
    public SqueakyMalletItem(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.knockback(1.2D, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.SQUEAK.get(),
                SoundSource.PLAYERS, 1.0F, 0.8F + attacker.getRandom().nextFloat() * 0.4F);
        return super.hurtEnemy(stack, target, attacker);
    }
}
