package com.vidi.droidmahjong.riichi

/** How much a tile contributes to shapes in the rest of the hand — duplicates and nearby
 *  ranks in the same suit raise it; lone honors/terminals score lowest and get cut first. */
fun tileUsefulness(tile: String, hand: List<String>): Double {
    val counts = countsFromTiles(hand)
    var score = ((counts[tile] ?: 1) - 1) * 2.0 // extra copies of this exact tile

    val suit = SUIT_OF[tile]
    val rank = rankOf(tile)
    if (suit != null && rank != null) {
        for (d in -2..2) {
            if (d == 0) continue
            val r = rank + d
            if (r in 1..9 && (counts["$suit$r"] ?: 0) > 0) score += if (kotlin.math.abs(d) == 1) 1.0 else 0.5
        }
    } else {
        score -= 0.5 // isolated honors are the least flexible tile in hand
    }
    return score
}

fun chooseBotDiscard(hand: List<String>, melds: List<Meld>): String {
    return hand.minByOrNull { tileUsefulness(it, hand) } ?: hand.first()
}

fun chooseBotReaction(engine: RiichiEngine, seat: Int, opts: ReactionOptions): ReactionResponse {
    if (opts.ron) return ReactionResponse.Ron
    if (opts.pon) return ReactionResponse.Pon
    if (opts.chi.isNotEmpty()) return ReactionResponse.Chi(opts.chi[0][0], opts.chi[0][1])
    return ReactionResponse.Pass
}

/** Bots keep it simple and never voluntarily riichi/ankan mid-hand — declaring correctly
 *  requires the same tenpai search the UI uses, and casual bots favor predictable play. */
fun chooseBotRiichi(engine: RiichiEngine, seat: Int): Boolean = false

fun chooseBotAnkan(engine: RiichiEngine, seat: Int): String? = null
