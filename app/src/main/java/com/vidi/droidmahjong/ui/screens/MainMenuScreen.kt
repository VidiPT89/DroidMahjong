package com.vidi.droidmahjong.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidmahjong.i18n.Localization

@Composable
fun MainMenuScreen(
    loc: Localization,
    hasSave: Boolean,
    onPlay: () -> Unit,
    onContinue: () -> Unit,
    onHowToPlay: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        BackgroundGlow()

        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.TopEnd) {
            LangToggle(loc, onToggle = { loc.toggle() })
        }

        Column(
            Modifier.fillMaxSize().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                loc.t("menuTag"),
                color = com.vidi.droidmahjong.ui.theme.Theme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(10.dp))
            BrandLogo(sizeSp = 40)
            Spacer(Modifier.height(10.dp))
            Text(
                loc.t("menuSubtitle"),
                color = com.vidi.droidmahjong.ui.theme.Theme.textDim,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(28.dp))

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
                GhostButton(loc.t("howToPlay"), onHowToPlay)
            }

            Spacer(Modifier.height(60.dp))
            FooterCredits(loc)
        }
    }
}
