package com.vidi.droidmahjong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidmahjong.engine.LEVELS_MAX_LEVEL
import com.vidi.droidmahjong.engine.LeaderboardStore
import com.vidi.droidmahjong.engine.LevelTier
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.ui.theme.Theme

@Composable
fun LevelSelectScreen(loc: Localization, onBack: () -> Unit, onSelectLevel: (Int) -> Unit) {
    val bestLevel = LeaderboardStore.getInfiniteBestLevel(LocalContext.current)

    Column(Modifier.fillMaxSize().background(Theme.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp).background(Theme.bgPanel2, CircleShape)) {
                Icon(Icons.Default.ArrowBack, contentDescription = loc.t("back"), tint = Theme.text)
            }
            Spacer(Modifier.weight(1f))
            Text(loc.t("levelsSelectTitle"), color = Theme.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(36.dp))
        }

        Text(
            loc.t("levelsSelectSubtitle"),
            color = Theme.textDim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )

        if (bestLevel >= LEVELS_MAX_LEVEL) {
            Spacer(Modifier.height(10.dp))
            Text(
                loc.t("allLevelsComplete"),
                color = Theme.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LevelTier.entries.forEach { tier ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        tierLabel(loc, tier),
                        color = Theme.textFaint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                    )
                }
                items(tier.range.toList()) { level ->
                    LevelTile(
                        level = level,
                        unlocked = level <= bestLevel + 1,
                        cleared = level <= bestLevel,
                        onClick = { onSelectLevel(level) }
                    )
                }
            }
        }
    }
}

private fun tierLabel(loc: Localization, tier: LevelTier): String = when (tier) {
    LevelTier.EASY -> loc.t("tierEasy")
    LevelTier.MEDIUM -> loc.t("tierMedium")
    LevelTier.HARD -> loc.t("tierHard")
}

@Composable
private fun LevelTile(level: Int, unlocked: Boolean, cleared: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                if (cleared) Theme.accent.copy(alpha = 0.18f) else Theme.bgPanel2,
                RoundedCornerShape(10.dp)
            )
            .border(1.dp, if (cleared) Theme.accent else Theme.border, RoundedCornerShape(10.dp))
            .then(if (unlocked) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (unlocked) {
            Text("$level", color = if (cleared) Theme.accent else Theme.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Theme.textFaint, modifier = Modifier.size(16.dp))
        }
    }
}
