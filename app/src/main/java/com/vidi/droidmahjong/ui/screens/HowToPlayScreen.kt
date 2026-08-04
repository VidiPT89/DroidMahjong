package com.vidi.droidmahjong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun HowToPlayScreen(loc: Localization, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Theme.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp).background(Theme.bgPanel2, CircleShape)) {
                Icon(Icons.Default.ArrowBack, contentDescription = loc.t("back"), tint = Theme.text)
            }
            Spacer(Modifier.weight(1f))
            Text(loc.t("htpTitle"), color = Theme.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(36.dp))
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(loc.t("htpIntro"), color = Theme.textDim, fontSize = 15.sp)
            Spacer(Modifier.height(26.dp))

            Text(loc.t("htpFreeTitle"), color = Theme.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(loc.t("htpFreeBody"), color = Theme.textDim, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HtpExample(loc.t("htpCoveredLabel"), loc.t("htpCoveredDesc")) {
                    Box(Modifier.size(50.dp, 60.dp), contentAlignment = Alignment.Center) {
                        MiniTile(Modifier.offset(y = 6.dp))
                        MiniTile(Modifier.offset(y = (-6).dp), width = 28.dp, height = 40.dp)
                    }
                }
                HtpExample(loc.t("htpBlockedLabel"), loc.t("htpBlockedDesc")) {
                    Row {
                        MiniTile(width = 26.dp, height = 40.dp)
                        MiniTile(width = 26.dp, height = 40.dp, modifier = Modifier.offset(x = (-4).dp))
                        MiniTile(width = 26.dp, height = 40.dp, modifier = Modifier.offset(x = (-8).dp))
                    }
                }
                HtpExample(loc.t("htpFreeLabel"), loc.t("htpFreeDesc")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniTile(width = 28.dp, height = 40.dp, highlighted = true)
                        MiniTile(width = 26.dp, height = 40.dp)
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            Text(loc.t("htpMatchTitle"), color = Theme.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(loc.t("htpMatchBody"), color = Theme.textDim, fontSize = 14.sp)

            Spacer(Modifier.height(26.dp))
            Text(loc.t("htpToolsTitle"), color = Theme.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            ToolRow(loc.t("hint"), loc.t("htpHintBody"))
            ToolRow(loc.t("shuffle"), loc.t("htpShuffleBody"))
            ToolRow(loc.t("undo"), loc.t("htpUndoBody"))
            Spacer(Modifier.height(90.dp))
        }

        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            PrimaryButton(loc.t("htpCloseButton"), onClose)
        }
    }
}

@Composable
private fun HtpExample(label: String, desc: String, diagram: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Box(Modifier.height(60.dp), contentAlignment = Alignment.Center) { diagram() }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(desc, color = Theme.textDim, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun MiniTile(modifier: Modifier = Modifier, highlighted: Boolean = false, width: androidx.compose.ui.unit.Dp = 34.dp, height: androidx.compose.ui.unit.Dp = 46.dp) {
    Box(
        modifier
            .size(width, height)
            .background(Theme.tileFace, RoundedCornerShape(4.dp))
            .border(if (highlighted) 2.dp else 1.dp, if (highlighted) Theme.ok else Theme.tileEdge, RoundedCornerShape(4.dp))
    )
}

@Composable
private fun ToolRow(title: String, body: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(title, color = Theme.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("  — $body", color = Theme.textDim, fontSize = 14.sp)
    }
}
