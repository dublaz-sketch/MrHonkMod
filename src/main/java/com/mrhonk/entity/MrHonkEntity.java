package com.mrhonk.entity;

import com.mrhonk.registry.ModItems;
import com.mrhonk.registry.ModSounds;
import com.mrhonk.util.HonkConfig;
import com.mrhonk.util.HonkData;
import com.mrhonk.util.HonkSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Mr. Honk.
 *
 * Modos:
 *  HIDE  – fica parado escondido, espiando. Se você olhar direto para ele, você o "acha".
 *  STALK – estilo dweller: aparece longe, congela quando você olha, anda quando você vira as costas,
 *          teleporta para trás de você.
 *  HUNT  – persegue e ataca. Fase 2 = marreta e tortas; fase 3 = machado e facas, apaga tochas.
 */
public class MrHonkEntity extends Monster {

    private static final EntityDataAccessor<Integer> STAGE =
            SynchedEntityData.defineId(MrHonkEntity.class, EntityDataSerializers.INT);

    public static final int MODE_HIDE = 0;
    public static final int MODE_STALK = 1;
    public static final int MODE_HUNT = 2;

    @Nullable private UUID targetUUID;
    private int mode = MODE_HIDE;
    private int life;
    private int lookTicks;
    private int watchedTicks;
    private int huntTicks;
    private int attackCooldown;
    private int rangedCooldown = 40;
    private boolean finished;

