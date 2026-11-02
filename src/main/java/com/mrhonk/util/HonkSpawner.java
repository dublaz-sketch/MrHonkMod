package com.mrhonk.util;

import com.mrhonk.entity.MrHonkEntity;
import com.mrhonk.registry.ModEntities;
import com.mrhonk.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public final class HonkSpawner {

    /** Procura o Mr. Honk que está "assombrando" esse jogador. */
    @Nullable
    public static MrHonkEntity findFor(Player p) {
        List<MrHonkEntity> list = p.level().getEntitiesOfClass(MrHonkEntity.class,
                p.getBoundingBox().inflate(160), h -> p.getUUID().equals(h.getTargetUUID()));
        return list.isEmpty() ? null : list.get(0);
    }

    /** Cria um Mr. Honk para o jogador, escolhendo o lugar de acordo com a fase. */
    public static boolean spawnFor(ServerPlayer p, int stage) {
        ServerLevel level = p.serverLevel();
        BlockPos pos = stage == HonkData.STAGE_HIDE
                ? findHideSpot(p, p.getRandom(), 14, 36)
                : findStalkSpot(p, p.getRandom(), 28, 48);
        if (pos == null) return false;

        MrHonkEntity honk = ModEntities.MR_HONK.get().create(level);
        if (honk == null) return false;

        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
        float yaw = (float) (Mth.atan2(p.getZ() - z, p.getX() - x) * (180F / Math.PI)) - 90F;
        honk.moveTo(x, pos.getY(), z, yaw, 0F);
        honk.setYHeadRot(yaw);
        honk.setup(p, stage);
        level.addFreshEntity(honk);

        // Avisos para o jogador
        if (stage == HonkData.STAGE_HIDE) {
            p.displayClientMessage(Component.translatable("msg.mrhonk.hiding").withStyle(ChatFormatting.YELLOW), true);
            level.playSound(null, honk.getX(), honk.getY(), honk.getZ(), ModSounds.HONK.get(), SoundSource.HOSTILE, 2.5F, 1.0F);
        } else if (stage >= HonkData.STAGE_REAL) {
            p.playNotifySound(ModSounds.STINGER.get(), SoundSource.HOSTILE, 0.6F, 0.6F);
        }
        return true;
    }

    // ---------------------------------------------------------------- lugares

    /** True quando o jogador está em caverna / Nether: procura lugares na altura dele em vez da superfície. */
    public static boolean useSurface(Player p) {
        Level l = p.level();
        if (l.dimensionType().hasCeiling()) return false;
        int surface = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getBlockX(), p.getBlockZ());
        return l.canSeeSky(p.blockPosition()) || p.getBlockY() >= surface - 6;
    }

    @Nullable
    public static BlockPos findStandable(Level level, int x, int z, int refY, boolean surface) {
        if (surface) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            return isStandable(level, p) ? p : null;
        }
        for (int dy = 0; dy <= 10; dy++) {
            BlockPos up = new BlockPos(x, refY + dy, z);
            if (isStandable(level, up)) return up;
            BlockPos down = new BlockPos(x, refY - dy, z);
            if (isStandable(level, down)) return down;
        }
        return null;
    }

    public static boolean isStandable(Level l, BlockPos p) {
        if (!l.isLoaded(p) || p.getY() <= l.getMinBuildHeight() || p.getY() >= l.getMaxBuildHeight() - 2) return false;
        BlockState below = l.getBlockState(p.below());
        if (!below.isFaceSturdy(l, p.below(), Direction.UP)) return false;
        BlockState feet = l.getBlockState(p);
        BlockState head = l.getBlockState(p.above());
        return feet.getCollisionShape(l, p).isEmpty()
                && head.getCollisionShape(l, p.above()).isEmpty()
                && l.getFluidState(p).isEmpty()
                && l.getFluidState(p.above()).isEmpty();
    }

    public static boolean canSee(Level l, Player p, Vec3 target) {
        HitResult r = l.clip(new ClipContext(p.getEyePosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return r.getType() == HitResult.Type.MISS;
    }

    /**
     * Esconde-esconde: prefere lugares onde só a cabeça aparece (espiando atrás de um bloco/árvore),
     * depois lugares totalmente escondidos.
     */
    @Nullable
    public static BlockPos findHideSpot(Player p, RandomSource r, double minD, double maxD) {
        Level l = p.level();
        boolean surface = useSurface(p);
        BlockPos best = null;
        int bestScore = -1;
        for (int i = 0; i < 64; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double d = minD + r.nextDouble() * (maxD - minD);
            int x = Mth.floor(p.getX() + Math.cos(ang) * d);
            int z = Mth.floor(p.getZ() + Math.sin(ang) * d);
            BlockPos pos = findStandable(l, x, z, p.getBlockY(), surface);
            if (pos == null || Math.abs(pos.getY() - p.getY()) > 20) continue;

            Vec3 head = new Vec3(pos.getX() + 0.5, pos.getY() + 1.75, pos.getZ() + 0.5);
            Vec3 body = new Vec3(pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5);
            boolean headVis = canSee(l, p, head);
            boolean bodyVis = canSee(l, p, body);
            int score;
            if (headVis && !bodyVis) score = 4;        // espiando
            else if (!headVis && !bodyVis) score = 3;  // escondido de verdade
            else if (!headVis) score = 2;
            else score = 0;                            // à vista
            if (l.getMaxLocalRawBrightness(pos) < 8) score++; // gosta do escuro

            if (score > bestScore) {
                bestScore = score;
                best = pos;
                if (score >= 4 && i > 12) break;
            }
        }
        return best;
    }

    /** Observador: aparece longe, de preferência atrás do jogador e num lugar aberto (para ser visto ao virar). */
    @Nullable
    public static BlockPos findStalkSpot(Player p, RandomSource r, double minD, double maxD) {
        Level l = p.level();
        boolean surface = useSurface(p);
        if (!surface) { minD = Math.min(minD, 12); maxD = Math.min(maxD, 24); }
        Vec3 look = p.getViewVector(1.0F);
        double back = Math.atan2(-look.z, -look.x);
        BlockPos fallback = null;
        for (int i = 0; i < 48; i++) {
            double ang = back + (r.nextDouble() - 0.5) * Math.PI * 1.2;
            double d = minD + r.nextDouble() * (maxD - minD);
            int x = Mth.floor(p.getX() + Math.cos(ang) * d);
            int z = Mth.floor(p.getZ() + Math.sin(ang) * d);
            BlockPos pos = findStandable(l, x, z, p.getBlockY(), surface);
            if (pos == null || Math.abs(pos.getY() - p.getY()) > 24) continue;
            Vec3 head = new Vec3(pos.getX() + 0.5, pos.getY() + 1.75, pos.getZ() + 0.5);
            if (canSee(l, p, head)) return pos;
            if (fallback == null) fallback = pos;
        }
        return fallback;
    }

    /** Mostra um título grande na tela. */
    public static void title(ServerPlayer p, Component title, Component subtitle) {
        if (!HonkConfig.SHOW_TITLES.get()) {
            p.sendSystemMessage(title.copy().append(" ").append(subtitle));
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private HonkSpawner() {}
}
