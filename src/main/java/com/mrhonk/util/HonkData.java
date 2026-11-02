package com.mrhonk.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Guarda, por jogador, quantas vezes ele achou o Mr. Honk e quando ele aparece de novo.
 * Fica no NBT "persistido" do jogador, então sobrevive à morte e ao relogar.
 */
public final class HonkData {
    private static final String KEY = "mrhonk";
    private static final String COUNT = "Found";
    private static final String NEXT = "NextSpawn";

    public static final int STAGE_HIDE = 0;     // esconde-esconde
    public static final int STAGE_WATCH = 1;    // observador (dweller)
    public static final int STAGE_CLOWN = 2;    // caça com armas de palhaço
    public static final int STAGE_REAL = 3;     // caça com armas reais

    private static CompoundTag read(Player p) {
        CompoundTag persist = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persist.getCompound(KEY);
    }

    private static void write(Player p, CompoundTag tag) {
        CompoundTag persist = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persist.put(KEY, tag);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persist);
    }

    public static CompoundTag copyRaw(Player p) { return read(p).copy(); }
    public static void putRaw(Player p, CompoundTag tag) { write(p, tag); }

    public static int getCount(Player p) { return read(p).getInt(COUNT); }

    public static void setCount(Player p, int value) {
        CompoundTag t = read(p);
        t.putInt(COUNT, Math.max(0, value));
        write(p, t);
    }

    public static int addFound(Player p) {
        int n = getCount(p) + 1;
        setCount(p, n);
        return n;
    }

    public static long getNext(Player p) { return read(p).getLong(NEXT); }

    public static void setNext(Player p, long gameTime) {
        CompoundTag t = read(p);
        t.putLong(NEXT, gameTime);
        write(p, t);
    }

    public static int stageFor(int count) {
        if (count >= HonkConfig.STAGE3_AT.get()) return STAGE_REAL;
        if (count >= HonkConfig.STAGE2_AT.get()) return STAGE_CLOWN;
        if (count >= HonkConfig.STAGE1_AT.get()) return STAGE_WATCH;
        return STAGE_HIDE;
    }

    public static int getStage(Player p) { return stageFor(getCount(p)); }

    /** Agenda a próxima aparição. Quanto mais avançada a fase, mais rápido ele volta. */
    public static void scheduleNext(Player p, int stage, int extraTicks) {
        int min, max;
        switch (stage) {
            case STAGE_HIDE -> { min = 1800; max = 3600; }   // 1,5 a 3 min
            case STAGE_WATCH -> { min = 1800; max = 3000; }
            case STAGE_CLOWN -> { min = 1500; max = 3000; }
            default -> { min = 1200; max = 2400; }           // 1 a 2 min
        }
        int delay = min + p.getRandom().nextInt(max - min + 1);
        delay = (int) Math.round(delay * HonkConfig.DELAY_MULTIPLIER.get()) + extraTicks;
        setNext(p, p.level().getGameTime() + delay);
    }

    private HonkData() {}
}