    public MrHonkEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 30;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 96.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STAGE, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Só arromba portas caçando na fase 3
        this.goalSelector.addGoal(1, new BreakDoorGoal(this, d -> d != Difficulty.PEACEFUL) {
            @Override
            public boolean canUse() {
                return MrHonkEntity.this.mode == MODE_HUNT
                        && MrHonkEntity.this.getStage() >= HonkData.STAGE_REAL
                        && HonkConfig.GRIEFING.get()
                        && super.canUse();
            }
        });
    }

    // ------------------------------------------------------------------ setup

    public void setup(Player target, int stage) {
        this.targetUUID = target.getUUID();
        setStage(stage);
        this.mode = stage == HonkData.STAGE_HIDE ? MODE_HIDE : MODE_STALK;
    }

    public int getStage() { return this.entityData.get(STAGE); }

    @Nullable public UUID getTargetUUID() { return targetUUID; }

    private void setStage(int stage) {
        this.entityData.set(STAGE, stage);
        double hp, dmg, speed;
        ItemStack hand = ItemStack.EMPTY;
        switch (stage) {
            case HonkData.STAGE_CLOWN -> { hp = 60; dmg = 4; speed = 0.31; hand = new ItemStack(ModItems.SQUEAKY_MALLET.get()); }
            case HonkData.STAGE_REAL -> { hp = 100; dmg = 6; speed = 0.33; hand = new ItemStack(Items.IRON_AXE); }
            default -> { hp = 40; dmg = 3; speed = 0.30; }
        }
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(dmg);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        setHealth(getMaxHealth());
        setItemSlot(EquipmentSlot.MAINHAND, hand);
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(stage >= HonkData.STAGE_REAL);
        }
    }

    @Nullable
    private Player resolveTarget() {
        if (targetUUID == null) {
            // Veio de um ovo de spawn ou comando de summon: escolhe o jogador mais perto
            Player near = level().getNearestPlayer(this, 64);
            if (near == null || near.isSpectator()) return null;
            setup(near, HonkData.getStage(near));
            return near;
        }
        return level().getPlayerByUUID(targetUUID);
    }

    // ------------------------------------------------------------------ olhar

    /** Mira em cima dele (como o Enderman verifica). */
    public boolean isLookedAtDirectly(Player p) {
        Vec3 look = p.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(getX() - p.getX(), getEyeY() - 0.4 - p.getEyeY(), getZ() - p.getZ());
        double d = to.length();
        if (d < 0.01) return true;
        double dot = look.dot(to.normalize());
        double threshold = Math.max(0.965, 1.0 - 0.09 / d);
        return dot > threshold && p.hasLineOfSight(this);
    }

    /** Está dentro do campo de visão do jogador (para "congelar" como um dweller). */
    public boolean isInFieldOfView(Player p) {
        Vec3 look = p.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(getX() - p.getX(), getEyeY() - p.getEyeY(), getZ() - p.getZ()).normalize();
        return look.dot(to) > 0.55 && p.hasLineOfSight(this);
    }

    // ------------------------------------------------------------------ tick

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (finished) return;
        life++;
        if (attackCooldown > 0) attackCooldown--;
        if (rangedCooldown > 0) rangedCooldown--;

        Player p = resolveTarget();
        if (p == null) {
            if (life > 100) vanish(false, null);
            return;
        }
        if (!p.isAlive()) { vanish(true, p); return; }
        if (p.isSpectator() || p.level() != level()) { vanish(false, null); return; }

        double dist = distanceTo(p);
        if (dist > 150) { vanish(false, null); return; }

        getLookControl().setLookAt(p, 40.0F, 40.0F);

        switch (mode) {
            case MODE_HIDE -> tickHide(p, dist);
            case MODE_STALK -> tickStalk(p, dist);
            default -> tickHunt(p, dist);
        }
    }

    private void tickHide(Player p, double dist) {
        getNavigation().stop();
        if (dist < 64 && isLookedAtDirectly(p)) {
            if (++lookTicks >= 8) { onFound(p); return; }
        } else {
            lookTicks = Math.max(0, lookTicks - 1);
        }

        // Pista: uma buzina de vez em quando
        if (life % 300 == 150) {
            playSound(ModSounds.HONK.get(), 1.6F, 0.85F + random.nextFloat() * 0.3F);
        }
        // Chegou perto sem olhar? Ele ri e muda de esconderijo
        if (dist < 5 && !isInFieldOfView(p)) {
            playSound(ModSounds.LAUGH.get(), 0.6F, 1.4F);
            BlockPos spot = HonkSpawner.findHideSpot(p, random, 12, 28);
            if (spot != null) {
                poof();
                teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
            }
        }
        if (life > 20 * 60 * 3) { // 3 minutos
            p.displayClientMessage(Component.translatable("msg.mrhonk.gave_up").withStyle(ChatFormatting.GRAY), true);
            vanish(false, p);
        }
    }

    private void tickStalk(Player p, double dist) {
        int stage = getStage();
        boolean watched = isInFieldOfView(p);

        if (dist < 96 && isLookedAtDirectly(p)) {
            if (++lookTicks >= 12) { onFound(p); return; }
        } else {
            lookTicks = Math.max(0, lookTicks - 1);
        }

        if (watched) {
            // Congela enquanto é observado
            getNavigation().stop();
            watchedTicks++;
            if (stage == HonkData.STAGE_WATCH && watchedTicks > 100) {
                vanish(false, p); // some se você ficar encarando sem "mirar" nele
                return;
            }
        } else {
            watchedTicks = 0;
            if (dist > 4 && tickCount % 10 == 0) {
                getNavigation().moveTo(p, 1.0 + 0.15 * stage);
            }
            // Aparece atrás de você
            if (dist > 20 && random.nextInt(120) == 0) {
                teleportNear(p, 9, 16);
            }
            if (random.nextInt(400) == 0) {
                playSound(ModSounds.HONK.get(), 1.2F, 0.6F);
            }
        }

        if (dist < 3.5 && !watched) {
            if (stage == HonkData.STAGE_WATCH) {
                jumpscare(p);
                vanish(false, p);
            } else {
                startHunt(p);
            }
            return;
        }

        if (life > 20 * 120) { // 2 minutos observando
            if (stage == HonkData.STAGE_WATCH) vanish(false, p);
            else startHunt(p);
        }
    }

    private void tickHunt(Player p, double dist) {
        int stage = getStage();
        huntTicks++;
        setTarget(p);
        boolean watched = isInFieldOfView(p);

        // Mais lento quando você olha, rápido quando vira as costas
        if (tickCount % 5 == 0) {
            getNavigation().moveTo(p, watched ? 0.65 : 1.35);
        }

        if (dist < 2.4 && attackCooldown <= 0 && hasLineOfSight(p)) {
            swing(InteractionHand.MAIN_HAND);
            if (doHurtTarget(p) && stage == HonkData.STAGE_CLOWN) {
                playSound(ModSounds.SQUEAK.get(), 1.0F, 0.8F + random.nextFloat() * 0.4F);
            }
            attackCooldown = stage >= HonkData.STAGE_REAL ? 18 : 24;
        }

        if (dist > 5 && dist < 22 && rangedCooldown <= 0 && hasLineOfSight(p)) {
            throwAt(p, stage >= HonkData.STAGE_REAL);
            rangedCooldown = stage >= HonkData.STAGE_REAL ? 45 : 70;
        }

        if (stage >= HonkData.STAGE_REAL && huntTicks % 40 == 0) {
            snuffLights(p);
        }

        if (dist > 48 || (getNavigation().isDone() && dist > 6 && !watched && random.nextInt(40) == 0)) {
            teleportNear(p, 6, 12);
        }

        if (random.nextInt(160) == 0) {
            playSound(ModSounds.LAUGH.get(), 1.4F, stage >= HonkData.STAGE_REAL ? 0.7F : 1.0F);
        }

        if (huntTicks > 20 * 180) { // 3 minutos sem pegar você
            p.displayClientMessage(Component.translatable("msg.mrhonk.escaped").withStyle(ChatFormatting.GRAY), true);
            vanish(true, p);
        }
    }

    // ------------------------------------------------------------------ eventos

    private void onFound(Player p) {
        if (finished || level().isClientSide) return;
        int stageBefore = HonkData.getStage(p);
        int count = HonkData.addFound(p);
        int stageAfter = HonkData.stageFor(count);

        level().playSound(null, getX(), getY(), getZ(), ModSounds.FOUND.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        p.displayClientMessage(Component.translatable("msg.mrhonk.found", count).withStyle(ChatFormatting.GOLD), true);

        if (stageAfter > stageBefore && p instanceof ServerPlayer sp) {
            ChatFormatting color = stageAfter >= HonkData.STAGE_CLOWN ? ChatFormatting.DARK_RED : ChatFormatting.RED;
            HonkSpawner.title(sp,
                    Component.translatable("title.mrhonk.stage" + stageAfter).withStyle(color),
                    Component.translatable("msg.mrhonk.stage" + stageAfter).withStyle(ChatFormatting.GRAY));
            sp.playNotifySound(ModSounds.STINGER.get(), SoundSource.HOSTILE, 0.8F, 1.0F);
        }

        if (getStage() >= HonkData.STAGE_CLOWN) {
            startHunt(p);
        } else {
            vanish(false, p);
        }
    }

    private void startHunt(Player p) {
        if (mode == MODE_HUNT) return;
        mode = MODE_HUNT;
        huntTicks = 0;
        rangedCooldown = 30;
        setTarget(p);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.LAUGH.get(), SoundSource.HOSTILE, 3.0F, 0.9F);
        p.displayClientMessage(Component.translatable("msg.mrhonk.hunt").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        if (getStage() >= HonkData.STAGE_REAL) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
        }
        if (p instanceof ServerPlayer sp) {
            sp.playNotifySound(ModSounds.STINGER.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
        }
    }

    private void jumpscare(Player p) {
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        if (p instanceof ServerPlayer sp) {
            sp.playNotifySound(ModSounds.STINGER.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            sp.playNotifySound(ModSounds.LAUGH.get(), SoundSource.HOSTILE, 1.0F, 0.7F);
        }
    }

    private void throwAt(Player p, boolean knife) {
        ThrowableItemProjectile proj = knife ? new ThrowingKnifeEntity(level(), this) : new CreamPieEntity(level(), this);
        double dx = p.getX() - getX();
        double dy = p.getEyeY() - 1.1 - proj.getY();
        double dz = p.getZ() - getZ();
        double h = Math.sqrt(dx * dx + dz * dz) * 0.2;
        proj.shoot(dx, dy + h, dz, knife ? 1.7F : 1.3F, knife ? 3.0F : 6.0F);
        swing(InteractionHand.OFF_HAND);
        playSound(knife ? ModSounds.LAUGH.get() : ModSounds.HONK.get(), 0.8F, 1.3F);
        level().addFreshEntity(proj);
    }

    /** Fase 3: apaga as luzes perto de você. */
    private void snuffLights(Player p) {
        if (!HonkConfig.GRIEFING.get() || !ForgeEventFactory.getMobGriefingEvent(level(), this)) return;
        int broken = 0;
        BlockPos c = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-7, -3, -7), c.offset(7, 4, 7))) {
            BlockState s = level().getBlockState(pos);
            if (isLight(s)) {
                level().destroyBlock(pos, true, this);
                if (++broken >= 2) break;
            }
        }
        if (broken > 0) playSound(ModSounds.LAUGH.get(), 1.0F, 0.6F);
    }

    private static boolean isLight(BlockState s) {
        return s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH)
                || s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH)
                || s.is(Blocks.REDSTONE_TORCH) || s.is(Blocks.REDSTONE_WALL_TORCH)
                || s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_LANTERN)
                || s.is(BlockTags.CANDLES);
    }

    private boolean teleportNear(Player p, double minD, double maxD) {
        Vec3 look = p.getViewVector(1.0F);
        double back = Math.atan2(-look.z, -look.x);
        boolean surface = HonkSpawner.useSurface(p);
        for (int i = 0; i < 24; i++) {
            double ang = back + (random.nextDouble() - 0.5) * Math.PI * 0.9;
            double d = minD + random.nextDouble() * (maxD - minD);
            int x = Mth.floor(p.getX() + Math.cos(ang) * d);
            int z = Mth.floor(p.getZ() + Math.sin(ang) * d);
            BlockPos pos = HonkSpawner.findStandable(level(), x, z, p.getBlockY(), surface);
            if (pos != null && Math.abs(pos.getY() - p.getY()) < 8) {
                teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                getNavigation().stop();
                return true;
            }
        }
        return false;
    }

    private void poof() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 25, 0.4, 0.8, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 10, 0.3, 0.6, 0.3, 0.01);
        }
    }

    /** Desaparece e agenda a próxima visita. */
    private void vanish(boolean laugh, @Nullable Player p) {
        if (finished) return;
        finished = true;
        poof();
        if (laugh) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.LAUGH.get(), SoundSource.HOSTILE, 2.0F, 0.8F);
        }
        if (p != null) {
            HonkData.scheduleNext(p, HonkData.getStage(p), 0);
        }
        discard();
    }

    // ------------------------------------------------------------------ interação / dano

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && mode != MODE_HUNT) {
            if (player.getUUID().equals(targetUUID) || targetUUID == null) {
                if (targetUUID == null) setup(player, HonkData.getStage(player));
                onFound(player);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (level().isClientSide) return false;

        if (mode != MODE_HUNT) {
            if (source.getEntity() instanceof Player p && (p.getUUID().equals(targetUUID) || targetUUID == null)) {
                if (targetUUID == null) setup(p, HonkData.getStage(p));
                if (getStage() < HonkData.STAGE_CLOWN) {
                    onFound(p);   // nas fases 0 e 1 bater nele também conta como "achar"
                    return false;
                }
                onFound(p);       // fase 2+: conta e começa a caçar
            } else {
                return false;
            }
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt) playSound(ModSounds.SQUEAK.get(), 1.0F, 0.6F);
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && targetUUID != null) {
            Player p = level().getPlayerByUUID(targetUUID);
            if (p != null) {
                p.sendSystemMessage(Component.translatable("msg.mrhonk.killed").withStyle(ChatFormatting.DARK_PURPLE));
                HonkData.scheduleNext(p, HonkData.getStage(p), 20 * 60 * 5); // folga extra de 5 min
            }
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    protected boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public boolean isPushable() { return mode == MODE_HUNT; }

    // ------------------------------------------------------------------ sons

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return mode == MODE_HUNT ? ModSounds.LAUGH.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.SQUEAK.get(); }

    @Override
    protected SoundEvent getDeathSound() { return ModSounds.HONK.get(); }

    @Override
    public float getVoicePitch() { return 0.7F + random.nextFloat() * 0.2F; }

    // ------------------------------------------------------------------ NBT

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetUUID != null) tag.putUUID("HonkTarget", targetUUID);
        tag.putInt("HonkStage", getStage());
        tag.putInt("HonkMode", mode);
        tag.putInt("HonkLife", life);
        tag.putInt("HonkHunt", huntTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("HonkTarget")) targetUUID = tag.getUUID("HonkTarget");
        float hp = getHealth();
        setStage(tag.getInt("HonkStage"));
        if (tag.contains("Health")) setHealth(Math.min(hp, getMaxHealth()));
        mode = tag.getInt("HonkMode");
        life = tag.getInt("HonkLife");
        huntTicks = tag.getInt("HonkHunt");
    }
}
