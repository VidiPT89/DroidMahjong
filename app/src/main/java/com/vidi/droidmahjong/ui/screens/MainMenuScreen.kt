package com.vidi.droidmahjong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidmahjong.engine.Difficulty
import com.vidi.droidmahjong.engine.LeaderboardStore
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.ui.theme.Theme

@Composable
fun MainMenuScreen(
    loc: Localization,
    hasSave: Boolean,
    selectedDifficulty: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit,
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onHowToPlay: () -> Unit,
    onTraditionalMode: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        BackgroundGlow()

        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.TopEnd) {
            LangToggle(loc, onToggle = { loc.toggle() })
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                loc.t("menuTag"),
                color = Theme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(10.dp))
            BrandLogo(sizeSp = 40)
            Spacer(Modifier.height(10.dp))
            Text(
                loc.t("menuSubtitle"),
                color = Theme.textDim,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(20.dp))

            DifficultyPicker(loc, selectedDifficulty, onDifficultyChange)
            Spacer(Modifier.height(20.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            ) {
                if (hasSave) {
                    SecondaryButton(loc.t("continueGame"), onContinue)
                    Spacer(Modifier.height(12.dp))
                }
                PrimaryButton(loc.t("play"), onPlay)
                Spacer(Modifier.height(12.dp))
                SecondaryButton(loc.t("traditionalMode"), onTraditionalMode)
                Spacer(Modifier.height(12.dp))
                GhostButton(loc.t("howToPlay"), onHowToPlay)
            }

            Spacer(Modifier.height(40.dp))
            FooterCredits(loc)
        }
    }
}

@Composable
private fun DifficultyPicker(loc: Localization, selected: Difficulty, onChange: (Difficulty) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(loc.t("difficultyLabel"), color = Theme.textDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .background(Theme.bgPanel2, RoundedCornerShape(50))
                .padding(3.dp)
        ) {
            val options = listOf(
                Difficulty.EASY to loc.t("difficultyEasy"),
                Difficulty.MEDIUM to loc.t("difficultyMedium"),
                Difficulty.HARD to loc.t("difficultyHard"),
                Difficulty.INFINITE to loc.t("difficultyInfinite")
            )
            options.forEach { (diff, label) ->
                val isSelected = diff == selected
                Box(
                    Modifier
                        .background(if (isSelected) Theme.accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(50))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onChange(diff) }
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        label,
                        color = if (isSelected) Theme.bg else Theme.textDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        val bestLevel = LeaderboardStore.getInfiniteBestLevel(LocalContext.current)
        if (bestLevel > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                loc.t("infiniteBestLabel").replace("{level}", "$bestLevel"),
                color = Theme.textFaint,
                fontSize = 11.sp
            )
        }
    }
}
