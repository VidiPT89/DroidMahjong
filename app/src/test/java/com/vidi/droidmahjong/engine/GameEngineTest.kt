package com.vidi.droidmahjong.engine

import com.vidi.droidmahjong.data.GameTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Solitaire engine's two core guarantees:
 *  1. Every dealt board is provably solvable (see GameEngine.computeSolvablePairing's doc).
 *  2. The "free tile" rule — a tile is free only if nothing sits above it AND it has an open
 *     left or right neighbour on the same layer/row.
 */
class GameEngineTest {

    /**
     * Repeatedly asks the engine for a legal free matching pair (the same query the in-game
     * hint button uses) and removes it, until the board is fully cleared. If dealing ever
     * produced an unsolvable board, this loop would eventually find zero free matching pairs
     * while tiles remain, and the test would fail. Runs several shuffled deals per difficulty
     * since dealing involves randomness.
     */
    @Test
    fun `every dealt board is fully solvable by repeatedly clearing free pairs`() {
        val attemptsPerDifficulty = 15
        for (difficulty in Difficulty.entries) {
            repeat(attemptsPerDifficulty) { attempt ->
                val engine = GameEngine(difficulty)
                val maxSteps = engine.tiles.size // at most one pair removed per iteration
                var steps = 0

                while (engine.remaining() > 0) {
                    steps++
                    assertTrue(
                        "Board for $difficulty (attempt $attempt) did not shrink within $maxSteps steps",
                        steps <= maxSteps
                    )

                    val pair = engine.findHint()
                    assertNotNull(
                        "No legal free matching pair found for $difficulty with " +
                            "${engine.remaining()} tiles left (attempt $attempt) — the deal is not solvable",
                        pair
                    )
                    val (a, b) = pair!!

                    val firstResult = engine.select(a.id)
                    assertTrue(
                        "Expected selecting the first hinted tile to succeed, got $firstResult",
                        firstResult is SelectResult.Selected
                    )
                    val secondResult = engine.select(b.id)
                    assertTrue(
                        "Expected the hinted pair to match, got $secondResult",
                        secondResult is SelectResult.Matched
                    )
                }

                assertEquals(0, engine.remaining())
                assertTrue(engine.isWon())
            }
        }
    }

    @Test
    fun `dealt board tile count matches the difficulty's layout size`() {
        assertEquals(108, GameEngine(Difficulty.EASY).tiles.size)
        assertEquals(144, GameEngine(Difficulty.MEDIUM).tiles.size)
        assertEquals(144, GameEngine(Difficulty.HARD).tiles.size)
    }

    // --- Free tile rule -----------------------------------------------------------------
    // isFree(tile) == !isCovered(tile) && (isOpenLeft(tile) || isOpenRight(tile))
    // These tests replace the engine's dealt tiles with a small synthetic board so the rule
    // can be checked in isolation, using the engine's own public isCovered/isOpenLeft/
    // isOpenRight/isFree queries.

    @Test
    fun `a tile with another tile stacked directly above it is covered and not free`() {
        val engine = GameEngine(Difficulty.EASY)
        engine.tiles.clear()
        // Base tile at (0,0,0), fully covered by a tile at the same (x,y) one layer up.
        engine.tiles.add(GameTile(id = 1, x = 0, y = 0, z = 0))
        engine.tiles.add(GameTile(id = 2, x = 0, y = 0, z = 1))

        val base = engine.getTile(1)!!
        val top = engine.getTile(2)!!

        assertTrue(engine.isCovered(base))
        assertFalse("A covered tile must never be free, regardless of open sides", engine.isFree(base))

        assertFalse(engine.isCovered(top))
        assertTrue("The uncovered top tile has both sides open — it must be free", engine.isFree(top))
    }

    @Test
    fun `an uncovered tile boxed in on both sides is blocked, not free`() {
        val engine = GameEngine(Difficulty.EASY)
        engine.tiles.clear()
        // Three tiles in a row on the same layer/row: x=0, x=1, x=2.
        engine.tiles.add(GameTile(id = 1, x = 0, y = 0, z = 0))
        engine.tiles.add(GameTile(id = 2, x = 1, y = 0, z = 0))
        engine.tiles.add(GameTile(id = 3, x = 2, y = 0, z = 0))

        val middle = engine.getTile(2)!!
        assertFalse(engine.isCovered(middle))
        assertFalse(engine.isOpenLeft(middle))
        assertFalse(engine.isOpenRight(middle))
        assertFalse("A tile boxed in on both sides must not be free", engine.isFree(middle))
    }

    @Test
    fun `an uncovered tile open on at least one side is free`() {
        val engine = GameEngine(Difficulty.EASY)
        engine.tiles.clear()
        engine.tiles.add(GameTile(id = 1, x = 0, y = 0, z = 0))
        engine.tiles.add(GameTile(id = 2, x = 1, y = 0, z = 0))
        engine.tiles.add(GameTile(id = 3, x = 2, y = 0, z = 0))

        val left = engine.getTile(1)!!
        val right = engine.getTile(3)!!
        assertTrue("Leftmost tile has no left neighbour — it should be free", engine.isFree(left))
        assertTrue("Rightmost tile has no right neighbour — it should be free", engine.isFree(right))
    }

    @Test
    fun `coverage blocks freeness even when a tile is geometrically open on both sides`() {
        val engine = GameEngine(Difficulty.EASY)
        engine.tiles.clear()
        // A single isolated tile at (0,0,0) has both sides open — on its own it would be free...
        engine.tiles.add(GameTile(id = 1, x = 0, y = 0, z = 0))
        // ...but stacking a tile directly above it must still block it: coverage always wins.
        engine.tiles.add(GameTile(id = 2, x = 0, y = 0, z = 1))

        val base = engine.getTile(1)!!
        assertTrue(engine.isOpenLeft(base))
        assertTrue(engine.isOpenRight(base))
        assertTrue(engine.isCovered(base))
        assertFalse("Coverage must block freeness regardless of open sides", engine.isFree(base))
    }

    @Test
    fun `removed tiles never count as covering or blocking neighbours`() {
        val engine = GameEngine(Difficulty.EASY)
        engine.tiles.clear()
        engine.tiles.add(GameTile(id = 1, x = 0, y = 0, z = 0))
        engine.tiles.add(GameTile(id = 2, x = 1, y = 0, z = 0, removed = true))
        engine.tiles.add(GameTile(id = 3, x = 2, y = 0, z = 0))

        val left = engine.getTile(1)!!
        // Its right neighbour (id 2) has already been removed, so that side counts as open.
        assertTrue(engine.isOpenRight(left))
        assertTrue(engine.isFree(left))
    }
}
