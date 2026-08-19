package com.vidi.droidmahjong.engine

import com.vidi.droidmahjong.data.BoardPosition
import com.vidi.droidmahjong.data.buildEasyPairUnits
import com.vidi.droidmahjong.data.buildShuffledPairUnits

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

/** Easy — the base silhouette only, no stacking at all: every tile starts fully visible. */
fun buildEasyLayout(): List<BoardPosition> {
    val positions = mutableListOf<BoardPosition>()
    fun push(x: Int, y: Int, z: Int) { positions.add(BoardPosition(x, y, z)) }
    val baseRows = listOf(
        Triple(0, 2, 12), Triple(1, 1, 13), Triple(2, 0, 14), Triple(3, 0, 14),
        Triple(4, 0, 14), Triple(5, 0, 14), Triple(6, 1, 13), Triple(7, 2, 12)
    )
    for ((y, x0, x1) in baseRows) for (x in x0..x1) push(x, y, 0)
    return positions
}

val EASY_LAYOUT = buildEasyLayout()

/** Hard — same 144-tile inventory as the Turtle spread, but the peak is spread across 5
 *  upper layers instead of 4, so more of the board stays covered at any given moment. */
fun buildHardLayout(): List<BoardPosition> {
    val positions = mutableListOf<BoardPosition>()
    fun push(x: Int, y: Int, z: Int) { positions.add(BoardPosition(x, y, z)) }

    val baseRows = listOf(
        Triple(0, 2, 12), Triple(1, 1, 13), Triple(2, 0, 14), Triple(3, 0, 14),
        Triple(4, 0, 14), Triple(5, 0, 14), Triple(6, 1, 13), Triple(7, 2, 12)
    )
    for ((y, x0, x1) in baseRows) for (x in x0..x1) push(x, y, 0)
    push(-1, 3, 0); push(-1, 4, 0); push(15, 3, 0); push(15, 4, 0)

    for (y in 2..5) for (x in 6..9) push(x, y, 1)
    for (y in 3..4) for (x in 6..9) push(x, y, 2)
    for (y in 3..4) for (x in 7..8) push(x, y, 3)
    push(7, 3, 4); push(8, 3, 4)
    push(7, 3, 5); push(8, 3, 5)

    return positions
}

val HARD_LAYOUT = buildHardLayout()

/**
 * Procedural layout for the "Infinite" mode: a flat base rectangle that grows wider and
 * taller every couple of levels, with a shrinking stack of centered layers on top whose
 * count also grows with the level. Base width/height are always kept even, so every layer's
 * area stays even too, meaning the board never needs an odd tile discarded to stay pairable.
 *
 * Layers always shrink by 2 in each dimension and stop at 2x2 (same "never a lone unpaired
 * tile at the peak" rule as the hand-authored layouts above), so this feeds the exact same
 * computeSolvablePairingWithRetry() used everywhere else without any special-casing.
 */
fun buildInfiniteLayout(level: Int): List<BoardPosition> {
    val positions = mutableListOf<BoardPosition>()
    fun push(x: Int, y: Int, z: Int) { positions.add(BoardPosition(x, y, z)) }

    val baseW = 10 + 2 * ((level - 1) / 2)
    val baseH = 6 + 2 * ((level - 1) / 3)
    for (y in 0 until baseH) for (x in 0 until baseW) push(x, y, 0)

    val maxLayers = 1 + level / 2
    var w = baseW - 4
    var h = baseH - 2
    var z = 1
    while (w >= 2 && h >= 2 && z <= maxLayers) {
        val xOff = (baseW - w) / 2
        val yOff = (baseH - h) / 2
        for (yy in 0 until h) for (xx in 0 until w) push(xOff + xx, yOff + yy, z)
        w -= 2
        h -= 2
        z++
    }

    return positions
}

enum class Difficulty { EASY, MEDIUM, HARD, INFINITE }

val LAYOUTS: Map<Difficulty, List<BoardPosition>> = mapOf(
    Difficulty.EASY to EASY_LAYOUT,
    Difficulty.MEDIUM to TURTLE_LAYOUT,
    Difficulty.HARD to HARD_LAYOUT
)

val LAYOUT_TILE_COUNT: Map<Difficulty, Int> = LAYOUTS.mapValues { it.value.size }

/** Dispatches to the right pair-unit builder for the board size the difficulty deals. */
fun buildPairUnitsForDifficulty(difficulty: Difficulty): List<Pair<String, String>> =
    when (difficulty) {
        Difficulty.EASY -> buildEasyPairUnits()
        else -> buildShuffledPairUnits()
    }
