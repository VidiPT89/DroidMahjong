package com.vidi.droidmahjong.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidmahjong.data.NUMERALS
import com.vidi.droidmahjong.data.PIP_ROWS
import com.vidi.droidmahjong.data.Suit
import com.vidi.droidmahjong.data.TILE_TYPES_BY_ID
import com.vidi.droidmahjong.data.TileCategory
import com.vidi.droidmahjong.ui.theme.Theme

private val windKanji = mapOf("wE" to "東", "wS" to "南", "wW" to "西", "wN" to "北")
private val flowerKanji = mapOf("fl1" to "梅", "fl2" to "蘭", "fl3" to "竹", "fl4" to "菊")
private val seasonKanji = mapOf("se1" to "春", "se2" to "夏", "se3" to "秋", "se4" to "冬")
private val circleColors = listOf(Theme.circleBlue, Theme.circleRed, Theme.circleGreen)

/** Renders the printed face of a tile type — CJK glyphs and simple vector pips, no image
 *  assets, mirroring the web app's hand-drawn SVG/glyph approach. */
@Composable
fun TileFaceView(typeId: String, modifier: Modifier = Modifier) {
    val type = TILE_TYPES_BY_ID[typeId] ?: return
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (type.category) {
            TileCategory.SUIT -> when (type.suit) {
                Suit.CHARACTERS -> CharactersFace(type.rank ?: 1)
                Suit.BAMBOO -> PipFace(type.rank ?: 1, isBamboo = true)
                Suit.CIRCLE -> PipFace(type.rank ?: 1, isBamboo = false)
                null -> {}
            }
            TileCategory.WIND -> SoloGlyph(windKanji[typeId] ?: "?", Theme.glyphDark)
            TileCategory.DRAGON -> when (typeId) {
                "dR" -> SoloGlyph("中", Theme.dragonRed)
                "dG" -> SoloGlyph("發", Theme.dragonGreen)
                else -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            }
            TileCategory.FLOWER -> BonusGlyph(flowerKanji[typeId] ?: "?", type.rank ?: 1, Theme.bonusPink)
            TileCategory.SEASON -> BonusGlyph(seasonKanji[typeId] ?: "?", type.rank ?: 1, Theme.bonusBlue)
        }
    }
}

@Composable
private fun CharactersFace(rank: Int) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(NUMERALS[rank], color = Theme.charInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("萬", color = Theme.glyphDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SoloGlyph(text: String, color: Color) {
    Text(text, color = color, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun BonusGlyph(text: String, number: Int, color: Color) {
    Box {
        Text(text, color = color, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "$number",
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd),
            style = TextStyle(fontSize = 8.sp)
        )
    }
}

@Composable
private fun PipFace(rank: Int, isBamboo: Boolean) {
    val rows = PIP_ROWS[rank] ?: listOf(1)
    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        val rowH = size.height / rows.size
        rows.forEachIndexed { ri, count ->
            val colW = size.width / count
            for (ci in 0 until count) {
                val cx = colW * ci + colW / 2
                val cy = rowH * ri + rowH / 2
                if (isBamboo) {
                    val color = if (rank == 1) Theme.bambooGold else Theme.bambooInk
                    val w = colW * 0.5f
                    val h = (rowH * 0.72f).coerceAtMost(26f)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(cx - w / 2, cy - h / 2),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.3f)
                    )
                } else {
                    val color = circleColors[(ri + ci) % circleColors.size]
                    val r = (colW.coerceAtMost(rowH) * 0.32f)
                    drawCircle(color = color, radius = r, center = Offset(cx, cy))
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.3f),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f)
                    )
                }
            }
        }
    }
}
