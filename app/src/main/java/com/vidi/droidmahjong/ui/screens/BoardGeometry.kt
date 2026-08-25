package com.vidi.droidmahjong.ui.screens

import androidx.compose.ui.geometry.Offset
import com.vidi.droidmahjong.data.GameTile

/** Board extents computed from whichever tile set is actually on screen — Easy/Medium/Hard
 *  layouts each have different min/max/z bounds, so this can't be a fixed global. */
class BoardExtents(tiles: List<GameTile>) {
    val minX = tiles.minOfOrNull { it.x } ?: 0
    val maxX = tiles.maxOfOrNull { it.x } ?: 0
    val minY = tiles.minOfOrNull { it.y } ?: 0
    val maxY = tiles.maxOfOrNull { it.y } ?: 0
    val maxZ = tiles.maxOfOrNull { it.z } ?: 0

    // Must match the span point() actually produces -- an extra `+1` row/column here (beyond
    // what point() places tiles across) would pad only the trailing edge, since point()
    // anchors its leftmost/topmost content at exactly maxZ * LAYER_NUDGE with no matching
    // margin reserved on the near side, making the board visibly lean toward the top-left
    // inside its own frame.
    val width = (maxX - minX) * BoardGeometry.STEP_X + BoardGeometry.TILE_W + maxZ * BoardGeometry.LAYER_NUDGE * 2
    val height = (maxY - minY) * BoardGeometry.STEP_Y + BoardGeometry.TILE_H + maxZ * BoardGeometry.LAYER_NUDGE * 2
}

object BoardGeometry {
    const val TILE_W = 44f
    const val TILE_H = 60f
    val STEP_X = TILE_W * 0.86f
    val STEP_Y = TILE_H * 0.82f
    const val LAYER_NUDGE = 5f

    fun point(tile: GameTile, extents: BoardExtents): Offset {
        val nudge = tile.z * LAYER_NUDGE
        val maxNudge = extents.maxZ * LAYER_NUDGE
        val x = (tile.x - extents.minX) * STEP_X + TILE_W / 2 + (maxNudge - nudge)
        val y = (tile.y - extents.minY) * STEP_Y + TILE_H / 2 + (maxNudge - nudge)
        return Offset(x, y)
    }

    fun zIndex(tile: GameTile): Float = (tile.z * 1000 + tile.y * 20 + tile.x + 50).toFloat()
}
