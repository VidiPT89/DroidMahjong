# 🀄 DroidMahjong — Mahjong Solitaire & 4-Player Riichi for Android

> A native Jetpack Compose Mahjong app for Android with two independent modes: the classic Solitaire (provably solvable 144-tile turtle spread) and a full local 4-player Riichi table with bots.

"DroidMahjong" ships two game modes built from scratch in Kotlin:

- **Solitaire** — clear a 144-tile turtle pyramid by matching two free tiles at a time. Every deal is generated from a solved state working backwards, so a full clear is always mathematically possible — the same dealing algorithm proven out in the [VidiMahjong](https://github.com/VidiPT89/VidiMahjong) web version and its [iMahjong](https://github.com/VidiPT89/iMahjong) Swift sibling, reimplemented independently in Kotlin.
- **4 Players (Riichi)** — a real Riichi Mahjong table: wall, hands, chi/pon/kan, riichi declarations, dora/ura-dora, a wide yaku set, and han/fu scoring. Played entirely **local pass-and-play on one device**, with any empty seats auto-filled by a simple discard/reaction bot. There is currently no online/networked mode.

## 📦 What's Inside

### Solitaire
- 🎚️ Three difficulty levels — **Easy** (a flat 108-tile suits-only spread, nothing ever covered), **Medium** (the classic 144-tile turtle), and **Hard** (the same 144 tiles stacked into a taller peak across 6 layers)
- 🐢 Full "turtle" pyramid spread with proper covered / blocked / free tile rules
- ✅ Provably solvable deals — tiles are assigned by walking the board's own removal order backwards, so a complete solve always exists
- 💡 Limited hints (5 per game) that highlight a real playable pair, 🔀 a shuffle that keeps the remaining board solvable, and ↩️ unlimited undo
- 🎬 Smooth Compose animations — lift on select, shake on mismatch, staggered deal-in
- 🀫 34 tile faces (Characters, Bamboos, Circles, Winds, Dragons, Flowers, Seasons) drawn with CJK glyphs and native Canvas shapes — no image assets
- 💾 Autosaves mid-game, with a "Continue Game" option from the main menu
- 🏆 A local best-time / fewest-moves leaderboard per difficulty, stored on-device (no backend), shown after a win
- 🔊 Sound feedback on tile pick, match, mismatch and win (see [Sound](#-sound) below)
- 📖 An in-app "How to Play" guide with a visual diagram of the covered / blocked / free tile rule
- 🇵🇹 🇬🇧 One-click language toggle between European Portuguese and English, remembered between visits

### 4 Players (Riichi)
- 🀫 The same 34 tile faces (minus Flowers/Seasons, which aren't part of a standard 136-tile Riichi wall) shared with Solitaire
- 🎴 Real winning-hand detection: recursive decomposition into 4 sets + a pair, plus the special Chiitoitsu (seven pairs) and Kokushi Musou (thirteen orphans) shapes
- 🀄 Chi, pon, daiminkan (open kan), ankan (concealed kan) and shouminkan (added kan), with correct reaction priority (ron > pon/kan > chi) and simultaneous multi-player reactions (e.g. a double ron)
- 🎯 Riichi declarations (with a closed-tenpai check before allowing it), ippatsu, and double riichi
- 🎲 A wide yaku set with han/fu scoring: Riichi, Ippatsu, Menzen Tsumo, Pinfu, Tanyao, Yakuhai (dragons, round wind, seat wind), Toitoi, Sanankou, Chanta/Junchan, Ittsuu, Sanshoku Doujun/Doukou, Iipeikou/Ryanpeikou, Honitsu/Chinitsu, Honroutou, Shousangen, plus the yakuman hands (Kokushi Musou, Suuankou, Daisangen, Tsuuiisou, Chinroutou, Ryuuiisou, Suukantsu)
- 🀇 Dora and ura-dora (ura-dora only counted for riichi hands), with new dora indicators revealed on each kan
- 🤖 A discard/reaction bot AI that auto-fills any empty seats (1–4 human players per table, pass-and-play on the same device)
- 📊 Riichi sticks carried across hands, dealer rotation on a non-dealer win, exhaustive-draw tenpai/noten payments, and a running points total across the match
- 🕹️ Scope is a single East round (4 hands, one dealer turn each — a casual "tonpuusen" format) rather than a full East+South hanchan
- ❌ No online/networked play — this is local-device only (unlike the WebSocket-based online rooms in the sibling [VidiMahjong](https://github.com/VidiPT89/VidiMahjong) web project)

## 🛠️ Tech Stack

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?style=flat&logo=android&logoColor=white)
![Material3](https://img.shields.io/badge/Material%203-757575?style=flat&logo=materialdesign&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat&logo=gradle&logoColor=white)

## 🏗️ Project Structure

```
DroidMahjong/
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── res/                          # Theme, strings, adaptive launcher icon
│       │   └── java/com/vidi/droidmahjong/
│       │       ├── MainActivity.kt            # Entry point, screen router
│       │       ├── data/
│       │       │   ├── TileType.kt             # 34 tile types, matching rules, pair-unit builders
│       │       │   └── Tile.kt                  # Board position & tile instance types
│       │       ├── engine/
│       │       │   ├── Layout.kt                 # Fixed 144-position turtle layout (+ Easy/Hard variants)
│       │       │   ├── GameEngine.kt              # Board state, free-tile rules, solvable dealing
│       │       │   ├── SaveStore.kt                # Mid-game autosave/restore (SharedPreferences)
│       │       │   └── LeaderboardStore.kt          # Local best time/moves per difficulty (SharedPreferences)
│       │       ├── riichi/                          # 4-Player Riichi mode (local pass-and-play only)
│       │       │   ├── RiichiTiles.kt                # Standard 136-tile wall, tile-type helpers
│       │       │   ├── HandEval.kt                    # Winning-hand decomposition (standard/chiitoitsu/kokushi)
│       │       │   ├── Yaku.kt                         # Yaku detection + fu calculation
│       │       │   ├── Scoring.kt                       # Han/fu → points, ron/tsumo payment split
│       │       │   ├── RiichiEngine.kt                   # Turn engine: draw/discard/chi/pon/kan/riichi
│       │       │   ├── Bot.kt                             # Discard/reaction bot heuristics
│       │       │   └── LocalMatch.kt                       # Chains hands together, drives bots, local pass-and-play
│       │       ├── i18n/Localization.kt            # PT/EN strings and language persistence
│       │       └── ui/
│       │           ├── theme/Theme.kt               # ividi.dev-matched color tokens
│       │           ├── sound/SoundFx.kt               # Placeholder ToneGenerator-based sound feedback
│       │           └── screens/
│       │               ├── SplashScreen.kt              # Animated intro with developer credit
│       │               ├── MainMenuScreen.kt             # Menu, difficulty picker, language toggle
│       │               ├── HowToPlayScreen.kt             # Solitaire rules guide with visual diagrams
│       │               ├── GameScreen.kt                   # Solitaire board screen, stats, actions, modals
│       │               ├── TileView.kt                      # Individual tile rendering & animations
│       │               ├── TileFaceView.kt                   # CJK glyph / pip tile face rendering
│       │               ├── BoardGeometry.kt                   # Layout → screen coordinate math
│       │               ├── Modals.kt                            # Win / stuck / confirm modals + leaderboard block
│       │               ├── Buttons.kt                             # Shared button styles
│       │               ├── BackgroundGlow.kt                       # Shared theme components
│       │               ├── TraditionalModeSelectScreen.kt            # Riichi: mode entry point
│       │               ├── TraditionalSetupScreen.kt                  # Riichi: choose human/bot seats
│       │               └── TraditionalTableScreen.kt                    # Riichi: the table itself
│       └── test/java/com/vidi/droidmahjong/            # JUnit unit tests (engine, Riichi hand/yaku logic)
├── LICENSE
└── README.md
```

## ⚙️ Game Mechanics

### Solitaire — solvable dealing
```
Free tile rule:
  a tile is FREE only if:
    - no other tile occupies the same (x, y) at a higher layer, AND
    - its left OR right neighbour (same layer, same row) is empty

Dealing a solvable board:
  1. walk every board position, repeatedly pairing up whichever tiles are
     currently free (given only the positions not yet paired)
  2. assign each matching pair-unit of tile types to one such pair
  3. because freeness only depends on position — never on tile type —
     replaying that same pairing order back is always a valid full solve
```

### 4 Players (Riichi) — turn engine
```
Draw → (tsumo? / ankan?) → discard → reaction window (ron > pon/kan > chi) → next draw

Winning-hand check: recursive decomposition of the concealed hand (plus any
called melds) into 4 sets + 1 pair, tried against every possible split so the
highest-scoring yaku combination is used — plus the special Chiitoitsu and
Kokushi Musou shapes, which skip normal decomposition entirely.

A single hand ends in one of: tsumo (self-draw win), ron (discard win, with
support for a double ron), or an exhaustive draw (wall empty), which pays out
based on which seats are in tenpai.
```

## 🚀 How to Run

```bash
# 1. Clone the repository
git clone https://github.com/VidiPT89/DroidMahjong.git
cd DroidMahjong

# 2. Build and install a debug APK on a connected device/emulator
./gradlew installDebug

# ...or open the project in Android Studio and run it from there.

# Run the JUnit unit tests (Solitaire engine + Riichi hand/yaku logic)
./gradlew test
```

## 🔊 Sound

Tile pick, match, mismatch and win feedback use `android.media.ToneGenerator` — this is an explicit **placeholder**, since the repository ships no custom audio assets under `res/raw/`. See the comment at the top of `ui/sound/SoundFx.kt` for how to swap in real sound files later (drop `.ogg`/`.mp3` files into `res/raw/` and replace the `ToneGenerator.startTone` calls with a small `SoundPool`).

## 📝 Notes

- Flowers and Seasons in Solitaire are special: any Flower matches any other Flower, and any Season matches any other Season, without needing to be identical — matching real Mahjong rules
- Language, hints used, in-progress Solitaire games, and the local leaderboard are all stored locally via `SharedPreferences`, so they persist between visits
- The board layout, matching rules and solvable-dealing algorithm mirror the web version at [VidiMahjong](https://github.com/VidiPT89/VidiMahjong) and the [iMahjong](https://github.com/VidiPT89/iMahjong) Swift port, but this is an independent Kotlin codebase — no code is shared between them
- The Riichi yaku scope covers the common competitive set plus all yakuman; a few rare edge cases (abortive draws like four-riichi/four-kan, double-yakuman variants, kan-related special yaku like rinshan kaihou/chankan) are intentionally out of scope for this version

---

Developed by **David Arsénio Martins** — *"Vidi"*
