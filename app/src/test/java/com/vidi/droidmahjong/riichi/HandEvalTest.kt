package com.vidi.droidmahjong.riichi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HandEval.kt]'s three winning-hand shapes: a regular 4-sets-plus-pair
 * decomposition, Chiitoitsu (seven pairs) and Kokushi Musou (thirteen orphans).
 *
 * Note on hand construction: [findDecompositions] always resolves the lexicographically
 * smallest remaining tile type first (into a triplet or a sequence starting at that tile), and
 * only recognises the pair once it is literally the only tile type left. So the pair's tile type
 * must sort after every other tile type used in the hand, or the search dead-ends before it ever
 * gets a chance to be treated as the pair. The hands below are built with that constraint in
 * mind (e.g. using a wind tile, which always sorts last, as the pair).
 */
class HandEvalTest {

    // --- Regular hand decomposition (4 groups + pair) -----------------------------------

    @Test
    fun `a regular 14-tile hand decomposes into four groups plus a pair`() {
        // m1-m2-m3, p7-p8-p9, s4-s5-s6 (sequences), dR-dR-dR (triplet), wE-wE (pair, sorts
        // after every suited tile so it is correctly resolved as the trailing pair).
        val hand = listOf(
            "m1", "m2", "m3",
            "p7", "p8", "p9",
            "s4", "s5", "s6",
            "dR", "dR", "dR",
            "wE", "wE"
        )

        val win = checkWin(hand, emptyList())
        assertNotNull("A well-formed 4-sets-plus-pair hand must be recognised as a win", win)
        assertEquals(HandKind.REGULAR, win!!.kind)
        assertTrue(win.decompositions.isNotEmpty())

        val decomp = win.decompositions.first { it.head == "wE" }
        assertEquals(4, decomp.groups.size)
        assertTrue(decomp.groups.any { it.kind == "triplet" && it.tiles == listOf("dR", "dR", "dR") })
        assertTrue(decomp.groups.count { it.kind == "sequence" } == 3)
    }

    @Test
    fun `a called meld accounts for one full group without needing tiles in the closed hand`() {
        // One pon of s5 already called; the remaining 11 concealed tiles must supply the
        // other 3 groups + pair: m1-m2-m3, p4-p5-p6, dG-dG-dG, wS-wS (pair sorts last).
        val hand = listOf(
            "m1", "m2", "m3",
            "p4", "p5", "p6",
            "dG", "dG", "dG",
            "wS", "wS"
        )
        val melds = listOf(Meld("pon", listOf("s5", "s5", "s5")))

        val win = checkWin(hand, melds)
        assertNotNull(win)
        assertEquals(HandKind.REGULAR, win!!.kind)
        val decomp = win.decompositions.first()
        // 3 concealed groups + 1 meld group = 4 groups total.
        assertEquals(4, decomp.groups.size)
        assertTrue(decomp.groups.any { it.meld?.kind == "pon" })
    }

    @Test
    fun `a hand that cannot be split into sets and a pair is not a win`() {
        // No triplets or runs possible here: an arbitrary scatter of singles/near-singles.
        val hand = listOf(
            "m1", "m3", "m5", "m7", "m9",
            "s2", "s4", "s6", "s8",
            "p1", "p3", "p9",
            "dR", "wN"
        )
        assertNull(checkWin(hand, emptyList()))
    }

    @Test
    fun `hand size must match 14 minus 3 tiles per called meld`() {
        // 14-tile hand but declaring a meld means only 11 concealed tiles are expected.
        val hand = listOf(
            "m1", "m2", "m3",
            "p7", "p8", "p9",
            "s4", "s5", "s6",
            "dR", "dR", "dR",
            "wE", "wE"
        )
        val melds = listOf(Meld("pon", listOf("s5", "s5", "s5")))
        assertNull("14 concealed tiles plus a called meld is not a legal 14-tile hand", checkWin(hand, melds))
    }

    // --- Chiitoitsu (seven pairs) ----------------------------------------------------------
    // isChiitoitsu is a plain tile-count check (not the sorted-recursion decomposer above), so
    // it has no tile-ordering constraints.

    @Test
    fun `seven distinct pairs are recognised as chiitoitsu`() {
        val hand = listOf(
            "m1", "m1", "m3", "m3", "m5", "m5",
            "s7", "s7", "s9", "s9",
            "p2", "p2",
            "dR", "dR"
        )
        assertTrue(isChiitoitsu(hand))

        val win = checkWin(hand, emptyList())
        assertNotNull(win)
        assertEquals(HandKind.CHIITOITSU, win!!.kind)
    }

    @Test
    fun `four of a kind does not count as two pairs for chiitoitsu`() {
        // Only 6 distinct tile types (one appears 4 times) — not 7 distinct pairs.
        val hand = listOf(
            "m1", "m1", "m1", "m1",
            "m3", "m3", "m5", "m5",
            "s7", "s7", "s9", "s9",
            "p2", "p2"
        )
        assertFalse(isChiitoitsu(hand))
    }

    // --- Kokushi Musou (thirteen orphans) --------------------------------------------------
    // isKokushiMusou is also a plain tile-membership/count check — no ordering constraints.

    @Test
    fun `all thirteen terminal-honor types plus one duplicate is kokushi musou`() {
        val hand = KOKUSHI_TYPES + "m1" // duplicate the m1 pair to reach 14 tiles
        assertEquals(14, hand.size)
        assertTrue(isKokushiMusou(hand))

        val win = checkWin(hand, emptyList())
        assertNotNull(win)
        assertEquals(HandKind.KOKUSHI, win!!.kind)
    }

    @Test
    fun `a single simple tile disqualifies a kokushi musou hand`() {
        // Replace the duplicated m1 with a simple (m2) — no longer all terminals/honors.
        val hand = KOKUSHI_TYPES.drop(1) + listOf("m2", "m2")
        assertEquals(14, hand.size)
        assertFalse(isKokushiMusou(hand))
    }
}
