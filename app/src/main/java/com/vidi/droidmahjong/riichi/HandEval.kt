package com.vidi.droidmahjong.riichi

data class Meld(
    val kind: String, // "chi", "pon", "daiminkan", "ankan", "shouminkan"
    val tiles: List<String>
)

data class HandGroup(
    val kind: String, // "triplet", "sequence"
    val tiles: List<String>,
    val meld: Meld?
)

data class Decomposition(
    val groups: List<HandGroup>,
    val head: String
)

enum class HandKind { KOKUSHI, CHIITOITSU, REGULAR }

data class WinCheck(
    val kind: HandKind,
    val decompositions: List<Decomposition>
)

fun countsFromTiles(tiles: List<String>): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    for (t in tiles) counts[t] = (counts[t] ?: 0) + 1
    return counts
}

private fun meldsToGroups(melds: List<Meld>): List<HandGroup> = melds.map { m ->
    HandGroup(
        kind = if (m.kind == "chi") "sequence" else "triplet",
        tiles = m.tiles.take(3),
        meld = m
    )
}

/**
 * Recursively finds every way to decompose the closed part of the hand (13 or 14 concealed
 * tiles, minus called melds) into 4-groups-plus-a-pair. Concealed melds (ankan) count as one
 * of the four groups but keep all 4 tiles for fu scoring.
 */
fun findDecompositions(hand: List<String>, melds: List<Meld>): List<Decomposition> {
    val meldGroups = meldsToGroups(melds)
    val closedCounts = countsFromTiles(hand).toMutableMap()
    val decomps = mutableListOf<Decomposition>()

    fun recurse(remaining: MutableMap<String, Int>, groups: List<HandGroup>) {
        val nonZero = remaining.filterValues { it > 0 }
        if (nonZero.isEmpty()) {
            decomps.add(Decomposition(groups, ""))
            return
        }
        if (nonZero.values.sum() == 2) {
            val headTile = nonZero.keys.first()
            if (nonZero[headTile] == 2) {
                decomps.add(Decomposition(groups, headTile))
            }
            return
        }

        val tile = nonZero.keys.min()

        if ((remaining[tile] ?: 0) >= 3) {
            remaining[tile] = remaining[tile]!! - 3
            recurse(remaining, groups + HandGroup("triplet", listOf(tile, tile, tile), null))
            remaining[tile] = remaining[tile]!! + 3
        }

        val suit = SUIT_OF[tile]
        val rank = rankOf(tile)
        if (suit != null && rank != null && rank <= 7) {
            val next1 = "$suit${rank + 1}"
            val next2 = "$suit${rank + 2}"
            if ((remaining[next1] ?: 0) >= 1 && (remaining[next2] ?: 0) >= 1) {
                remaining[tile] = remaining[tile]!! - 1
                remaining[next1] = remaining[next1]!! - 1
                remaining[next2] = remaining[next2]!! - 1
                recurse(remaining, groups + HandGroup("sequence", listOf(tile, next1, next2), null))
                remaining[tile] = remaining[tile]!! + 1
                remaining[next1] = remaining[next1]!! + 1
                remaining[next2] = remaining[next2]!! + 1
            }
        }
    }

    recurse(closedCounts, meldGroups)
    return decomps.distinct()
}

fun isChiitoitsu(hand: List<String>): Boolean {
    if (hand.size != 14) return false
    val counts = countsFromTiles(hand)
    return counts.size == 7 && counts.values.all { it == 2 }
}

val KOKUSHI_TYPES = listOf("m1", "m9", "s1", "s9", "p1", "p9", "wE", "wS", "wW", "wN", "dR", "dG", "dW")

fun isKokushiMusou(hand: List<String>): Boolean {
    if (hand.size != 14) return false
    val counts = countsFromTiles(hand)
    if (counts.keys.any { it !in KOKUSHI_TYPES }) return false
    return counts.keys.size == 13 && counts.values.any { it == 2 } && counts.values.all { it <= 2 }
}

fun checkWin(hand: List<String>, melds: List<Meld>): WinCheck? {
    // Every meld (pon/chi/ankan/daiminkan/shouminkan) accounts for exactly one of the hand's
    // 4 groups regardless of whether it holds 3 or 4 physical tiles (kans draw a replacement
    // tile to compensate) — so the concealed hand alone must hold the other groups + the pair.
    if (hand.size != 14 - 3 * melds.size) return null

    if (melds.isEmpty() && isKokushiMusou(hand)) {
        return WinCheck(HandKind.KOKUSHI, emptyList())
    }

    if (melds.isEmpty() && isChiitoitsu(hand)) {
        return WinCheck(HandKind.CHIITOITSU, emptyList())
    }

    val decomps = findDecompositions(hand, melds)
    return if (decomps.isNotEmpty()) WinCheck(HandKind.REGULAR, decomps) else null
}
