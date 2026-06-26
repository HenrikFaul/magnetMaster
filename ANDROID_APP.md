# Magnet Master — Native Android App

This repository now contains a **fully playable, native Android (Kotlin) game**
that implements the Magnet Master design from [`AI_PROMPT.md`](./AI_PROMPT.md)
and [`README.md`](./README.md): a side-view physics arcade where you drag a
finger-magnet to pull metal into the goal chute, upgrade a 16-node skill tree,
fight boss wrecks and chase a daily salvage-yard leaderboard.

It is built as a **pure native app** (no WebView / no Capacitor): a custom 2D
physics engine + `SurfaceView` canvas renderer running a 60 FPS game loop, so it
matches the "native APK" requirement and stays well under the size budget.

## Get the APK

A prebuilt, **signed and installable** APK ships in the repo:

```
dist/MagnetMaster.apk      # v1.0.0 · minSdk 24 / target 34 · ~1.4 MB · v2-signed
```

Download it, enable "install unknown apps", and tap to install on any Android 7.0+
device. Verify with `apksigner verify` (APK Signature Scheme v2).

### Rebuild it yourself

Two equivalent paths:

1. **No Gradle / no Google SDK** (works behind locked-down networks):
   ```bash
   tools/local-build/build-apk.sh        # → tools/local-build/out/MagnetMaster.apk
   ```
   See [`tools/local-build/README.md`](./tools/local-build/README.md). Every tool
   is fetched from a public mirror (GitHub / Maven Central / googleapis), so it
   builds even when `dl.google.com` is blocked.

2. **Standard Gradle** (needs JDK 17 + Android SDK API 34):
   ```bash
   ./gradlew :app:assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
   ```
   The included **GitHub Actions** workflow runs this on every push and uploads
   the APK as the `MagnetMaster-APK` artifact; push a tag (e.g. `v1.0.0`) to
   publish a GitHub Release.

## Build locally

Requirements: JDK 17 and the Android SDK (API 34). With `ANDROID_HOME` set:

```bash
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open the project root in Android Studio (Giraffe+) and press Run for the IDE
workflow.

## How the spec maps to the code

| Spec (AI_PROMPT.md / README.md) | Implementation |
|---|---|
| Side-view 1600×900 world, magnet = finger | `GameView` letterboxes a virtual 1600×900 canvas; touch → world coords |
| Force `F = k·m·q/r²`, r-clamp 50px, NaN-safe | `engine/World.kt` (`step`), acceleration `÷ mass` so heavier metals drag slower |
| Metals: Bolt/Coin/Gear/Plate/Wreckage (mass + kg) | `engine/Body.kt` (`Kind` enum, exact mass/kg table) |
| Distractors: Bomb (−1 HP, ✕ icon), Sharp (−0.5), Rubber (blocks) | `Kind` hazard values + `Render` colour-blind X marker |
| Field-lines (8 outward lines) | `screens/Render.drawFieldLines` (animated, static under Reduce-Motion) |
| 16-node skill tree: Range/Pull/Pulse/Polarity | `game/Skills.kt` + `screens/SkillTreeScreen.kt` |
| Pulse mega-shockwave every 3s; double-tap Polarity Flip | `World.firePulse`, double-tap repel in `GameplayScreen` |
| Progression `T(n)=1.5+0.15n`, time `90−n/2`, bombs `floor(n/10)` | `game/Levels.kt` |
| Boss every 10 levels (rip panels + reactor core) | `Levels.buildBoss`, `Kind.PANEL`/`CORE` |
| DDA: 3× fail → +time | `GameplayScreen` fail tracking |
| Smart hint after 5s idle | `GameplayScreen.computeHint` |
| Daily challenge + top leaderboard | `screens/DailyScreen.kt` (deterministic day-seed) |
| Rewarded (+15s, 2× gems, daily wheel) | result modal + `ShopScreen` (simulated; wire to an ad SDK) |
| Cosmetic magnet skins (neon/gold/plasma) | `game/Skins.kt` + `ShopScreen` |
| Cloud `user_progress` schema | mirrored locally in `game/Store.kt` (SharedPreferences) |
| Haptics on every slam | `core/Haptics.kt` (Vibrator) |
| Brand palette + fonts | `core/Theme.kt` |
| Determinism (fixed-dt) for replay/anti-cheat | fixed `1/60` step accumulator in `GameplayScreen.update` |

## Online features

Leaderboard rivals, daily seed and IAP are implemented **on-device** so the game
is fully playable offline. The data shapes match the Lovable Cloud schema and
Edge Functions in `AI_PROMPT.md §4`; swapping the local `Store`/`DailyScreen`
sources for HTTP calls to `submit-daily` / `daily-challenge` / `match-ghost`
wires up the cloud backend without touching gameplay.

## Project layout

```
app/src/main/java/com/magnetmaster/game/
├── MainActivity.kt              # fullscreen immersive host activity
├── GameView.kt                  # SurfaceView, game loop, viewport, input
├── core/                        # Theme, Ui, Screen, Game, Haptics
├── engine/                      # Body, World (physics + magnet force)
├── game/                        # Levels, Skills, Skins, Store, Rng
└── screens/                     # Title, LevelSelect, Gameplay, SkillTree,
                                 # Daily, Shop, Settings, Render
```
