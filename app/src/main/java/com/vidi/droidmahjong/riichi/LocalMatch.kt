package com.vidi.droidmahjong.riichi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Orchestrates a full local match (bots + humans on one device): chains RiichiEngine hands
 * together with dealer rotation and running points, and drives bot turns automatically.
 * Compose UI observes this class's mutableStateOf properties and calls the human* methods
 * in response to taps.
 *
 * Scope simplification: plays a single East round (4 hands, one dealer turn each) — a common
 * casual "tonpuusen" format — rather than a full East+South hanchan.
 */
private const val BOT_DELAY_MS = 550L

sealed class MatchEvent {
    object HandStart : MatchEvent()
    data class Drawn(val seat: Int, val rinshan: Boolean) : MatchEvent()
    object AwaitHumanDiscard : MatchEvent()
    data class AwaitHumanReaction(val seatIndex: Int, val opts: ReactionOptions) : MatchEvent()
    data class Discarded(val seat: Int, val tile: String) : MatchEvent()
    data class Called(val kind: String, val seat: Int) : MatchEvent()
    data class Riichi(val seat: Int) : MatchEvent()
    data class Kan(val seat: Int) : MatchEvent()
    data class HandEnd(val result: HandResult) : MatchEvent()
    object MatchEnd : MatchEvent()
}

class LocalMatch(val isHuman: List<Boolean>, private val scope: CoroutineScope) {
    var engine: RiichiEngine? by mutableStateOf(null)
        private set
    var points: List<Int> by mutableStateOf(listOf(25000, 25000, 25000, 25000))
        private set
    var riichiSticksOnTable by mutableStateOf(0)
        private set
    var matchOver by mutableStateOf(false)
        private set
    var pendingHumanSeats: List<Int> by mutableStateOf(emptyList())
        private set
    var awaitingHumanReaction by mutableStateOf(false)
        private set
    var lastEvent: MatchEvent? by mutableStateOf(null)
        private set

    var roundWind by mutableStateOf("wE")
        private set
    var handNumber by mutableStateOf(1)
        private set
    var dealerSeat by mutableStateOf(0)
        private set

    private var pendingReactionResponses: MutableMap<Int, ReactionResponse> = mutableMapOf()
    private var gen = 0

    private fun emit(event: MatchEvent) {
        lastEvent = event
    }

    /** Schedules fn after delayMs, tagged with the current generation — if state moved on
     *  (a new hand started, the match stopped) before it fires, it's a no-op. */
    private fun scheduleAfter(delayMs: Long, fn: () -> Unit) {
        val g = ++gen
        scope.launch {
            delay(delayMs)
            if (gen == g) fn()
        }
    }

    fun startHand() {
        val g = RiichiEngine(
            dealerSeat = dealerSeat,
            roundWind = roundWind,
            startingPoints = points
        )
        engine = g
        emit(MatchEvent.HandStart)
        tick()
    }

    fun currentSeatIsHuman(): Boolean = engine?.let { isHuman[it.currentSeat] } ?: false

    /** Advances the state machine until it needs human input or the hand ends. */
    private fun tick() {
        val g = engine ?: return
        when (g.phase) {
            EnginePhase.ENDED -> finishHand()
            EnginePhase.DRAW -> {
                val wasHuman = currentSeatIsHuman()
                when (val outcome = g.draw()) {
                    is RiichiEngine.DrawOutcome.Exhaustive -> finishHand()
                    is RiichiEngine.DrawOutcome.Drawn -> {
                        emit(MatchEvent.Drawn(outcome.seat, outcome.rinshan))
                        if (wasHuman) emit(MatchEvent.AwaitHumanDiscard)
                        else scheduleAfter(BOT_DELAY_MS) { runBotTurn() }
                    }
                }
            }
            EnginePhase.REACTION -> resolveReactionPhase()
            EnginePhase.DISCARD -> {
                // Reached right after a pon/chi/kan call (not a draw) — the caller still owes
                // an immediate discard.
                if (currentSeatIsHuman()) emit(MatchEvent.AwaitHumanDiscard)
                else scheduleAfter(BOT_DELAY_MS) { runBotTurn() }
            }
        }
    }

