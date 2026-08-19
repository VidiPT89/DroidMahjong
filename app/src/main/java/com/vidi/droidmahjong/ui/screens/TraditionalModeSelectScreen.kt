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
import com.vidi.droidmahjong.ui.theme.Theme

@Composable
fun TraditionalModeSelectScreen(
    loc: Localization,
    onBack: () -> Unit,
    onLocal: () -> Unit,
    onOnline: () -> Unit
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
                loc.t("tradSetupTitle"),
                color = Theme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(10.dp))
            BrandLogo(sizeSp = 40)
            Spacer(Modifier.height(10.dp))
            Text(
                loc.t("tradSetupSubtitle"),
                color = Theme.textDim,
                fontSize = 15.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(28.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            ) {
                PrimaryButton(loc.t("playLocal"), onLocal)
                Spacer(Modifier.height(12.dp))
                SecondaryButton(loc.t("playOnline"), onOnline)
                Spacer(Modifier.height(12.dp))
                GhostButton(loc.t("back"), onBack)
            }

            Spacer(Modifier.height(60.dp))
            FooterCredits(loc)
        }
    }
}
