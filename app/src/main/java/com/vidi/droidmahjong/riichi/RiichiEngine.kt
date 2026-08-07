package com.vidi.droidmahjong.riichi

enum class EnginePhase { DRAW, DISCARD, REACTION, ENDED }

data class ReactionOptions(
    val ron: Boolean = false,
    val pon: Boolean = false,
    val kan: Boolean = false,
    val chi: List<List<String>> = emptyList()
)

data class EvaluatedWin(val seat: Int, val win: YakuResult)

data class HandResult(
    val kind: String, // "tsumo", "ron", "exhaustive"
    val tsumoSeat: Int? = null,
    val tsumoWin: YakuResult? = null,
    val ronDiscarder: Int? = null,
    val ronWinners: List<EvaluatedWin> = emptyList(),
    val tenpaiSeats: List<Int> = emptyList(),
    val dealerTenpai: Boolean = false,
    val exhaustivePayments: List<Int> = listOf(0, 0, 0, 0)
)

sealed class ReactionResponse {
    object Pass : ReactionResponse()
    object Ron : ReactionResponse()
    object Pon : ReactionResponse()
    object Kan : ReactionResponse()
    data class Chi(val tile1: String, val tile2: String) : ReactionResponse()
}

sealed class ReactionResolution {
    object Advance : ReactionResolution()
    data class Called(val kind: String, val seat: Int) : ReactionResolution()
    object Ron : ReactionResolution()
}

class RiichiSeat(
    val wind: String,
    var hand: MutableList<String> = mutableListOf(),
    var melds: MutableList<Meld> = mutableListOf(),
    var discards: MutableList<Discard> = mutableListOf(),
    var riichi: Boolean = false,
    var ippatsu: Boolean = false,
    var points: Int = 25000,
    var passedRon: Boolean = false,
    var doubleRiichi: Boolean = false
)

data class Discard(val tile: String, val seat: Int, val calledAway: Boolean = false)

/** Chi options for `seat` on `discardedTile`, as (before, after) pairs still needed from hand. */
fun chiOptionsFor(hand: List<String>, discardedTile: String): List<List<String>> {
    val suit = SUIT_OF[discardedTile] ?: return emptyList()
    val rank = rankOf(discardedTile) ?: return emptyList()
    val counts = countsFromTiles(hand)
    val options = mutableListOf<List<String>>()

    fun has(r: Int) = r in 1..9 && (counts["$suit$r"] ?: 0) > 0

    if (has(rank - 2) && has(rank - 1)) options.add(listOf("$suit${rank - 2}", "$suit${rank - 1}"))
    if (has(rank - 1) && has(rank + 1)) options.add(listOf("$suit${rank - 1}", "$suit${rank + 1}"))
    if (has(rank + 1) && has(rank + 2)) options.add(listOf("$suit${rank + 1}", "$suit${rank + 2}"))

    return options
}

