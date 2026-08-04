package com.vidi.droidmahjong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.ui.theme.Theme

@Composable
private fun ModalScaffold(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 380.dp)
                .background(Theme.bgPanel, RoundedCornerShape(18.dp))
                .border(1.dp, Theme.borderStrong, RoundedCornerShape(18.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
fun WinModal(loc: Localization, time: String, moves: Int, score: Int, onPlayAgain: () -> Unit, onMenu: () -> Unit) {
    ModalScaffold {
        Text("🎉", fontSize = 40.sp)
        Spacer12()
        Text(loc.t("winTitle"), color = Theme.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer8()
        Text(loc.t("winSubtitle"), color = Theme.textDim, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer12()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
            StatBlock(loc.t("finalTime"), time)
            StatBlock(loc.t("finalMoves"), "$moves")
            StatBlock(loc.t("finalScore"), "$score")
        }
        Spacer12()
        PrimaryButton(loc.t("playAgain"), onPlayAgain)
        Spacer8()
        GhostButton(loc.t("backToMenu"), onMenu)
    }
}

@Composable
fun StuckModal(loc: Localization, onShuffle: () -> Unit, onUndo: () -> Unit, onMenu: () -> Unit) {
    ModalScaffold {
        Text("🀫", fontSize = 36.sp)
        Spacer12()
        Text(loc.t("stuckTitle"), color = Theme.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer8()
        Text(loc.t("stuckSubtitle"), color = Theme.textDim, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer12()
        PrimaryButton(loc.t("shuffle"), onShuffle)
        Spacer8()
        SecondaryButton(loc.t("undo"), onUndo)
        Spacer8()
        GhostButton(loc.t("backToMenu"), onMenu)
    }
}

@Composable
fun ConfirmModal(message: String, loc: Localization, onYes: () -> Unit, onCancel: () -> Unit) {
    ModalScaffold {
        Text(message, color = Theme.text, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer12()
        PrimaryButton(loc.t("confirmYes"), onYes)
        Spacer8()
        GhostButton(loc.t("confirmNo"), onCancel)
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Theme.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Theme.textFaint, fontSize = 11.sp)
    }
}

@Composable
private fun Spacer12() = androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))

@Composable
private fun Spacer8() = androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
