package com.vidi.droidmahjong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.ui.theme.Theme

private val windKeys = listOf("windE", "windS", "windW", "windN")

@Composable
fun TraditionalSetupScreen(
    loc: Localization,
    humanSeats: List<Boolean>,
    onToggleSeat: (Int) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        BackgroundGlow()

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
            Spacer(Modifier.height(24.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .background(Theme.bgPanel, RoundedCornerShape(10.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(loc.t("humanPlayers"), color = Theme.textDim, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(4) { i ->
                        val isHuman = humanSeats[i]
                        Button(
                            onClick = { onToggleSeat(i) },
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.6f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isHuman) Theme.accent else Theme.bgPanel2,
                                contentColor = if (isHuman) Theme.bg else Theme.textDim
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(loc.t(windKeys[i]), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                if (!isHuman) {
                                    Text("(${loc.t("bot")})", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            ) {
                PrimaryButton(loc.t("startMatch"), onStart)
                Spacer(Modifier.height(12.dp))
                GhostButton(loc.t("back"), onBack)
            }
        }
    }
}
