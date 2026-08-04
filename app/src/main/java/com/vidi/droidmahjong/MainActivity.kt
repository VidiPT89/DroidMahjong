package com.vidi.droidmahjong

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vidi.droidmahjong.engine.GameEngine
import com.vidi.droidmahjong.engine.SaveStore
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.ui.screens.GameScreen
import com.vidi.droidmahjong.ui.screens.HowToPlayScreen
import com.vidi.droidmahjong.ui.screens.MainMenuScreen
import com.vidi.droidmahjong.ui.screens.SplashScreen

private enum class AppScreen { SPLASH, MENU, HOW_TO_PLAY, GAME }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RootApp()
        }
    }
}

@Composable
private fun RootApp() {
    val context = LocalContext.current
    val loc = remember { Localization(context) }
    val engine = remember { GameEngine() }

    var screen by remember { mutableStateOf(AppScreen.SPLASH) }
    var hasSave by remember { mutableStateOf(SaveStore.hasSave(context)) }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        when (screen) {
            AppScreen.SPLASH -> SplashScreen(loc) {
                screen = AppScreen.MENU
            }
            AppScreen.MENU -> MainMenuScreen(
                loc = loc,
                hasSave = hasSave,
                onPlay = {
                    engine.reset()
                    SaveStore.clear(context)
                    hasSave = false
                    screen = AppScreen.GAME
                },
                onContinue = {
                    val snapshot = SaveStore.load(context)
                    if (snapshot != null) engine.restore(snapshot) else engine.reset()
                    screen = AppScreen.GAME
                },
                onHowToPlay = { screen = AppScreen.HOW_TO_PLAY }
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
        }
    }
}
