package com.vidi.droidmahjong.ui.screens

import androidx.compose.ui.geometry.Offset
import com.vidi.droidmahjong.data.GameTile
import com.vidi.droidmahjong.engine.TURTLE_LAYOUT

object BoardGeometry {
    const val TILE_W = 44f
    const val TILE_H = 60f
    val STEP_X = TILE_W * 0.86f
    val STEP_Y = TILE_H * 0.82f
    const val LAYER_NUDGE = 5f
    const val MAX_LAYER = 4f

    val minX = TURTLE_LAYOUT.minOf { it.x }
    val maxX = TURTLE_LAYOUT.maxOf { it.x }
    val minY = TURTLE_LAYOUT.minOf { it.y }
    val maxY = TURTLE_LAYOUT.maxOf { it.y }

    val boardWidth = (maxX - minX + 1) * STEP_X + TILE_W + MAX_LAYER * LAYER_NUDGE * 2
    val boardHeight = (maxY - minY + 1) * STEP_Y + TILE_H + MAX_LAYER * LAYER_NUDGE * 2

    fun point(tile: GameTile): Offset {
        val nudge = tile.z * LAYER_NUDGE
        val x = (tile.x - minX) * STEP_X + TILE_W / 2 + (MAX_LAYER * LAYER_NUDGE - nudge)
        val y = (tile.y - minY) * STEP_Y + TILE_H / 2 + (MAX_LAYER * LAYER_NUDGE - nudge)
        return Offset(x, y)
    }

    fun zIndex(tile: GameTile): Float = (tile.z * 1000 + tile.y * 20 + tile.x + 50).toFloat()
}
