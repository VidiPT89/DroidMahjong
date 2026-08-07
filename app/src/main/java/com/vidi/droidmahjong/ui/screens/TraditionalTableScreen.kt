package com.vidi.droidmahjong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.riichi.HandResult
import com.vidi.droidmahjong.riichi.LocalMatch
import com.vidi.droidmahjong.riichi.MatchEvent
import com.vidi.droidmahjong.riichi.ReactionResponse
import com.vidi.droidmahjong.ui.theme.Theme

private val windLabelKeys = mapOf("wE" to "windE", "wS" to "windS", "wW" to "windW", "wN" to "windN")

@Composable
fun TraditionalTableScreen(loc: Localization, match: LocalMatch, onExit: () -> Unit) {
    var handResult by remember { mutableStateOf<HandResult?>(null) }
    var showMatchEnd by remember { mutableStateOf(false) }

    // Observing match.lastEvent (a mutableStateOf) makes this recompose on every engine step.
    val lastEvent = match.lastEvent
    if (lastEvent is MatchEvent.HandEnd) {
        if (handResult != lastEvent.result) handResult = lastEvent.result
    }

    val engine = match.engine ?: return
    val displaySeat = remember(engine.currentSeat, match.isHuman) {
        if (match.isHuman[engine.currentSeat]) engine.currentSeat else match.isHuman.indexOfFirst { it }.coerceAtLeast(0)
    }

    Box(Modifier.fillMaxSize().background(Theme.bg)) {
        Column(Modifier.fillMaxSize()) {
            TableHeader(loc, match, onExit)
            OpponentsRow(loc, match, displaySeat)
            Spacer(Modifier.height(10.dp))
            HumanPanel(loc, match, displaySeat)
        }

        if (match.awaitingHumanReaction && displaySeat in match.pendingHumanSeats) {
            ReactionModal(loc, match, displaySeat)
        }

        handResult?.let { result ->
            HandResultModal(
                loc = loc, match = match, result = result, displaySeat = displaySeat,
                onNext = {
                    handResult = null
                    if (match.matchOver) showMatchEnd = true else match.advanceToNextHand()
                }
            )
        }

        if (showMatchEnd) {
            MatchEndModal(loc, match.points, onDone = { match.stop(); onExit() })
        }
    }
}

