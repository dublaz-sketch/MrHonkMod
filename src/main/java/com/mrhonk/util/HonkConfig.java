package com.mrhonk.util;

import net.minecraftforge.common.ForgeConfigSpec;

/** Arquivo gerado em config/mrhonk-common.toml */
public final class HonkConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue STAGE1_AT;
    public static final ForgeConfigSpec.IntValue STAGE2_AT;
    public static final ForgeConfigSpec.IntValue STAGE3_AT;
    public static final ForgeConfigSpec.DoubleValue DELAY_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue GRIEFING;
    public static final ForgeConfigSpec.BooleanValue TARGET_CREATIVE;
    public static final ForgeConfigSpec.BooleanValue SHOW_TITLES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("geral");
        ENABLED = b.comment("Liga/desliga o aparecimento automático do Mr. Honk.")
                .define("ativado", true);
        TARGET_CREATIVE = b.comment("Mr. Honk também aparece para jogadores no modo criativo (bom para testar).")
                .define("aparecerNoCriativo", false);
        SHOW_TITLES = b.comment("Mostra títulos na tela ao mudar de fase.")
                .define("mostrarTitulos", true);
        b.pop();

        b.push("fases");
        STAGE1_AT = b.comment("Quantas vezes achar o Mr. Honk para ele virar OBSERVADOR (estilo dweller).")
                .defineInRange("fase1Observador", 3, 1, 1000);
        STAGE2_AT = b.comment("Quantas vezes achar para ele começar a CAÇAR com armas de palhaço.")
                .defineInRange("fase2ArmasDePalhaco", 7, 2, 1000);
        STAGE3_AT = b.comment("Quantas vezes achar para ele trazer ARMAS DE VERDADE.")
                .defineInRange("fase3ArmasReais", 12, 3, 1000);
        b.pop();

        b.push("aparicoes");
        DELAY_MULTIPLIER = b.comment("Multiplica o intervalo entre aparições (0.5 = duas vezes mais frequente).")
                .defineInRange("multiplicadorIntervalo", 1.0, 0.1, 10.0);
        GRIEFING = b.comment("Na fase 3 ele apaga tochas/lanternas perto de você e arromba portas (respeita a regra mobGriefing).")
                .define("quebrarLuzesEPortas", true);
        b.pop();

        SPEC = b.build();
    }

    private HonkConfig() {}
}
