package com.vidi.droidmahjong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidmahjong.data.TILE_TYPES_BY_ID
import com.vidi.droidmahjong.engine.OnlineClient
import com.vidi.droidmahjong.engine.boolOrNull
import com.vidi.droidmahjong.engine.intOrNull
import com.vidi.droidmahjong.engine.objArray
import com.vidi.droidmahjong.engine.objOrNull
import com.vidi.droidmahjong.engine.strArray
import com.vidi.droidmahjong.engine.intArray
import com.vidi.droidmahjong.engine.strOrNull
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.ui.theme.Theme
import org.json.JSONObject

private val windLabelKeysOnline = mapOf("wE" to "windE", "wS" to "windS", "wW" to "windW", "wN" to "windN")

/** Renders the traditional 4-player Riichi table for online play. Unlike TraditionalTableScreen
 *  (which reads a local RiichiEngine object graph), this reads the loosely-typed `state` JSON
 *  the server pushes down after every change -- the server is the sole source of truth, so
 *  this screen never runs any game rule itself, only sends the player's intent
 *  (discard/react/riichi/tsumo/ankan/nextHand) and renders whatever comes back. */
@Composable
fun OnlineTableScreen(loc: Localization, client: OnlineClient, onExit: () -> Unit) {
    val state = client.state ?: JSONObject()
    val mySeat = client.seat ?: 0

    Box(Modifier.fillMaxSize().background(Theme.bg)) {
        Column(Modifier.fillMaxSize()) {
            Header(loc, state)
            OpponentsRow(loc, state, mySeat)
            Spacer(Modifier.height(8.dp))
            HumanPanel(loc, client, state, mySeat)
        }

        client.awaitReactionOpts?.let { opts -> ReactionModal(loc, client, state, opts) }

        if (state.strOrNull("phase") == "ended") {
            state.objOrNull("result")?.let { HandResultModal(loc, client, state, it) }
        }

        if (state.boolOrNull("matchOver") == true) {
            MatchEndModal(loc, state, onExit)
        }

        if (client.connectionLost) {
            ConnectionLostOverlay(loc, onExit)
        }
    }
}

