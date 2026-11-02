package com.mrhonk.entity;

import com.mrhonk.registry.ModEntities;
import com.mrhonk.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Faca de arremesso: arma "de verdade" da fase 3. */
public class ThrowingKnifeEntity extends ThrowableItemProjectile {

    public ThrowingKnifeEntity(EntityType<? extends ThrowingKnifeEntity> type, Level level) {
        super(type, level);
    }

    public ThrowingKnifeEntity(Level level, LivingEntity owner) {
        super(ModEntities.THROWING_KNIFE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.THROWING_KNIFE.get();
    }

    @Override
    protected float getGravity() {
        return 0.02F;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        float damage = getOwner() instanceof MrHonkEntity ? 7.0F : 6.0F;
        if (target.hurt(damageSources().thrown(this, getOwner()), damage)) {
            playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.3F);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (result.getType() == HitResult.Type.BLOCK) {
                playSound(SoundEvents.TRIDENT_HIT_GROUND, 1.0F, 1.5F);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.1);
                }
            }
            discard();
        }
    }
}
