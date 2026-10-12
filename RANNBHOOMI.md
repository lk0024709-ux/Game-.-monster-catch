# Blade Brawl: Rannbhoomi

A complete, offline, top-down melee battle-royale RPG for Android — 350 campaign
levels, 7 rank bosses, 7 elite chimera duels and an idle 3×3 weapon forge.
Every creature, weapon and effect is drawn procedurally on a Compose `Canvas`;
there is not a single sprite file in the project.

> **Implementation note.** The original brief asked for Flutter 3 + Flame. This
> sandbox cannot run the Flutter toolchain, so the game is implemented natively
> in Kotlin with Jetpack Compose (Compose `Canvas` renderer + Kotlin coroutine
> game loop). Feature-for-feature the design is identical, and the pure game
> layer is Android-free so it is unit-testable.

---

## How to play

| Input | Action |
| --- | --- |
| Drag anywhere on the **left half** | Virtual joystick — move the warrior |
| **Hold** anywhere on the **right half** | Attack (aim assist snaps to the nearest foe) |
| ⏸ top-right | Pause / abandon battle |

Battle rules:

* Every creature fights the **nearest enemy of any faction** — monsters brawl
  each other while you pick your moment. Attacking a neutral creature (Rann's
  Avatar) wakes it up, and it comes straight for you.
* A storm ring closes in six phases (1200 → 60 radius, 0 → 30 damage/s).
  Standing outside it drains health fast.
* Healing shrines (+5 HP/s, 15s cooldown, relocating every 40s), sanctuary
  pockets (immune but you cannot attack), destructible crystals and treasure
  chests are all in the world.
* Anti-camping: 30s without dealing damage marks you `Coward` (−20% damage),
  after 150s the storm goes lethal and after 180s creatures gain Bloodlust
  (+25% damage, +10% speed).

## Modes

| Mode | Content |
| --- | --- |
| **Campaign** | 7 ranks × 50 levels. 20 stackable modifiers, mini-bosses every 10th level, a handcrafted boss at every 50th. |
| **Elite Trials** | Pure 1v1 chimera duels with phase shifts at 66% / 33%, adaptive AI (never repeats an ability back-to-back, re-plans every 0.5s), 10 attack and 8 defence abilities across 7 chimeras. You get 2× HP, 1.5× damage and 2 free revives; boosters are banned. Elite mode runs on its own dark palette. |
| **Forge** | Idle 3×3 merge grid. The furnace drops a dagger every 5s (−0.3s per 10 merges, floor 1.5s), drag equal tiers together to climb Dagger → Mythic, offline smelting included. |

## Economy

* `base coins = 40 + levelInRank × 8`
* grade multiplier `D×1 / C×1.2 / B×1.5 / A×2 / S×3 / SS×5` (grade = the **lowest**
  of the time, HP% and damage-taken sub-scores)
* rank multiplier `Bronze×1 … Grand Masters×5`, first clear ×1.5
* bonuses: first clear +300, new best +200, no damage +100, sub-60s +75,
  daily first win +100, consolation on loss +30, every merge +5
* the `Rannslayer` title grants a permanent ×1.5 coin multiplier

## Code map

```
app/src/main/java/com/example/
├── core/MainActivity.kt            single activity + navigation
├── ui/theme/                       palette, type scale, Material3 wrappers
└── rann/
    ├── model/      Enums.kt  Balance.kt  LevelSpec.kt      (pure Kotlin)
    ├── content/    Content.kt  LevelFactory.kt  ChimeraFactory.kt
    ├── sim/        Entities.kt  BattleSim.kt  CreatureAi.kt
    │               BossAi.kt  Twists.kt                    ← headless simulation
    ├── forge/      Forge.kt                                ← merge grid + offline
    ├── profile/    Profile.kt  ProfileStore.kt             ← save codec + prefs
    ├── render/     Art.kt  BattleRenderer.kt               ← Compose Canvas art
    ├── audio/      Sound.kt                                ← ToneGenerator + haptics
    ├── vm/         RannViewModel.kt                        ← state + progression
    └── ui/         Common.kt  Showcase.kt  screens/*       ← 7 screens
```

The simulation never imports Android APIs, so a full battle — spawns, AI,
hazards, storm, rewards — runs headlessly in a plain JVM test. The renderer
reuses the same `BattleSim` coordinates, which is why what you see is exactly
what hits you.

## Build

```bash
./gradlew :app:assembleDebug     # minSdk 24, targetSdk 36
./gradlew :app:test              # unit + Robolectric tests
```

Assets: none. Audio: synthesised with `ToneGenerator`; haptics use
`VibrationEffect`. Everything is stored locally in `SharedPreferences`
(`rannbhoomi_save / profile_v1`).