@Composable
private fun Header(loc: Localization, state: JSONObject) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "${loc.t("roundLabel")} ${loc.t(windLabelKeysOnline[state.strOrNull("roundWind")] ?: "windE")} · ${loc.t("handLabel")} ${state.intOrNull("handNumber") ?: 1}",
                color = Theme.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
            )
            Text(
                "${loc.t("doraLabel")}: ${state.strArray("dora").joinToString(" ") { TILE_TYPES_BY_ID[it]?.id ?: it }}",
                color = Theme.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
            )
        }
        Text("${loc.t("wallLeft")}: ${state.intOrNull("wallCount") ?: 0}", color = Theme.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        val sticks = state.intOrNull("riichiSticksOnTable") ?: 0
        if (sticks > 0) {
            Spacer(Modifier.width(8.dp))
            Text("${loc.t("riichiSticksLabel")}: $sticks", color = Theme.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OpponentsRow(loc: Localization, state: JSONObject, mySeat: Int) {
    val seats = state.objArray("seats")
    val dealerSeat = state.intOrNull("dealerSeat") ?: 0
    val currentSeat = state.intOrNull("currentSeat") ?: 0

    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (offset in 1..3) {
            val seatIndex = (mySeat + offset) % 4
            if (seatIndex < seats.size) {
                OpponentPanel(
                    loc, seats[seatIndex], isDealer = seatIndex == dealerSeat, isActive = seatIndex == currentSeat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OpponentPanel(loc: Localization, seat: JSONObject, isDealer: Boolean, isActive: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Theme.bgPanel, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Row {
            Text(
                loc.t(windLabelKeysOnline[seat.strOrNull("wind")] ?: "windE") + if (isDealer) " 🀄" else "",
                color = Theme.text, fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text("${seat.intOrNull("points") ?: 0}", color = Theme.textDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Row {
            val isBot = seat.boolOrNull("isBot") ?: true
            val isConnected = seat.boolOrNull("isConnected") ?: false
            val name = if (isBot) loc.t("bot") else if (isConnected) (seat.strOrNull("name") ?: "") else "${seat.strOrNull("name") ?: ""} (${loc.t("playerWaiting")})"
            Text(
                name + if (seat.boolOrNull("riichi") == true) " · ${loc.t("riichiBtn")}" else "",
                color = Theme.textFaint, fontSize = 9.sp, maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            Text("${seat.intOrNull("handCount") ?: 0} 🀫", color = Theme.textFaint, fontSize = 9.sp)
        }
        val melds = seat.objArray("melds")
        if (melds.isNotEmpty()) {
            Row {
                for (m in melds) for (t in m.strArray("tiles")) MiniTileChip(t)
            }
        }
        val discards = seat.objArray("discards")
        if (discards.isNotEmpty()) {
            LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.height(((discards.size / 6 + 1) * 22).dp)) {
                androidx.compose.foundation.lazy.grid.items(discards) { d -> MiniTileChip(d.strOrNull("tile") ?: "") }
            }
        }
    }
}

@Composable
private fun HumanPanel(loc: Localization, client: OnlineClient, state: JSONObject, mySeat: Int) {
    val you = state.objOrNull("you") ?: JSONObject()
    val seats = state.objArray("seats")
    val mySeatInfo = seats.getOrNull(mySeat)
    val isMyDiscardTurn = state.intOrNull("currentSeat") == mySeat && state.strOrNull("phase") == "discard"
    val drawnTile = state.strOrNull("turnDrawnTile")

    Column(Modifier.fillMaxWidth().background(Theme.bgPanel.copy(alpha = 0.6f)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                loc.t(windLabelKeysOnline[mySeatInfo?.strOrNull("wind")] ?: "windE") + if (mySeat == state.intOrNull("dealerSeat")) " 🀄" else "",
                color = Theme.text, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text("${loc.t("pointsLabel")}: ${you.intOrNull("points") ?: 0}", color = Theme.textDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (you.boolOrNull("riichi") == true) {
                Spacer(Modifier.width(6.dp))
                Text(
                    loc.t("riichiBtn"), color = Theme.bg, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Theme.accent, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        val melds = you.objArray("melds")
        if (melds.isNotEmpty()) {
            Row { for (m in melds) for (t in m.strArray("tiles")) MiniTileChip(t) }
        }

        val discards = you.objArray("discards")
        if (discards.isNotEmpty()) {
            LazyVerticalGrid(columns = GridCells.Fixed(9), modifier = Modifier.height(((discards.size / 9 + 1) * 26).dp)) {
                androidx.compose.foundation.lazy.grid.items(discards) { d -> MiniTileChip(d.strOrNull("tile") ?: "") }
            }
        }

        Text(
            if (isMyDiscardTurn) loc.t("yourTurnDiscard") else if (state.strOrNull("phase") == "reaction") loc.t("waitingOthers") else "",
            color = Theme.textFaint, fontSize = 11.sp
        )

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (tile in you.strArray("hand").sorted()) {
                Box(
                    Modifier
                        .size(34.dp, 46.dp)
                        .background(Theme.tileFace, RoundedCornerShape(4.dp))
                        .then(
                            if (tile == drawnTile) Modifier.border(2.dp, Theme.ok, RoundedCornerShape(4.dp))
                            else Modifier.border(1.dp, Theme.tileEdge, RoundedCornerShape(4.dp))
                        )
                        .clickable(enabled = isMyDiscardTurn) { client.discard(tile) }
                ) {
                    TileFaceView(typeId = tile, modifier = Modifier.fillMaxSize())
                }
            }
        }

        if (isMyDiscardTurn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (state.boolOrNull("canTsumo") == true) PrimaryButton(loc.t("tsumoBtn"), { client.declareTsumo() })
                if (state.boolOrNull("canRiichi") == true) SecondaryButton(loc.t("riichiBtn"), { client.declareRiichi() })
                for (type in state.strArray("ankanOptions")) {
                    GhostButton("${loc.t("kanBtn")} ${TILE_TYPES_BY_ID[type]?.id ?: type}", { client.ankan(type) })
                }
            }
        }
    }
}

@Composable
private fun MiniTileChip(typeId: String) {
    Box(Modifier.size(14.dp, 20.dp).background(Theme.tileFace, RoundedCornerShape(3.dp))) {
        TileFaceView(typeId = typeId, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ReactionModal(loc: Localization, client: OnlineClient, state: JSONObject, opts: JSONObject) {
    val lastDiscard = state.objOrNull("lastDiscard")
    val tile = lastDiscard?.strOrNull("tile") ?: ""

    ModalScrim {
        Column(
            Modifier.background(Theme.bgPanel, RoundedCornerShape(14.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(TILE_TYPES_BY_ID[tile]?.id ?: tile, color = Theme.text)
                Box(Modifier.size(34.dp, 46.dp).padding(start = 8.dp)) {
                    TileFaceView(typeId = tile, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.height(12.dp))
            if (opts.boolOrNull("ron") == true) PrimaryButton(loc.t("ronBtn"), { client.reactRon() })
            if (opts.boolOrNull("kan") == true) GhostButton(loc.t("kanBtn"), { client.reactKan() })
            if (opts.boolOrNull("pon") == true) GhostButton(loc.t("ponBtn"), { client.reactPon() })
            val chiArr = opts.optJSONArray("chi")
            if (chiArr != null) {
                for (i in 0 until chiArr.length()) {
                    val pairArr = chiArr.optJSONArray(i) ?: continue
                    val pair = (0 until pairArr.length()).map { pairArr.optString(it) }
                    GhostButton("${loc.t("chiBtn")} ${pair.joinToString("+") { TILE_TYPES_BY_ID[it]?.id ?: it }}", { client.reactChi(pair) })
                }
            }
            GhostButton(loc.t("passBtn"), { client.reactPass() })
        }
    }
}

@Composable
private fun HandResultModal(loc: Localization, client: OnlineClient, state: JSONObject, result: JSONObject) {
    val seats = state.objArray("seats")
    val type = result.strOrNull("type") ?: ""

    ModalScrim {
        Column(
            Modifier.background(Theme.bgPanel, RoundedCornerShape(14.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                when (type) {
                    "exhaustive" -> loc.t("exhaustiveDrawTitle")
                    "tsumo" -> loc.t("tsumoWinTitle")
                    else -> loc.t("ronWinTitle")
                },
                color = Theme.text, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))

            if (type == "exhaustive") {
                val tenpaiSeats = result.intArray("tenpaiSeats")
                for (i in seats.indices) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(loc.t(windLabelKeysOnline[seats[i].strOrNull("wind")] ?: "windE"), color = Theme.textDim, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Text(if (tenpaiSeats.contains(i)) loc.t("tenpaiLabel") else loc.t("notenLabel"), color = Theme.textDim, fontSize = 13.sp)
                    }
                }
            } else {
                val winners = if (type == "tsumo") listOf(result) else result.objArray("winners")
                for (w in winners) {
                    val seatIdx = w.intOrNull("seat") ?: result.intOrNull("seat") ?: 0
                    Text(
                        loc.t(windLabelKeysOnline[seats.getOrNull(seatIdx)?.strOrNull("wind")] ?: "windE"),
                        color = Theme.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    for (y in w.objArray("yakuList")) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(y.strOrNull("name") ?: "", color = Theme.textDim, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            val han = y.intOrNull("han") ?: 0
                            if (han > 0) Text("$han han", color = Theme.textDim, fontSize = 12.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("${loc.t("fuLabel")} ${w.intOrNull("fu") ?: 0} · ${loc.t("hanLabel")} ${w.intOrNull("han") ?: 0}", color = Theme.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("${w.intOrNull("total") ?: 0} ${loc.t("totalPoints")}", color = Theme.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            PrimaryButton(loc.t("nextHand"), { client.nextHand() })
        }
    }
}

@Composable
private fun MatchEndModal(loc: Localization, state: JSONObject, onExit: () -> Unit) {
    val seats = state.objArray("seats")
    val points = state.intArray("points")
    val ranked = points.mapIndexed { i, p -> i to p }.sortedByDescending { it.second }

    ModalScrim {
        Column(
            Modifier.background(Theme.bgPanel, RoundedCornerShape(18.dp)).padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(loc.t("matchEndTitle"), color = Theme.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(loc.t("finalStandings"), color = Theme.textDim, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            for ((seatIdx, pts) in ranked) {
                val seat = seats.getOrNull(seatIdx)
                val name = if (seat?.boolOrNull("isBot") == true) loc.t("bot") else (seat?.strOrNull("name") ?: "")
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(name, color = Theme.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("$pts", color = Theme.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(loc.t("backToMenu"), onExit)
        }
    }
}

@Composable
private fun ConnectionLostOverlay(loc: Localization, onExit: () -> Unit) {
    ModalScrim {
        Column(
            Modifier.background(Theme.bgPanel, RoundedCornerShape(14.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(loc.t("connectionLost"), color = Theme.text)
            Spacer(Modifier.height(10.dp))
            PrimaryButton(loc.t("backToMenu"), onExit)
        }
    }
}

@Composable
private fun ModalScrim(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        content()
    }
}
