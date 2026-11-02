package com.mrhonk.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mrhonk.MrHonkMod;
import com.mrhonk.entity.MrHonkEntity;
import com.mrhonk.util.HonkConfig;
import com.mrhonk.util.HonkData;
import com.mrhonk.util.HonkSpawner;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MrHonkMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HonkEvents {

    /** Gerencia as aparições: a cada segundo, vê se está na hora do Mr. Honk aparecer. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        if (p.tickCount % 20 != 0) return;
        if (!HonkConfig.ENABLED.get() || p.isSpectator()) return;
        if (p.isCreative() && !HonkConfig.TARGET_CREATIVE.get()) return;

        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        long next = HonkData.getNext(p);
        if (next == 0) {                       // primeira vez: dá 1 minuto de paz
            HonkData.setNext(p, now + 1200);
            return;
        }
        if (now < next) return;
        if (HonkSpawner.findFor(p) != null) return;

        int stage = HonkData.getStage(p);
        // Nas fases de caça ele prefere a noite ou cavernas
        if (stage >= HonkData.STAGE_CLOWN && level.isDay() && level.canSeeSky(p.blockPosition())
                && p.getRandom().nextFloat() > 0.25F) {
            HonkData.setNext(p, now + 1200);
            return;
        }

        boolean ok = HonkSpawner.spawnFor(p, stage);
        // Trava de segurança; o próprio Mr. Honk reagenda quando vai embora
        HonkData.setNext(p, now + (ok ? 20 * 60 * 6 : 200));
    }

    /** Garante que o contador sobrevive à morte. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        HonkData.putRaw(event.getEntity(), HonkData.copyRaw(event.getOriginal()));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mrhonk")
                .then(Commands.literal("achados").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int n = HonkData.getCount(p);
                    int stage = HonkData.stageFor(n);
                    ctx.getSource().sendSuccess(() -> Component.translatable("cmd.mrhonk.count", n,
                            Component.translatable("stage.mrhonk." + stage)), false);
                    return n;
                }))
                .then(Commands.literal("definir").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("quantidade", IntegerArgumentType.integer(0, 999)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int n = IntegerArgumentType.getInteger(ctx, "quantidade");
                            HonkData.setCount(p, n);
                            int stage = HonkData.stageFor(n);
                            ctx.getSource().sendSuccess(() -> Component.translatable("cmd.mrhonk.count", n,
                                    Component.translatable("stage.mrhonk." + stage)), false);
                            return n;
                        })))
                .then(Commands.literal("invocar").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    MrHonkEntity old = HonkSpawner.findFor(p);
                    if (old != null) old.discard();
                    boolean ok = HonkSpawner.spawnFor(p, HonkData.getStage(p));
                    ctx.getSource().sendSuccess(() -> Component.translatable(ok ? "cmd.mrhonk.spawned" : "cmd.mrhonk.nospot"), false);
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("resetar").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    HonkData.setCount(p, 0);
                    HonkData.setNext(p, 0);
                    ctx.getSource().sendSuccess(() -> Component.translatable("cmd.mrhonk.reset"), false);
                    return 1;
                })));
    }

    private HonkEvents() {}
}
