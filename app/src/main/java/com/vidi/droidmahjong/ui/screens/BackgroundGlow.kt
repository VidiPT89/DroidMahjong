package com.vidi.droidmahjong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidmahjong.i18n.Lang
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.ui.theme.Theme

@Composable
fun BackgroundGlow(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Theme.bg)
            .background(
                Brush.radialGradient(
                    colors = listOf(Theme.accent.copy(alpha = 0.16f), Theme.accent.copy(alpha = 0f))
                )
            )
    )
}

@Composable
fun BrandLogo(sizeSp: Int = 40) {
    Row {
        Text("Vidi", color = Theme.text, fontSize = sizeSp.sp, fontWeight = FontWeight.Bold)
        Text("Mahjong", color = Theme.accent, fontSize = sizeSp.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FooterCredits(loc: Localization) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Text(loc.t("developedBy"), color = Theme.textDim, fontSize = 12.sp)
            Text(" David Arsénio Martins", color = Theme.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun LangToggle(loc: Localization, onToggle: () -> Unit) {
    val ptSelected = loc.lang == Lang.PT
    Row(
        Modifier
            .background(Theme.bgPanel2, RoundedCornerShape(50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
    ) {
        Box(
            Modifier
                .background(if (ptSelected) Theme.accent else Color.Transparent, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text("PT", color = if (ptSelected) Theme.bg else Theme.textDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .background(if (!ptSelected) Theme.accent else Color.Transparent, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text("EN", color = if (!ptSelected) Theme.bg else Theme.textDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
