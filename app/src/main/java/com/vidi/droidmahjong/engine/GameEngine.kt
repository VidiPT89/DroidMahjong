package com.vidi.droidmahjong.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vidi.droidmahjong.data.GameTile
import com.vidi.droidmahjong.data.buildPairUnitsFromInventory
import com.vidi.droidmahjong.data.buildShuffledPairUnits
import com.vidi.droidmahjong.data.tilesMatch

data class RefPosition(val refId: Int, val x: Int, val y: Int, val z: Int)

class UnpairableBoardException : Exception()

/**
 * Given a list of (refId, x, y, z) positions, returns an ordered list of (refIdA, refIdB)
 * pairs such that each pair was mutually free — not covered, and open on at least one side —
 * at the moment it was paired, considering only the positions not yet paired earlier in the
 * list.
 *
 * Why this guarantees a solvable board: geometric freeness (covered / open-side) depends only
 * on which POSITIONS are still occupied, never on tile type. So once a matching tile type is
 * assigned to each pair here, playing the pairs back in this exact order reproduces this same
 * legal removal sequence on the real board.
 *
 * Each round collects every currently-free position and pairs them off two at a time; a
 * leftover odd one just stays free (freeness only grows as other tiles are removed) and joins
 * next round's pairing.
 */
fun computeSolvablePairing(positions: List<RefPosition>): List<Pair<Int, Int>> {
    val remaining = LinkedHashMap<Int, RefPosition>()
    positions.forEach { remaining[it.refId] = it }
    val pairs = mutableListOf<Pair<Int, Int>>()

    fun isCovered(p: RefPosition) =
        remaining.values.any { it.refId != p.refId && it.x == p.x && it.y == p.y && it.z > p.z }

    fun isOpenSide(p: RefPosition, dx: Int) =
        remaining.values.none { it.refId != p.refId && it.z == p.z && it.y == p.y && it.x == p.x + dx }

    fun isFree(p: RefPosition) = !isCovered(p) && (isOpenSide(p, -1) || isOpenSide(p, 1))

    while (remaining.isNotEmpty()) {
        val free = remaining.values.filter { isFree(it) }.shuffled()
        if (free.size < 2) throw UnpairableBoardException()
        var i = 0
        while (i + 1 < free.size) {
            pairs.add(free[i].refId to free[i + 1].refId)
            remaining.remove(free[i].refId)
            remaining.remove(free[i + 1].refId)
            i += 2
        }
        // An odd one out (if any) simply stays in `remaining` — it's still free next round
        // by construction, so the loop always makes progress.
    }
    return pairs
}

/**
 * computeSolvablePairing occasionally paints itself into a corner depending on which free
 * tiles get grouped together early on; retrying re-rolls that choice until a full reduction
 * succeeds.
 */
fun computeSolvablePairingWithRetry(positions: List<RefPosition>, maxAttempts: Int = 200): List<Pair<Int, Int>> {
    var lastError: Exception = UnpairableBoardException()
    repeat(maxAttempts) {
        try {
            return computeSolvablePairing(positions)
        } catch (e: UnpairableBoardException) {
            lastError = e
        }
    }
    throw lastError
}

sealed class SelectResult {
    object Ignored : SelectResult()
    data class Blocked(val tile: GameTile) : SelectResult()
    data class Selected(val tile: GameTile) : SelectResult()
    data class Deselected(val tile: GameTile) : SelectResult()
    data class Matched(val a: GameTile, val b: GameTile, val won: Boolean) : SelectResult()
    data class Mismatch(val previous: GameTile, val tile: GameTile) : SelectResult()
}

class GameEngine(initialDifficulty: Difficulty = Difficulty.MEDIUM) {
    var difficulty by mutableStateOf(initialDifficulty)
        private set
    var tiles = mutableStateListOf<GameTile>()
        private set
    var selectedId by mutableStateOf<Int?>(null)
        private set
    private var history = mutableListOf<Pair<Int, Int>>()
    var moves by mutableStateOf(0)
        private set
    var hintsUsed by mutableStateOf(0)
        private set
    var startedAtMillis: Long = System.currentTimeMillis()
        private set

    /** The construction-time pairing order used to guarantee solvability — kept for
     *  solvability testing, not used by normal gameplay. */
    var provenSolveOrder: List<Pair<Int, Int>> = emptyList()
        private set

    init {
        reset(initialDifficulty)
    }

    fun reset(newDifficulty: Difficulty = difficulty) {
        difficulty = newDifficulty
        tiles.clear()
        LAYOUTS.getValue(newDifficulty).forEachIndexed { i, pos ->
            tiles.add(GameTile(id = i, x = pos.x, y = pos.y, z = pos.z))
        }

        val refPositions = tiles.map { RefPosition(it.id, it.x, it.y, it.z) }
        val pairing = computeSolvablePairingWithRetry(refPositions)
        val units = buildPairUnitsForDifficulty(newDifficulty)

        pairing.forEachIndexed { idx, (a, b) ->
            val (typeA, typeB) = units[idx]
            setTypeId(a, typeA)
            setTypeId(b, typeB)
        }
        provenSolveOrder = pairing

        selectedId = null
        history = mutableListOf()
        moves = 0
        hintsUsed = 0
        startedAtMillis = System.currentTimeMillis()
    }

    private fun setTypeId(id: Int, typeId: String) {
        val idx = tiles.indexOfFirst { it.id == id }
        if (idx >= 0) tiles[idx] = tiles[idx].copy(typeId = typeId)
    }

    fun getTile(id: Int): GameTile? = tiles.firstOrNull { it.id == id }

