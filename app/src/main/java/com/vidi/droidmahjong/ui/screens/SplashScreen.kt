package com.vidi.droidmahjong.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.ui.theme.Theme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(loc: Localization, onFinish: () -> Unit) {
    var appeared by remember { mutableFloatStateOf(0f) }
    val appearedAnim by animateFloatAsState(appeared, animationSpec = tween(700), label = "splash")

    LaunchedEffect(Unit) {
        appeared = 1f
        delay(2600)
        onFinish()
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        BackgroundGlow()
        Column(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onFinish() }
                .padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            androidx.compose.foundation.layout.Box(Modifier)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(appearedAnim)) {
                BrandLogo(sizeSp = 46)
                Spacer(Modifier.height(14.dp))
                Text(loc.t("menuSubtitle"), color = Theme.textDim, fontSize = 15.sp)
                Spacer(Modifier.height(20.dp))
                Text(loc.t("developedBy"), color = Theme.textFaint, fontSize = 13.sp)
                Text("David Arsénio Martins", color = Theme.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("ividi.dev · github.com/VidiPT89", color = Theme.accent, fontSize = 13.sp)
            }
            Text(
                loc.t("tapToContinue"),
                color = Theme.textFaint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(appearedAnim * 0.8f)
            )
        }
    }
}
