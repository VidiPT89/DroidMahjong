package com.vidi.droidmahjong.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Provably solvable" (see the top-level GameEngineTest doc) means there EXISTS a clearing
 * order, not that any order a player picks will clear the board — the dealer computes that
 * one order (walking a full reduction backwards) and stashes it on [GameEngine.provenSolveOrder]
 * specifically so this can be verified. That's what these tests replay, rather than greedily
 * following whichever hint comes up first, which isn't a guarantee this engine makes.
 */
class InfiniteModeTest {

    @Test
    fun `an infinite-mode board is solvable in its own proven order for several levels`() {
        for (level in listOf(1, 3, 8)) {
            val engine = GameEngine(Difficulty.INFINITE)
            repeat(level - 1) { engine.dealNextInfiniteLevel() }

            val totalTiles = engine.tiles.size
            assertEquals(totalTiles / 2, engine.provenSolveOrder.size)

            for ((a, b) in engine.provenSolveOrder) {
                val first = engine.select(a)
                assertTrue("expected $a to be selectable, got $first", first is SelectResult.Selected)
                val second = engine.select(b)
                assertTrue("expected $b to match, got $second", second is SelectResult.Matched)
            }

            assertEquals(0, engine.remaining())
        }
    }

    @Test
    fun `the board grows (or stays the same size) with every level and tile counts stay even`() {
        val engine = GameEngine(Difficulty.INFINITE)
        var previousSize = engine.tiles.size
        assertEquals(0, previousSize % 2)

        repeat(11) {
            engine.dealNextInfiniteLevel()
            assertEquals(0, engine.tiles.size % 2)
            assertTrue(
                "level ${engine.level} (${engine.tiles.size} tiles) should not be smaller than the previous level ($previousSize tiles)",
                engine.tiles.size >= previousSize
            )
            previousSize = engine.tiles.size
        }
    }

    @Test
    fun `dealNextInfiniteLevel advances the level without resetting cumulative moves`() {
        val engine = GameEngine(Difficulty.INFINITE)
        assertEquals(1, engine.level)

        val (a, b) = engine.provenSolveOrder[0]
        engine.select(a)
        engine.select(b)
        assertEquals(1, engine.moves)

        engine.dealNextInfiniteLevel()

        assertEquals(2, engine.level)
        assertEquals(1, engine.moves)
        assertEquals(engine.tiles.size, engine.remaining())
    }
}
