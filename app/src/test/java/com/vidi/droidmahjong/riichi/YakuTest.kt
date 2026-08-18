package com.vidi.droidmahjong.riichi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the most common yaku detected by [evaluateDecomposition] in Yaku.kt: Riichi,
 * Tanyao, Yakuhai, Pinfu and Toitoi. Each test builds a concrete winning hand (verified end to
 * end through [checkWin] + [bestYakuResult], the same path the engine uses) that is specifically
 * shaped to trigger the yaku under test while avoiding unrelated ones, so the assertions are
 * unambiguous about which detector fired.
 *
 * Note on hand construction: [findDecompositions] always resolves the lexicographically smallest
 * remaining tile type first (into a triplet or a sequence starting at that tile) with no
 * backtracking to "skip" a stuck tile type, so the pair's tile type must sort after every other
 * type used in the hand (types compare as d < m < p < s < w) or the search dead-ends before it
 * ever gets to treat it as the pair. Hands below are ordered with that in mind — see the
 * equivalent note in HandEvalTest.kt.
 */
class YakuTest {

    private fun win(hand: List<String>, melds: List<Meld> = emptyList(), ctx: WinContext): YakuResult {
        val check = checkWin(hand, melds)
        assertNotNull("Hand was expected to be a valid win: $hand", check)
        val result = bestYakuResult(check!!, ctx)
        assertNotNull("Expected at least one yaku to be found for $hand", result)
        return result!!
    }

    @Test
    fun `riichi yaku is awarded when the context declares riichi`() {
        // m2-m3-m4, m6-m6-m6 (sequence + triplet, both in the m suit, non-yakuhai),
        // p3-p4-p5, s5-s6-s7 (sequences), pair wN-wN (a wind, sorts last so it resolves as
        // the trailing pair). Closed hand, tsumo, riichi declared.
        val hand = listOf(
            "m2", "m3", "m4",
            "m6", "m6", "m6",
            "p3", "p4", "p5",
            "s5", "s6", "s7",
            "wN", "wN"
        )
        val ctx = WinContext(
            hand = hand,
            melds = emptyList(),
            drawnTile = "wN",
            winType = WinType.TSUMO,
            doraIndicators = emptyList(),
            uraDora = emptyList(),
            isDealer = false,
            riichi = true
        )
        val result = win(hand, ctx = ctx)
        assertTrue(
            "Expected a Riichi yaku entry, got ${result.yakus.map { it.name }}",
            result.yakus.any { it.name == "Riichi" && it.han == 1 }
        )
    }

    @Test
    fun `tanyao is awarded when every tile in the hand is a simple`() {
        // m2-m3-m4, p2-p3-p4, p6-p6-p6, s2-s3-s4 (sequences/triplet), pair s8-s8 — every tile
        // is rank 2-8, no terminals or honors anywhere, and s8 sorts after every other type used.
        val hand = listOf(
            "m2", "m3", "m4",
            "p2", "p3", "p4",
            "p6", "p6", "p6",
            "s2", "s3", "s4",
            "s8", "s8"
        )
        val ctx = WinContext(
            hand = hand,
            melds = emptyList(),
            drawnTile = "s8",
            winType = WinType.RON,
            doraIndicators = emptyList(),
            uraDora = emptyList(),
            isDealer = false
        )
        val result = win(hand, ctx = ctx)
        assertTrue(
            "Expected a Tanyao yaku entry, got ${result.yakus.map { it.name }}",
            result.yakus.any { it.name == "Tanyao" && it.han == 1 }
        )
    }