    private fun runBotTurn() {
        val g = engine ?: return
        if (g.canDeclareTsumo()) {
            g.declareTsumo()
            finishHand()
            return
        }
        val ankanChoice = chooseBotAnkan(g, g.currentSeat)
        if (ankanChoice != null && g.callAnkan(g.currentSeat, ankanChoice)) {
            emit(MatchEvent.Kan(g.currentSeat))
            scheduleAfter(BOT_DELAY_MS) { tick() }
            return
        }
        if (chooseBotRiichi(g, g.currentSeat) && g.canDeclareRiichi(g.currentSeat)) {
            g.declareRiichi(g.currentSeat)
            emit(MatchEvent.Riichi(g.currentSeat))
        }
        val seat = g.activeSeat()
        val tile = if (seat.riichi) g.turnDrawnTile ?: seat.hand.last() else chooseBotDiscard(seat.hand, seat.melds)
        g.discard(tile)
        emit(MatchEvent.Discarded(g.lastDiscard!!.seat, tile))
        scheduleAfter(BOT_DELAY_MS) { tick() }
    }

    private fun resolveReactionPhase() {
        val g = engine ?: return
        val summary = g.getReactionSummary()
        val responses = mutableMapOf<Int, ReactionResponse>()
        // Multiple human seats can simultaneously have a valid reaction (e.g. double ron) —
        // all must be asked, not just one.
        val humanPending = mutableListOf<Pair<Int, ReactionOptions>>()

        for (seatIndex in summary.keys.sorted()) {
            val opts = summary[seatIndex]!!
            if (isHuman[seatIndex]) {
                val hasAny = opts.ron || opts.pon || opts.kan || opts.chi.isNotEmpty()
                if (hasAny) { humanPending.add(seatIndex to opts); continue }
                responses[seatIndex] = ReactionResponse.Pass
                continue
            }
            val decision = chooseBotReaction(g, seatIndex, opts)
            if (decision == ReactionResponse.Ron) { responses[seatIndex] = ReactionResponse.Ron; continue }
            if (opts.ron) g.markPassedRon(seatIndex)
            responses[seatIndex] = decision
        }

        if (humanPending.isNotEmpty()) {
            pendingReactionResponses = responses
            pendingHumanSeats = humanPending.map { it.first }
            awaitingHumanReaction = true
            for ((seatIdx, opts) in humanPending) emit(MatchEvent.AwaitHumanReaction(seatIdx, opts))
            return
        }

        applyReactions(responses)
    }

    private fun applyReactions(responses: Map<Int, ReactionResponse>) {
        val g = engine ?: return
        when (val r = g.resolveReactions(responses)) {
            is ReactionResolution.Ron -> finishHand()
            is ReactionResolution.Called -> {
                emit(MatchEvent.Called(r.kind, r.seat))
                scheduleAfter(BOT_DELAY_MS) { tick() }
            }
            is ReactionResolution.Advance -> scheduleAfter(80L) { tick() }
        }
    }

    // MARK: - Human actions (called by the UI)

    fun humanDiscard(tile: String) {
        val g = engine ?: return
        if (g.phase != EnginePhase.DISCARD || !currentSeatIsHuman()) return
        g.discard(tile)
        emit(MatchEvent.Discarded(g.lastDiscard!!.seat, tile))
        scheduleAfter(80L) { tick() }
    }

    fun humanDeclareTsumo() {
        val g = engine ?: return
        if (!currentSeatIsHuman() || g.declareTsumo() == null) return
        finishHand()
    }

    fun humanDeclareRiichi() {
        val g = engine ?: return
        g.declareRiichi(g.currentSeat)
        emit(MatchEvent.Riichi(g.currentSeat))
    }

