package com.vidi.droidmahjong.engine

import com.vidi.droidmahjong.data.BoardPosition

/**
 * Board layout: fixed coordinate table for the 144-tile "Turtle" spread.
 * Grid is aligned (no half-cell offset between layers) — free-tile checks
 * only ever compare integer (x, y, z), which keeps the covering/open-side
 * rules simple and unambiguous. Visual staggering between layers is done
 * purely at render time; it never touches game logic.
 */
fun buildTurtleLayout(): List<BoardPosition> {
    val positions = mutableListOf<BoardPosition>()
    fun push(x: Int, y: Int, z: Int) { positions.add(BoardPosition(x, y, z)) }

    // Layer 0 — base silhouette (widths: 11,13,15,15,15,15,13,11 = 108)
    val baseRows = listOf(
        Triple(0, 2, 12), Triple(1, 1, 13), Triple(2, 0, 14), Triple(3, 0, 14),
        Triple(4, 0, 14), Triple(5, 0, 14), Triple(6, 1, 13), Triple(7, 2, 12)
    )
    for ((y, x0, x1) in baseRows) {
        for (x in x0..x1) push(x, y, 0)
    }

    // Flippers — two single-tile columns left/right at the mid rows (4)
    push(-1, 3, 0)
    push(-1, 4, 0)
    push(15, 3, 0)
    push(15, 4, 0)

    // Every upper layer is at least 2 tiles wide on every row, so a lone unpairable tile
    // can never occur at the peak (see GameEngine.computeSolvablePairing for why width-1
    // layers are risky: a single isolated tile has no simultaneous partner).

    // Layer 1 — 4 wide x 4 tall, centered (16)
    for (y in 2..5) for (x in 6..9) push(x, y, 1)

    // Layer 2 — 4 wide x 2 tall (8)
    for (y in 3..4) for (x in 6..9) push(x, y, 2)

    // Layer 3 — 2 wide x 2 tall (4)
    for (y in 3..4) for (x in 7..8) push(x, y, 3)

    // Layer 4 — the cap, 2 wide x 2 tall (4)
    for (y in 3..4) for (x in 7..8) push(x, y, 4)

    return positions
}

val TURTLE_LAYOUT = buildTurtleLayout()