class RiichiEngine(
    val dealerSeat: Int = 0,
    val roundWind: String = "wE",
    val isBot: List<Boolean> = listOf(true, true, true, true),
    startingPoints: List<Int> = listOf(25000, 25000, 25000, 25000)
) {
    var phase: EnginePhase = EnginePhase.DRAW
        private set
    var currentSeat: Int = dealerSeat
        private set

    val seats: List<RiichiSeat> = listOf("wE", "wS", "wW", "wN").mapIndexed { i, w ->
        RiichiSeat(wind = w, points = startingPoints[i])
    }

    var liveWall: MutableList<String>
        private set
    private val deadWall: MutableList<String>
    private val fullDoraIndicators: List<String>
    private val fullUraDora: List<String>
    private var revealedDoraCount = 1

    var lastDiscard: Discard? = null
        private set
    var turnDrawnTile: String? = null
        private set
    var result: HandResult? = null
        private set

    private var reactionSummary: Map<Int, ReactionOptions> = emptyMap()

    init {
        val fullWall = buildWall()
        // Each seat's opening 13-tile hand, dealt in wind order.
        val hands = mutableListOf<MutableList<String>>()
        var cursor = 0
        for (s in 0 until 4) {
            hands.add(fullWall.subList(cursor, cursor + 13).toMutableList())
            cursor += 13
        }
        deadWall = fullWall.subList(cursor, cursor + 14).toMutableList()
        cursor += 14
        fullDoraIndicators = fullWall.subList(cursor, cursor + 4)
        cursor += 4
        fullUraDora = fullWall.subList(cursor, cursor + 4)
        cursor += 4
        liveWall = fullWall.subList(cursor, fullWall.size).toMutableList()

        for (i in 0 until 4) seats[i].hand = hands[i]
        // Dealer draws the 14th tile immediately as the hand's opening draw.
        seats[dealerSeat].hand.add(liveWall.removeAt(0))
        turnDrawnTile = seats[dealerSeat].hand.last()
        phase = EnginePhase.DISCARD
    }

    fun activeSeat(): RiichiSeat = seats[currentSeat]

    fun doraIndicators(): List<String> = fullDoraIndicators.take(revealedDoraCount)
    private fun uraDoraIndicators(): List<String> = fullUraDora.take(revealedDoraCount)

    private fun doraNextTile(indicator: String): String {
        if (isHonor(indicator)) {
            val winds = listOf("wE", "wS", "wW", "wN")
            val dragons = listOf("dR", "dG", "dW")
            return when {
                indicator in winds -> winds[(winds.indexOf(indicator) + 1) % 4]
                indicator in dragons -> dragons[(dragons.indexOf(indicator) + 1) % 3]
                else -> indicator
            }
        }
        val suit = SUIT_OF[indicator] ?: return indicator
        val rank = rankOf(indicator) ?: return indicator
        val next = if (rank == 9) 1 else rank + 1
        return "$suit$next"
    }

    private fun countDora(allTiles: List<String>): Int {
        val doraTiles = doraIndicators().map { doraNextTile(it) }
        return allTiles.count { it in doraTiles }
    }

    private fun countUraDora(allTiles: List<String>, seat: Int): Int {
        if (!seats[seat].riichi) return 0
        val doraTiles = uraDoraIndicators().map { doraNextTile(it) }
        return allTiles.count { it in doraTiles }
    }

    sealed class DrawOutcome {
        data class Drawn(val seat: Int, val tile: String, val rinshan: Boolean) : DrawOutcome()
        object Exhaustive : DrawOutcome()
    }

    fun draw(): DrawOutcome {
        if (liveWall.isEmpty()) {
            phase = EnginePhase.ENDED
            result = buildExhaustiveDraw()
            return DrawOutcome.Exhaustive
        }
        val tile = liveWall.removeAt(0)
        activeSeat().hand.add(tile)
        turnDrawnTile = tile
        phase = EnginePhase.DISCARD
        return DrawOutcome.Drawn(currentSeat, tile, rinshan = false)
    }

    private fun evaluateTsumo(): YakuResult? {
        val seat = activeSeat()
        val win = checkWin(seat.hand, seat.melds) ?: return null
        val ctx = WinContext(
            hand = seat.hand, melds = seat.melds, drawnTile = turnDrawnTile,
            winType = WinType.TSUMO, doraIndicators = doraIndicators(), uraDora = uraDoraIndicators(),
            isDealer = currentSeat == dealerSeat, seatWind = seat.wind, roundWind = roundWind
        )
        val best = bestYakuResult(win, ctx) ?: return null
        val allTiles = seat.hand + seat.melds.flatMap { it.tiles }
        val bonusHan = countDora(allTiles) + countUraDora(allTiles, currentSeat) +
            (if (seat.riichi) 1 else 0) + (if (seat.doubleRiichi) 1 else 0) + (if (seat.ippatsu) 1 else 0)
        val finalHan = best.han + bonusHan
        val scored = scorePoints(
            ScoreResult(finalHan, best.fu, ScorePayments(), baseScoreFromHanFu(finalHan, best.fu, currentSeat == dealerSeat, WinType.TSUMO)),
            currentSeat == dealerSeat, WinType.TSUMO
        )
        return best.copy(han = finalHan, total = scored.total, payments = scored.payments)
    }

    fun canDeclareTsumo(): Boolean = evaluateTsumo() != null

    fun declareTsumo(): HandResult? {
        val best = evaluateTsumo() ?: return null
        phase = EnginePhase.ENDED
        result = HandResult(kind = "tsumo", tsumoSeat = currentSeat, tsumoWin = best)
        return result
    }

    fun canRon(seat: Int): Boolean {
        if (seats[seat].passedRon) return false
        return evaluateRon(seat) != null
    }

    private fun evaluateRon(seat: Int): YakuResult? {
        val discard = lastDiscard ?: return null
        val hand = seats[seat].hand + discard.tile
        val win = checkWin(hand, seats[seat].melds) ?: return null
        val ctx = WinContext(
            hand = hand, melds = seats[seat].melds, drawnTile = discard.tile,
            winType = WinType.RON, doraIndicators = doraIndicators(), uraDora = uraDoraIndicators(),
            isDealer = seat == dealerSeat, seatWind = seats[seat].wind, roundWind = roundWind
        )
        val best = bestYakuResult(win, ctx) ?: return null
        val allTiles = hand + seats[seat].melds.flatMap { it.tiles }
        val bonusHan = countDora(allTiles) + countUraDora(allTiles, seat) +
            (if (seats[seat].riichi) 1 else 0) + (if (seats[seat].doubleRiichi) 1 else 0) + (if (seats[seat].ippatsu) 1 else 0)
        val finalHan = best.han + bonusHan
        val scored = scorePoints(
            ScoreResult(finalHan, best.fu, ScorePayments(), baseScoreFromHanFu(finalHan, best.fu, seat == dealerSeat, WinType.RON)),
            seat == dealerSeat, WinType.RON
        )
        return best.copy(han = finalHan, total = scored.total, payments = scored.payments)
    }

    fun discard(tile: String) {
        val seat = activeSeat()
        seat.hand.remove(tile)
        val d = Discard(tile, currentSeat)
        seat.discards.add(d)
        lastDiscard = d
        turnDrawnTile = null

        reactionSummary = buildReactionSummary()
        phase = if (reactionSummary.values.any { it.ron || it.pon || it.kan || it.chi.isNotEmpty() }) {
            EnginePhase.REACTION
        } else {
            currentSeat = (currentSeat + 1) % 4
            EnginePhase.DRAW
        }
    }

    private fun buildReactionSummary(): Map<Int, ReactionOptions> {
        val discard = lastDiscard ?: return emptyMap()
        val summary = mutableMapOf<Int, ReactionOptions>()
        for (s in 0 until 4) {
            if (s == discard.seat) continue
            val ron = canRon(s)
            val counts = countsFromTiles(seats[s].hand)
            val pon = (counts[discard.tile] ?: 0) >= 2
            val kan = (counts[discard.tile] ?: 0) >= 3
            val chi = if (s == (discard.seat + 1) % 4) chiOptionsFor(seats[s].hand, discard.tile) else emptyList()
            if (ron || pon || kan || chi.isNotEmpty()) {
                summary[s] = ReactionOptions(ron, pon, kan, chi)
            }
        }
        return summary
    }

    fun getReactionSummary(): Map<Int, ReactionOptions> = reactionSummary

    fun markPassedRon(seat: Int) {
        seats[seat].passedRon = true
    }

    fun resolveReactions(responses: Map<Int, ReactionResponse>): ReactionResolution {
        val discard = lastDiscard!!
        breakAllIppatsu()

        val ronSeats = responses.entries.filter { it.value == ReactionResponse.Ron }.map { it.key }.sorted()
        if (ronSeats.isNotEmpty()) {
            val winners = ronSeats.mapNotNull { s -> evaluateRon(s)?.let { EvaluatedWin(s, it) } }
            if (winners.isNotEmpty()) {
                phase = EnginePhase.ENDED
                result = HandResult(kind = "ron", ronDiscarder = discard.seat, ronWinners = winners)
                return ReactionResolution.Ron
            }
        }

        val kanSeat = responses.entries.firstOrNull { it.value == ReactionResponse.Kan }?.key
        if (kanSeat != null) {
            performDaiminkan(kanSeat, discard.tile)
            currentSeat = kanSeat
            phase = EnginePhase.DISCARD
            return ReactionResolution.Called("kan", kanSeat)
        }

        val ponSeat = responses.entries.firstOrNull { it.value == ReactionResponse.Pon }?.key
        if (ponSeat != null) {
            val hand = seats[ponSeat].hand
            hand.remove(discard.tile); hand.remove(discard.tile)
            seats[ponSeat].melds.add(Meld("pon", listOf(discard.tile, discard.tile, discard.tile)))
            currentSeat = ponSeat
            phase = EnginePhase.DISCARD
            return ReactionResolution.Called("pon", ponSeat)
        }

        val chiEntry = responses.entries.firstOrNull { it.value is ReactionResponse.Chi }
        if (chiEntry != null) {
            val chi = chiEntry.value as ReactionResponse.Chi
            val hand = seats[chiEntry.key].hand
            hand.remove(chi.tile1); hand.remove(chi.tile2)
            seats[chiEntry.key].melds.add(Meld("chi", listOf(discard.tile, chi.tile1, chi.tile2).sortedBy { rankOf(it) ?: 0 }))
            currentSeat = chiEntry.key
            phase = EnginePhase.DISCARD
            return ReactionResolution.Called("chi", chiEntry.key)
        }

        currentSeat = (discard.seat + 1) % 4
        phase = EnginePhase.DRAW
        return ReactionResolution.Advance
    }

    fun breakAllIppatsu() {
        for (s in seats) s.ippatsu = false
    }

    fun canDeclareRiichi(seat: Int): Boolean {
        val s = seats[seat]
        if (s.riichi || s.melds.isNotEmpty() || s.points < 1000) return false
        if (s.hand.size != 14) return false
        // Closed tenpai check: must be one tile away from a win for at least one discard choice.
        val counts = countsFromTiles(s.hand).keys
        return counts.any { discardCandidate ->
            val trial = s.hand.toMutableList()
            trial.remove(discardCandidate)
            isTenpaiHand(trial, s.melds)
        }
    }

    fun declareRiichi(seat: Int) {
        seats[seat].riichi = true
        seats[seat].ippatsu = true
        seats[seat].points -= 1000
        if (seats[seat].discards.isEmpty()) seats[seat].doubleRiichi = true
    }

    private fun isTenpaiHand(hand: List<String>, melds: List<Meld>): Boolean {
        val counts = countsFromTiles(hand)
        for (typeId in STANDARD_TYPE_IDS) {
            if ((counts[typeId] ?: 0) >= 4) continue
            val trial = hand + typeId
            if (checkWin(trial, melds) != null) return true
        }
        return false
    }

    fun isTenpai(seat: Int): Boolean = isTenpaiHand(seats[seat].hand, seats[seat].melds)

    fun canAnkan(seat: Int): List<String> {
        val counts = countsFromTiles(seats[seat].hand)
        return counts.filter { it.value == 4 }.keys.toList()
    }

    /** Draws the kan replacement tile from the dead wall, then slides the live wall's last
     *  tile into the dead wall so it stays at its fixed 14-tile size (standard rinshan rule). */
    private fun drawRinshanTile(): String? {
        if (deadWall.isEmpty()) return null
        val tile = deadWall.removeAt(0)
        if (liveWall.isNotEmpty()) deadWall.add(liveWall.removeAt(liveWall.size - 1))
        return tile
    }

    fun callAnkan(seat: Int, type: String): Boolean {
        val hand = seats[seat].hand
        val counts = countsFromTiles(hand)
        if ((counts[type] ?: 0) < 4) return false
        repeat(4) { hand.remove(type) }
        seats[seat].melds.add(Meld("ankan", listOf(type, type, type, type)))
        revealNewDoraIndicator()
        drawRinshanTile()?.let { hand.add(it); turnDrawnTile = it }
        phase = EnginePhase.DISCARD
        return true
    }

    fun canShouminkan(seat: Int): List<String> {
        val counts = countsFromTiles(seats[seat].hand)
        return seats[seat].melds.filter { it.kind == "pon" }.map { it.tiles[0] }.filter { (counts[it] ?: 0) >= 1 }
    }

    fun callShouminkan(seat: Int, type: String): Boolean {
        val hand = seats[seat].hand
        val melds = seats[seat].melds
        val ponIdx = melds.indexOfFirst { it.kind == "pon" && it.tiles[0] == type }
        if (ponIdx < 0 || !hand.contains(type)) return false
        hand.remove(type)
        melds[ponIdx] = Meld("shouminkan", melds[ponIdx].tiles + type)
        revealNewDoraIndicator()
        drawRinshanTile()?.let { hand.add(it); turnDrawnTile = it }
        phase = EnginePhase.DISCARD
        return true
    }

    private fun performDaiminkan(seat: Int, type: String) {
        val hand = seats[seat].hand
        repeat(3) { hand.remove(type) }
        seats[seat].melds.add(Meld("daiminkan", listOf(type, type, type, type)))
        revealNewDoraIndicator()
        drawRinshanTile()?.let { hand.add(it); turnDrawnTile = it }
    }

    private fun revealNewDoraIndicator() {
        if (revealedDoraCount < fullDoraIndicators.size) revealedDoraCount++
    }

    private fun buildExhaustiveDraw(): HandResult {
        val tenpaiSeats = (0 until 4).filter { isTenpai(it) }
        val payments = MutableList(4) { 0 }
        if (tenpaiSeats.isNotEmpty() && tenpaiSeats.size < 4) {
            val gain = 3000 / tenpaiSeats.size
            val lose = 3000 / (4 - tenpaiSeats.size)
            for (s in 0 until 4) payments[s] = if (s in tenpaiSeats) gain else -lose
        }
        return HandResult(
            kind = "exhaustive",
            tenpaiSeats = tenpaiSeats,
            dealerTenpai = dealerSeat in tenpaiSeats,
            exhaustivePayments = payments
        )
    }
}