@Composable
private fun TableHeader(loc: Localization, match: LocalMatch, onExit: () -> Unit) {
    val engine = match.engine ?: return
    Row(
        Modifier.fillMaxWidth().background(Theme.bgPanel.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { match.stop(); onExit() }, modifier = Modifier.size(30.dp).background(Theme.bgPanel2, CircleShape)) {
            Icon(Icons.Default.ArrowBack, contentDescription = loc.t("back"), tint = Theme.text, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${loc.t("roundLabel")} ${loc.t(windLabelKeys[match.roundWind] ?: "windE")} · ${loc.t("handLabel")} ${match.handNumber}",
                color = Theme.textDim, fontSize = 10.sp, fontWeight = FontWeight.Medium
            )
            Text(
                "${loc.t("doraLabel")}: ${engine.doraIndicators().firstOrNull() ?: ""} · ${loc.t("wallLeft")}: ${engine.liveWall.size}",
                color = Theme.textDim, fontSize = 10.sp, fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            "${loc.t("riichiSticksLabel")}: ${match.riichiSticksOnTable}",
            color = Theme.accent, fontSize = 10.sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun OpponentsRow(loc: Localization, match: LocalMatch, displaySeat: Int) {
    val engine = match.engine ?: return
    val order = listOf(1, 2, 3).map { (displaySeat + it) % 4 }
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (seatIndex in order) {
            OpponentPanel(loc, match, seatIndex, isActive = engine.currentSeat == seatIndex)
        }
    }
}

@Composable
private fun OpponentPanel(loc: Localization, match: LocalMatch, seatIndex: Int, isActive: Boolean) {
    val engine = match.engine ?: return
    val seat = engine.seats[seatIndex]
    Column(
        Modifier
            .fillMaxWidth()
            .background(Theme.bgPanel, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(loc.t(windLabelKeys[seat.wind] ?: "windE"), color = Theme.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (seat.riichi) {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(6.dp).background(Theme.accent, CircleShape))
            }
            Spacer(Modifier.weight(1f))
            Text("${seat.points}", color = Theme.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (seat.discards.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (d in seat.discards.takeLast(8)) MiniTileChip(d.tile)
            }
        }
    }
}

@Composable
private fun HumanPanel(loc: Localization, match: LocalMatch, displaySeat: Int) {
    val engine = match.engine ?: return
    val seat = engine.seats[displaySeat]
    val isMyDiscardTurn = engine.currentSeat == displaySeat &&
        engine.phase == com.vidi.droidmahjong.riichi.EnginePhase.DISCARD && match.isHuman[displaySeat]

    Column(Modifier.fillMaxWidth().background(Theme.bgPanel), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(loc.t(windLabelKeys[seat.wind] ?: "windE"), color = Theme.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("${loc.t("pointsLabel")}: ${seat.points}", color = Theme.textDim, fontSize = 12.sp)
            if (seat.riichi) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.background(Theme.accent, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(loc.t("riichiBtn"), color = Theme.bg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (seat.melds.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (meld in seat.melds) {
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        for (t in meld.tiles) MiniTileChip(t)
                    }
                }
            }
        }

        if (seat.discards.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).height(40.dp).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (d in seat.discards) MiniTileChip(d.tile)
            }
        }

        Text(
            turnHintText(loc, match, displaySeat),
            color = Theme.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (tile in seat.hand) {
                Button(
                    onClick = { if (isMyDiscardTurn) match.humanDiscard(tile) },
                    enabled = isMyDiscardTurn,
                    modifier = Modifier.size(32.dp, 44.dp),
                    shape = RoundedCornerShape(5.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Theme.tileFace, disabledContainerColor = Theme.tileFace,
                        contentColor = Theme.text, disabledContentColor = Theme.text
                    )
                ) {
                    TileFaceView(typeId = tile, modifier = Modifier.fillMaxSize())
                }
            }
        }

        ActionButtons(loc, match, displaySeat)
        Spacer(Modifier.height(10.dp))
    }
}

private fun turnHintText(loc: Localization, match: LocalMatch, displaySeat: Int): String {
    val engine = match.engine ?: return ""
    return if (engine.currentSeat == displaySeat && match.isHuman[displaySeat]) {
        if (engine.phase == com.vidi.droidmahjong.riichi.EnginePhase.DISCARD) loc.t("yourTurnDiscard") else ""
    } else {
        loc.t("waitingOthers")
    }
}

@Composable
private fun ActionButtons(loc: Localization, match: LocalMatch, displaySeat: Int) {
    val engine = match.engine ?: return
    val isMyTurn = engine.currentSeat == displaySeat && match.isHuman[displaySeat]
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isMyTurn && engine.canDeclareTsumo()) {
            ActionButton(loc.t("tsumoBtn")) { match.humanDeclareTsumo() }
        }
        if (isMyTurn && engine.canDeclareRiichi(displaySeat)) {
            ActionButton(loc.t("riichiBtn")) { match.humanDeclareRiichi() }
        }
        if (isMyTurn) {
            val ankanOpts = engine.canAnkan(displaySeat)
            if (ankanOpts.isNotEmpty()) {
                ActionButton(loc.t("kanBtn")) { match.humanAnkan(ankanOpts.first()) }
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Theme.accent, contentColor = Theme.bg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniTileChip(typeId: String) {
    Box(
        Modifier
            .size(22.dp, 30.dp)
            .background(Theme.tileFace, RoundedCornerShape(3.dp))
    ) {
        TileFaceView(typeId = typeId, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ReactionModal(loc: Localization, match: LocalMatch, displaySeat: Int) {
    val engine = match.engine ?: return
    val opts = engine.getReactionSummary()[displaySeat] ?: com.vidi.droidmahjong.riichi.ReactionOptions()
    val tile = engine.lastDiscard?.tile ?: ""

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(32.dp)
                .background(Theme.bgPanel, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.size(44.dp, 60.dp).background(Theme.tileFace, RoundedCornerShape(6.dp))) {
                TileFaceView(typeId = tile, modifier = Modifier.fillMaxSize())
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (opts.ron) PrimaryButton(loc.t("ronBtn"), { match.humanReact(displaySeat, ReactionResponse.Ron) })
                if (opts.kan) SecondaryButton(loc.t("kanBtn"), { match.humanReact(displaySeat, ReactionResponse.Kan) })
                if (opts.pon) SecondaryButton(loc.t("ponBtn"), { match.humanReact(displaySeat, ReactionResponse.Pon) })
                for (pair in opts.chi) {
                    SecondaryButton(
                        "${loc.t("chiBtn")} ${pair[0]}+${pair[1]}",
                        { match.humanReact(displaySeat, ReactionResponse.Chi(pair[0], pair[1])) }
                    )
                }
                GhostButton(loc.t("passBtn"), { match.humanReact(displaySeat, ReactionResponse.Pass) })
            }
        }
    }
}

@Composable
private fun HandResultModal(loc: Localization, match: LocalMatch, result: HandResult, displaySeat: Int, onNext: () -> Unit) {
    val engine = match.engine ?: return
    val title = when (result.kind) {
        "tsumo" -> loc.t("tsumoWinTitle")
        "ron" -> loc.t("ronWinTitle")
        else -> loc.t("exhaustiveDrawTitle")
    }
    val winEntries = when (result.kind) {
        "tsumo" -> listOfNotNull(result.tsumoSeat?.let { it to result.tsumoWin!! })
        "ron" -> result.ronWinners.map { it.seat to it.win }
        else -> emptyList()
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(24.dp)
                .background(Theme.bgPanel, RoundedCornerShape(18.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = Theme.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)

            for ((seat, win) in winEntries) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${loc.t(windLabelKeys[engine.seats[seat].wind] ?: "windE")} — ${loc.t("hanLabel")} ${win.han} / ${loc.t("fuLabel")} ${win.fu}",
                        color = Theme.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        win.yakuList.joinToString(", ") { "${it["name"]} (${it["han"]})" },
                        color = Theme.textDim, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text("${loc.t("totalPoints")}: ${win.total}", color = Theme.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (result.kind == "exhaustive") {
                Text("${loc.t("tenpaiLabel")}: ${result.tenpaiSeats.size}", color = Theme.textDim, fontSize = 13.sp)
            }

            PrimaryButton(loc.t("nextHand"), onNext)
        }
    }
}

@Composable
private fun MatchEndModal(loc: Localization, points: List<Int>, onDone: () -> Unit) {
    val ranked = points.mapIndexed { i, p -> i to p }.sortedByDescending { it.second }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(24.dp)
                .background(Theme.bgPanel, RoundedCornerShape(18.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(loc.t("matchEndTitle"), color = Theme.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(loc.t("finalStandings"), color = Theme.textDim, fontSize = 13.sp)

            for ((seatIndex, pts) in ranked) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(loc.t(listOf("windE", "windS", "windW", "windN")[seatIndex]), color = Theme.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("$pts", color = Theme.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            PrimaryButton(loc.t("backToMenu"), onDone)
        }
    }
}
