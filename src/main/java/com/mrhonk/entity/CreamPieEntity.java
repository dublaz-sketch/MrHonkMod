package com.mrhonk.entity;

import com.mrhonk.registry.ModEntities;
import com.mrhonk.registry.ModItems;
import com.mrhonk.registry.ModSounds;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Torta de creme: pouco dano, mas cega e deixa lento. */
public class CreamPieEntity extends ThrowableItemProjectile {

    public CreamPieEntity(EntityType<? extends CreamPieEntity> type, Level level) {
        super(type, level);
    }

    public CreamPieEntity(Level level, LivingEntity owner) {
        super(ModEntities.CREAM_PIE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CREAM_PIE.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        target.hurt(damageSources().thrown(this, getOwner()), 2.0F);
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 1));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 3);
            playSound(SoundEvents.SLIME_SQUISH, 1.0F, 1.2F);
            playSound(ModSounds.SQUEAK.get(), 0.6F, 1.5F);
            discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ItemParticleOption particle = new ItemParticleOption(ParticleTypes.ITEM, getItem());
            for (int i = 0; i < 12; i++) {
                level().addParticle(particle, getX(), getY(), getZ(),
                        (random.nextDouble() - 0.5) * 0.2, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.2);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
}
