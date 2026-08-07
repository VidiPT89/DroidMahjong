package com.vidi.droidmahjong

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vidi.droidmahjong.engine.Difficulty
import com.vidi.droidmahjong.engine.GameEngine
import com.vidi.droidmahjong.engine.SaveStore
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.riichi.LocalMatch
import com.vidi.droidmahjong.ui.screens.GameScreen
import com.vidi.droidmahjong.ui.screens.HowToPlayScreen
import com.vidi.droidmahjong.ui.screens.MainMenuScreen
import com.vidi.droidmahjong.ui.screens.SplashScreen
import com.vidi.droidmahjong.ui.screens.TraditionalModeSelectScreen
import com.vidi.droidmahjong.ui.screens.TraditionalSetupScreen
import com.vidi.droidmahjong.ui.screens.TraditionalTableScreen

private enum class AppScreen { SPLASH, MENU, HOW_TO_PLAY, GAME, TRAD_MODE_SELECT, TRAD_SETUP, TRAD_TABLE }

private const val PREFS = "droidmahjong-prefs"
private const val KEY_DIFFICULTY = "difficulty"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RootApp()
        }
    }
}

private fun loadStoredDifficulty(context: Context): Difficulty {
    val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DIFFICULTY, null)
    return Difficulty.entries.firstOrNull { it.name == stored } ?: Difficulty.EASY
}

private fun storeDifficulty(context: Context, difficulty: Difficulty) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString(KEY_DIFFICULTY, difficulty.name)
        .apply()
}

@Composable
private fun RootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loc = remember { Localization(context) }
    var difficulty by remember { mutableStateOf(loadStoredDifficulty(context)) }
    val engine = remember { GameEngine(difficulty) }

    var screen by remember { mutableStateOf(AppScreen.SPLASH) }
    var hasSave by remember { mutableStateOf(SaveStore.hasSave(context)) }
    var humanSeats by remember { mutableStateOf(listOf(true, false, false, false)) }
    var localMatch by remember { mutableStateOf<LocalMatch?>(null) }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        when (screen) {
            AppScreen.SPLASH -> SplashScreen(loc) {
                screen = AppScreen.MENU
            }
            AppScreen.MENU -> MainMenuScreen(
                loc = loc,
                hasSave = hasSave,
                selectedDifficulty = difficulty,
                onDifficultyChange = {
                    difficulty = it
                    storeDifficulty(context, it)
                },
                onPlay = {
                    engine.reset(difficulty)
                    SaveStore.clear(context)
                    hasSave = false
                    screen = AppScreen.GAME
                },
                onContinue = {
                    val snapshot = SaveStore.load(context)
                    if (snapshot != null) engine.restore(snapshot) else engine.reset(difficulty)
                    screen = AppScreen.GAME
                },
                onHowToPlay = { screen = AppScreen.HOW_TO_PLAY },
                onTraditionalMode = { screen = AppScreen.TRAD_MODE_SELECT }
            )
            AppScreen.HOW_TO_PLAY -> HowToPlayScreen(loc) {
                screen = AppScreen.MENU
            }
            AppScreen.GAME -> GameScreen(
                engine = engine,
                loc = loc,
                onExit = {
                    hasSave = SaveStore.hasSave(context)
                    screen = AppScreen.MENU
                }
            )
            AppScreen.TRAD_MODE_SELECT -> TraditionalModeSelectScreen(
                loc = loc,
                onBack = { screen = AppScreen.MENU },
                onLocal = { screen = AppScreen.TRAD_SETUP }
            )
            AppScreen.TRAD_SETUP -> TraditionalSetupScreen(
                loc = loc,
                humanSeats = humanSeats,
                onToggleSeat = { i -> humanSeats = humanSeats.toMutableList().also { it[i] = !it[i] } },
                onBack = { screen = AppScreen.TRAD_MODE_SELECT },
                onStart = {
                    val match = LocalMatch(humanSeats, scope)
                    localMatch = match
                    match.startHand()
                    screen = AppScreen.TRAD_TABLE
                }
            )
            AppScreen.TRAD_TABLE -> localMatch?.let { match ->
                TraditionalTableScreen(
                    loc = loc,
                    match = match,
                    onExit = {
                        match.stop()
                        localMatch = null
                        screen = AppScreen.TRAD_MODE_SELECT
                    }
                )
            }
        }
    }
}
