# 🀄 DroidMahjong — Mahjong Solitaire for Android

> A native Jetpack Compose take on Mahjong Solitaire — the same provably solvable 144-tile turtle spread, rebuilt from scratch in Kotlin for Android.

"DroidMahjong" clears a 144-tile turtle pyramid by matching two free tiles at a time. Every deal is generated from a solved state working backwards, so a full clear is always mathematically possible — the same dealing algorithm proven out in the [VidiMahjong](https://github.com/VidiPT89/VidiMahjong) web version and its [iMahjong](https://github.com/VidiPT89/iMahjong) Swift sibling, reimplemented independently in Kotlin.

## 📦 What's Inside

- 🐢 Full 144-tile "turtle" pyramid spread across 5 layers, with proper covered / blocked / free tile rules
- ✅ Provably solvable deals — tiles are assigned by walking the board's own removal order backwards, so a complete solve always exists
- 💡 Limited hints that highlight a real playable pair, 🔀 a shuffle that keeps the remaining board solvable, and ↩️ unlimited undo
- 🎬 Smooth Compose animations — lift on select, shake on mismatch, staggered deal-in
- 🀫 34 tile faces (Characters, Bamboos, Circles, Winds, Dragons, Flowers, Seasons) drawn with CJK glyphs and native Canvas shapes — no image assets
- 💾 Autosaves mid-game, with a "Continue Game" option from the main menu
- 📖 An in-app "How to Play" guide with a visual diagram of the covered / blocked / free tile rule
- 🇵🇹 🇬🇧 One-click language toggle between European Portuguese and English, remembered between visits

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
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/                          # Theme, strings, adaptive launcher icon
│       └── java/com/vidi/droidmahjong/
│           ├── MainActivity.kt            # Entry point, screen router
│           ├── data/
│           │   ├── TileType.kt             # 34 tile types, matching rules, pair-unit builders
│           │   └── Tile.kt                  # Board position & tile instance types
│           ├── engine/
│           │   ├── Layout.kt                 # Fixed 144-position turtle layout
│           │   ├── GameEngine.kt              # Board state, free-tile rules, solvable dealing
│           │   └── SaveStore.kt                # Mid-game autosave/restore (SharedPreferences)
│           ├── i18n/Localization.kt            # PT/EN strings and language persistence
│           └── ui/
│               ├── theme/Theme.kt               # ividi.dev-matched color tokens
│               └── screens/
│                   ├── SplashScreen.kt           # Animated intro with developer credit
│                   ├── MainMenuScreen.kt          # Menu, language toggle
│                   ├── HowToPlayScreen.kt          # Rules guide with visual diagrams
│                   ├── GameScreen.kt                # Board screen, stats, actions, modals
│                   ├── TileView.kt                   # Individual tile rendering & animations
│                   ├── TileFaceView.kt                # CJK glyph / pip tile face rendering
│                   ├── BoardGeometry.kt                # Layout → screen coordinate math
│                   ├── Modals.kt                        # Win / stuck / confirm modals
│                   ├── Buttons.kt                         # Shared button styles
│                   └── BackgroundGlow.kt                   # Shared theme components
├── LICENSE
└── README.md
```

## ⚙️ Game Mechanics

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

## 🚀 How to Run

```bash
# 1. Clone the repository
git clone https://github.com/VidiPT89/DroidMahjong.git
cd DroidMahjong

# 2. Build and install a debug APK on a connected device/emulator
./gradlew installDebug

# ...or open the project in Android Studio and run it from there.
```

## 📝 Notes

- Flowers and Seasons are special: any Flower matches any other Flower, and any Season matches any other Season, without needing to be identical — matching real Mahjong rules
- Language, hints used and in-progress games are stored locally, so they persist between visits
- The board layout, matching rules and solvable-dealing algorithm mirror the web version at [VidiMahjong](https://github.com/VidiPT89/VidiMahjong) and the [iMahjong](https://github.com/VidiPT89/iMahjong) Swift port, but this is an independent Kotlin codebase — no code is shared between them

---

Developed by **David Arsénio Martins** — *"Vidi"*