    fun active(): List<GameTile> = tiles.filter { !it.removed }

    fun remaining(): Int = active().size

    fun isCovered(tile: GameTile): Boolean =
        tiles.any { !it.removed && it.x == tile.x && it.y == tile.y && it.z > tile.z }

    fun isOpenLeft(tile: GameTile): Boolean =
        tiles.none { !it.removed && it.z == tile.z && it.y == tile.y && it.x == tile.x - 1 }

    fun isOpenRight(tile: GameTile): Boolean =
        tiles.none { !it.removed && it.z == tile.z && it.y == tile.y && it.x == tile.x + 1 }

    fun isFree(tile: GameTile): Boolean {
        if (tile.removed) return false
        if (isCovered(tile)) return false
        return isOpenLeft(tile) || isOpenRight(tile)
    }

    fun freeTiles(): List<GameTile> = active().filter { isFree(it) }

    /** Attempts to select a tile; returns what happened so the UI can animate/react. */
    fun select(id: Int): SelectResult {
        val tile = getTile(id) ?: return SelectResult.Ignored
        if (tile.removed) return SelectResult.Ignored
        if (!isFree(tile)) return SelectResult.Blocked(tile)

        if (selectedId == id) {
            selectedId = null
            return SelectResult.Deselected(tile)
        }

        val currentSelectedId = selectedId
        if (currentSelectedId == null) {
            selectedId = id
            return SelectResult.Selected(tile)
        }

        val first = getTile(currentSelectedId)
        if (first == null) {
            selectedId = id
            return SelectResult.Selected(tile)
        }

        if (tilesMatch(first.typeId ?: "", tile.typeId ?: "")) {
            markRemoved(first.id)
            markRemoved(tile.id)
            selectedId = null
            moves++
            history.add(first.id to tile.id)
            return SelectResult.Matched(first, tile, remaining() == 0)
        }

        selectedId = id
        return SelectResult.Mismatch(first, tile)
    }

    private fun markRemoved(id: Int) {
        val idx = tiles.indexOfFirst { it.id == id }
        if (idx >= 0) tiles[idx] = tiles[idx].copy(removed = true)
    }

    private fun markRestored(id: Int) {
        val idx = tiles.indexOfFirst { it.id == id }
        if (idx >= 0) tiles[idx] = tiles[idx].copy(removed = false)
    }

    fun undo(): Pair<GameTile, GameTile>? {
        if (history.isEmpty()) return null
        val (a, b) = history.removeAt(history.size - 1)
        markRestored(a)
        markRestored(b)
        selectedId = null
        moves++
        return getTile(a)!! to getTile(b)!!
    }

    /** Finds one legal matching pair among currently free tiles, for the hint button. */
    fun findHint(): Pair<GameTile, GameTile>? {
        val free = freeTiles()
        for (i in free.indices) {
            for (j in i + 1 until free.size) {
                if (tilesMatch(free[i].typeId ?: "", free[j].typeId ?: "")) {
                    return free[i] to free[j]
                }
            }
        }
        return null
    }

    fun useHint() { hintsUsed++ }

    fun hasAvailableMove(): Boolean = findHint() != null

    fun isWon(): Boolean = remaining() == 0

    fun isStuck(): Boolean = !isWon() && !hasAvailableMove()

    /**
     * Reassigns tile types on the tiles still on the board (same positions), using the same
     * simultaneous-freeness pairing as the initial deal, so the remaining board is always
     * solvable again after a reshuffle.
     */
    fun reshuffle(): Boolean {
        val remainingTiles = active()
        val refPositions = remainingTiles.map { RefPosition(it.id, it.x, it.y, it.z) }
        val pairing = try {
            computeSolvablePairingWithRetry(refPositions)
        } catch (e: UnpairableBoardException) {
            return false
        }

        val typeCounts = mutableMapOf<String, Int>()
        remainingTiles.forEach { typeCounts[it.typeId ?: ""] = (typeCounts[it.typeId ?: ""] ?: 0) + 1 }
        val units = buildPairUnitsFromInventory(typeCounts)

        pairing.forEachIndexed { idx, (a, b) ->
            val (typeA, typeB) = units[idx]
            setTypeId(a, typeA)
            setTypeId(b, typeB)
        }
        provenSolveOrder = pairing

        selectedId = null
        return true
    }

    fun elapsedSeconds(): Long = (System.currentTimeMillis() - startedAtMillis) / 1000

    fun makeSnapshot(): GameSnapshot = GameSnapshot(
        tiles = tiles.toList(),
        selectedId = selectedId,
        historyPairs = history.map { listOf(it.first, it.second) },
        moves = moves,
        hintsUsed = hintsUsed,
        elapsedSeconds = elapsedSeconds(),
        difficulty = difficulty.name
    )

    fun restore(snapshot: GameSnapshot) {
        difficulty = Difficulty.entries.firstOrNull { it.name == snapshot.difficulty } ?: Difficulty.MEDIUM
        tiles.clear()
        tiles.addAll(snapshot.tiles)
        selectedId = snapshot.selectedId
        history = snapshot.historyPairs.map { it[0] to it[1] }.toMutableList()
        moves = snapshot.moves
        hintsUsed = snapshot.hintsUsed
        startedAtMillis = System.currentTimeMillis() - snapshot.elapsedSeconds * 1000
        provenSolveOrder = emptyList()
    }
}

data class GameSnapshot(
    val tiles: List<GameTile>,
    val selectedId: Int?,
    val historyPairs: List<List<Int>>,
    val moves: Int,
    val hintsUsed: Int,
    val elapsedSeconds: Long,
    val difficulty: String = Difficulty.MEDIUM.name
)
