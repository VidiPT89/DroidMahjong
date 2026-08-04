package com.vidi.droidmahjong.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.vidi.droidmahjong.data.GameTile
import com.vidi.droidmahjong.ui.theme.Theme
import kotlin.math.sin

@Composable
fun TileView(
    tile: GameTile,
    isFree: Boolean,
    isSelected: Boolean,
    isHinted: Boolean,
    shakeToken: Int,
    dealDelayMs: Long,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dealt by remember(tile.id) { mutableFloatStateOf(0f) }
    val dealtAnim by animateFloatAsState(
        targetValue = dealt,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 220f),
        label = "deal"
    )

    LaunchedEffect(tile.id) {
        kotlinx.coroutines.delay(dealDelayMs)
        dealt = 1f
    }

    val shakeAnim = remember(tile.id) { Animatable(0f) }
    LaunchedEffect(shakeToken) {
        if (shakeToken > 0) {
            shakeAnim.snapTo(0f)
            shakeAnim.animateTo(1f, animationSpec = tween(400, easing = LinearEasing))
        }
    }
    val shakeOffset = 6f * sin(shakeAnim.value * Math.PI.toFloat() * 6f)

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "scale"
    )
    val liftY by animateFloatAsState(
        targetValue = if (isSelected) -6f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "lift"
    )

    val borderColor = when {
        isSelected -> Theme.accent
        isHinted -> Theme.ok
        else -> Theme.tileEdge
    }
    val borderWidth = if (isSelected || isHinted) 2.5.dp else 1.dp

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(BoardGeometry.TILE_W.dp, BoardGeometry.TILE_H.dp)
            .graphicsLayer {
                scaleX = scale * (0.4f + 0.6f * dealtAnim)
                scaleY = scale * (0.4f + 0.6f * dealtAnim)
                translationX = shakeOffset
                translationY = liftY
                alpha = dealtAnim
            }
            .shadow(
                elevation = if (isSelected || isHinted) 10.dp else 2.dp,
                shape = RoundedCornerShape(6.dp),
                ambientColor = if (isSelected) Theme.accentGlow else Color.Black.copy(alpha = 0.25f),
                spotColor = if (isSelected) Theme.accentGlow else Color.Black.copy(alpha = 0.25f)
            )
            .background(
                Brush.verticalGradient(listOf(Theme.tileFace, Theme.tileFaceShade)),
                RoundedCornerShape(6.dp)
            )
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            )
    ) {
        tile.typeId?.let { typeId ->
            TileFaceView(typeId, modifier = Modifier.size(BoardGeometry.TILE_W.dp, BoardGeometry.TILE_H.dp))
        }
        if (!isFree) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .size(BoardGeometry.TILE_W.dp, BoardGeometry.TILE_H.dp)
                    .background(Color.Black.copy(alpha = 0.38f), RoundedCornerShape(6.dp))
            )
        }
    }
}