    fun humanAnkan(type: String) {
        val g = engine ?: return
        if (!currentSeatIsHuman() || !g.callAnkan(g.currentSeat, type)) return
        emit(MatchEvent.Kan(g.currentSeat))
        scheduleAfter(BOT_DELAY_MS) { tick() }
    }

    fun humanReact(seatIndex: Int, action: ReactionResponse) {
        if (seatIndex !in pendingHumanSeats) return
        val g = engine ?: return
        if (action is ReactionResponse.Pass && g.canRon(seatIndex)) g.markPassedRon(seatIndex)
        pendingReactionResponses[seatIndex] = action
        pendingHumanSeats = pendingHumanSeats.filter { it != seatIndex }
        if (pendingHumanSeats.isNotEmpty()) return // still waiting on other human seats

        val responses = pendingReactionResponses.toMap()
        pendingReactionResponses = mutableMapOf()
        awaitingHumanReaction = false
        applyReactions(responses)
    }

    // MARK: - Hand / match lifecycle

    private fun finishHand() {
        val g = engine ?: return
        val result = g.result ?: return
        applyResultToPoints(result)
        emit(MatchEvent.HandEnd(result))
    }

    /** The engine's own seat.points are the live source of truth during a hand (riichi-stick
     *  deductions already happened there via declareRiichi). Apply win/loss payments onto
     *  that same array, then copy the final values back into `points`. */
    private fun applyResultToPoints(result: HandResult) {
        val g = engine ?: return
        val seatPoints = g.seats.map { it.points }.toMutableList()

        when (result.kind) {
            "tsumo" -> {
                val winnerSeat = result.tsumoSeat!!
                val win = result.tsumoWin!!
                val isDealerWin = winnerSeat == g.dealerSeat
                for (i in 0 until 4) {
                    if (i == winnerSeat) continue
                    val pay = if (isDealerWin) {
                        win.payments.fromEachNonDealer ?: 0
                    } else {
                        if (i == g.dealerSeat) win.payments.fromDealer ?: 0 else win.payments.fromEachNonDealer ?: 0
                    }
                    seatPoints[i] -= pay
                    seatPoints[winnerSeat] += pay
                }
                seatPoints[winnerSeat] += riichiSticksOnTable * 1000
                riichiSticksOnTable = 0
            }
            "ron" -> {
                for (w in result.ronWinners) {
                    seatPoints[result.ronDiscarder!!] -= w.win.total
                    seatPoints[w.seat] += w.win.total
                }
                result.ronWinners.firstOrNull()?.let {
                    seatPoints[it.seat] += riichiSticksOnTable * 1000
                    riichiSticksOnTable = 0
                }
            }
            "exhaustive" -> {
                for (i in result.exhaustivePayments.indices) seatPoints[i] += result.exhaustivePayments[i]
                // Riichi sticks placed this hand (if any) carry over to the next hand's pot.
            }
        }

        // Count any riichi declared this hand toward the carrying pot (sticks already left
        // each declarer's own points via declareRiichi).
        for (s in g.seats) if (s.riichi) riichiSticksOnTable += 1

        points = seatPoints
    }

    fun advanceToNextHand() {
        val g = engine ?: return
        val result = g.result ?: return
        val dealerWon = when (result.kind) {
            "tsumo" -> result.tsumoSeat == g.dealerSeat
            "ron" -> result.ronWinners.any { it.seat == g.dealerSeat }
            else -> result.dealerTenpai
        }

        if (!dealerWon) {
            dealerSeat = (dealerSeat + 1) % 4
            handNumber += 1
        }

        if (handNumber > 4) {
            matchOver = true
            emit(MatchEvent.MatchEnd)
            return
        }
        startHand()
    }

    /** Invalidates any pending scheduled action so the match stops auto-advancing. Call when
     *  a match ends normally or the screen is left early. */
    fun stop() {
        gen++
    }
}
