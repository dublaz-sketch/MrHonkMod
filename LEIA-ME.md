# Mr. Honk — mod de terror para Forge 1.20.1

*Onde está o Mr. Honk?* Um palhaço se esconde pelo seu mundo. Cada vez que você o encontra, ele fica mais ousado... até parar de se esconder e começar a caçar você.

## Como gerar o .jar

Você precisa do **Java 17 (JDK)** instalado (ex.: Eclipse Temurin 17) e de internet na primeira vez.

1. Descompacte esta pasta.
2. Abra um terminal dentro da pasta `MrHonkMod`.
3. Rode:
   - Windows: `gradlew build`
   - Linux/Mac: `./gradlew build`
4. Na primeira vez demora (baixa o Minecraft e o Forge). O mod pronto fica em `build/libs/mrhonk-1.0.0.jar`.
5. Copie esse `.jar` para a pasta `mods` do seu Minecraft com **Forge 1.20.1** (versão 47.x).

Para testar direto do código: `gradlew runClient`.

## Como funciona

O contador de "achados" é guardado no seu jogador (não zera quando você morre).

| Fase | Quantas vezes você achou | O que ele faz |
|---|---|---|
| 0 – Esconde-esconde | 0 a 2 | Aparece escondido a 14–36 blocos, de preferência só com a cabeça para fora atrás de árvores e blocos, ou no escuro. Buzina de vez em quando como pista. **Mire nele** (ou clique/bata nele) para achá-lo. Se você chegar perto sem olhar, ele ri e troca de esconderijo. |
| 1 – Observador (estilo dweller) | 3 a 6 | Aparece longe, atrás de você. **Congela quando você olha** e anda quando você vira as costas. Teleporta para trás de você. Se encostar em você sem ser visto: susto (cegueira + grito) e some. |
| 2 – Armas de palhaço | 7 a 11 | Olhos e sorriso brilham no escuro. Fica te observando e, quando você o acha (ou ele chega perto), **começa a caçar**: marreta barulhenta e tortas de creme (cegueira e lentidão). Corre quando você não está olhando, anda devagar quando você está. |
| 3 – Armas reais | 12+ | Mais alto e mais forte (100 de vida): **machado de ferro e facas de arremesso**, aplica Escuridão, **apaga tochas e lanternas** perto de você e **arromba portas**. |

Nas fases 2 e 3 ele prefere aparecer à noite ou quando você está em cavernas. Se você sobreviver 3 minutos, ele vai embora... por enquanto. Se você matá-lo, ele deixa o **Nariz do Mr. Honk** (clique direito = HONK!), tortas e facas — e demora mais para voltar.

## Itens

- **Torta de Creme** — arremessável, cega o alvo.
- **Marreta Barulhenta** — empurra muito, faz "piii".
- **Faca de Arremesso** — 6 de dano.
- **Nariz do Mr. Honk** — troféu.
- **Ovo Gerador de Mr. Honk** — no modo criativo, aba "Mr. Honk".

## Comandos

- `/mrhonk achados` — mostra quantas vezes você o achou e a fase.
- `/mrhonk definir <número>` — muda o contador (ex.: `/mrhonk definir 12` para ir direto à fase 3). *(op)*
- `/mrhonk invocar` — faz ele aparecer agora. *(op)*
- `/mrhonk resetar` — zera tudo. *(op)*

## Configuração

Depois de abrir o jogo uma vez, edite `config/mrhonk-common.toml`:
- quantos achados para cada fase,
- frequência das aparições (`multiplicadorIntervalo`),
- se ele pode quebrar luzes e portas,
- se aparece no modo criativo (útil para testar).

## Estrutura do código

```
src/main/java/com/mrhonk/
  MrHonkMod.java            classe principal
  entity/MrHonkEntity.java  toda a IA (esconder, observar, caçar)
  entity/CreamPieEntity, ThrowingKnifeEntity   projéteis
  util/HonkSpawner.java     escolhe os esconderijos
  util/HonkData.java        contador por jogador e fases
  util/HonkConfig.java      configuração
  event/HonkEvents.java     aparições automáticas e comandos
  client/                   renderização (modelo humanoide + olhos brilhantes)
src/main/resources/assets/mrhonk/   texturas, sons, traduções (pt_br e en_us)
```

O chapéu-coco é uma peça 3D definida em `client/ClientSetup.java` (aba 11x1x11 e copa 7x4x7), com textura na parte de baixo da skin (y 32 a 55).

A skin fica em `assets/mrhonk/textures/entity/mr_honk.png` (formato de skin de jogador 64x64). Dá para trocar por uma desenhada no Blockbench ou em qualquer editor de skin.