    @Test
    fun `a dragon triplet is awarded as a yakuhai entry`() {
        // dR-dR-dR (Red Dragon triplet — dragons sort first, so it resolves cleanly up front),
        // m2-m3-m4, p3-p4-p5, s5-s6-s7 (sequences), pair wN-wN (sorts last).
        val hand = listOf(
            "dR", "dR", "dR",
            "m2", "m3", "m4",
            "p3", "p4", "p5",
            "s5", "s6", "s7",
            "wN", "wN"
        )
        val ctx = WinContext(
            hand = hand,
            melds = emptyList(),
            drawnTile = "wN",
            winType = WinType.RON,
            doraIndicators = emptyList(),
            uraDora = emptyList(),
            isDealer = false
        )
        val result = win(hand, ctx = ctx)
        assertTrue(
            "Expected a dragon yakuhai entry for the dR triplet, got ${result.yakus.map { it.name }}",
            result.yakus.any { it.name.contains("Chun") && it.han == 1 }
        )
    }

    @Test
    fun `a round-wind triplet is also a yakuhai entry`() {
        // wE-wE-wE with roundWind = wE (the default): the round-wind triplet is yakuhai.
        // Pair is wN-wN — "wE" sorts before "wN" (E < N), so the triplet still resolves before
        // the pair once every other suit is out of the way.
        val hand = listOf(
            "m2", "m3", "m4",
            "p3", "p4", "p5",
            "s5", "s6", "s7",
            "wE", "wE", "wE",
            "wN", "wN"
        )
        val ctx = WinContext(
            hand = hand,
            melds = emptyList(),
            drawnTile = "wN",
            winType = WinType.RON,
            doraIndicators = emptyList(),
            uraDora = emptyList(),
            isDealer = false,
            roundWind = "wE"
        )
        val result = win(hand, ctx = ctx)
        assertTrue(
            "Expected a Round Wind yakuhai entry, got ${result.yakus.map { it.name }}",
            result.yakus.any { it.name == "Round Wind" && it.han == 1 }
        )
    }

    @Test
    fun `pinfu is awarded for a closed all-sequence hand with a non-yakuhai pair`() {
        // Four sequences (m1-m2-m3, m4-m5-m6, p7-p8-p9, s4-s5-s6) plus a non-yakuhai pair
        // (s8-s8, a suited simple that sorts after the s4-s6 sequence it shares a suit with).
        val hand = listOf(
            "m1", "m2", "m3",
            "m4", "m5", "m6",
            "p7", "p8", "p9",
            "s4", "s5", "s6",
            "s8", "s8"
        )
        val ctx = WinContext(
            hand = hand,
            melds = emptyList(),
            drawnTile = "s8",
            winType = WinType.RON,
            doraIndicators = emptyList(),
            uraDora = emptyList(),
            isDealer = false
        )
        val result = win(hand, ctx = ctx)
        assertTrue(
            "Expected a Pinfu yaku entry, got ${result.yakus.map { it.name }}",
            result.yakus.any { it.name == "Pinfu" && it.han == 1 }
        )
        // Pinfu on a ron win has a fixed 30 fu, regardless of the groups involved.
        assertEquals(30, result.fu)
    }

    @Test
    fun `toitoi is awarded for an all-triplets hand`() {
        // One called pon (s5-s5-s5) plus three concealed triplets (m3-m3-m3, p7-p7-p7,
        // wN-wN-wN) and a pair (wS-wS, sorting after wN so it resolves last) — every group is a
        // triplet, but only 3 are concealed (ankouCount == 3) so this stays Toitoi rather than
        // escalating to the Suuankou yakuman.
        val hand = listOf(
            "m3", "m3", "m3",
            "p7", "p7", "p7",
            "wN", "wN", "wN",
            "wS", "wS"
        )
        val melds = listOf(Meld("pon", listOf("s5", "s5", "s5")))
        val ctx = WinContext(
            hand = hand,
            melds = melds,
            drawnTile = "wS",
            winType = WinType.RON,
            doraIndicators = emptyList(),
            uraDora = emptyList(),
            isDealer = false
        )
        val result = win(hand, melds, ctx)
        assertTrue(
            "Expected a Toitoi yaku entry, got ${result.yakus.map { it.name }}",
            result.yakus.any { it.name == "Toitoi" && it.han == 2 }
        )
    }
}
