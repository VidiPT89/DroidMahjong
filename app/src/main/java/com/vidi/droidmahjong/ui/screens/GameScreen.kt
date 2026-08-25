package com.vidi.droidmahjong.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vidi.droidmahjong.data.GameTile
import com.vidi.droidmahjong.engine.Difficulty
import com.vidi.droidmahjong.engine.GameEngine
import com.vidi.droidmahjong.engine.LEVELS_MAX_LEVEL
import com.vidi.droidmahjong.engine.LeaderboardStore
import com.vidi.droidmahjong.engine.RecordOutcome
import com.vidi.droidmahjong.engine.SaveStore
import com.vidi.droidmahjong.engine.SelectResult
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.ui.sound.SoundFx
import com.vidi.droidmahjong.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ModalKind { NONE, WIN, STUCK, CONFIRM_RESTART }

/** Deals tiles from the center of the board outward, so the pyramid visibly builds up from
 *  the middle rather than from a random order. */
private fun assignDealOrder(engine: GameEngine, dealOrder: MutableMap<Int, Long>) {
    val extents = BoardExtents(engine.tiles)
    val centerX = (extents.minX + extents.maxX) / 2.0
    val centerY = (extents.minY + extents.maxY) / 2.0

    val sorted = engine.tiles.sortedBy { tile ->
        val dx = tile.x - centerX
        val dy = tile.y - centerY
        dx * dx + dy * dy
    }
    sorted.forEachIndexed { i, tile -> dealOrder[tile.id] = (i * 6).toLong() }
}

@Composable
fun GameScreen(engine: GameEngine, loc: Localization, onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScopeCompat()

    var modal by remember { mutableStateOf(ModalKind.NONE) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var hintedIds by remember { mutableStateOf(setOf<Int>()) }
    val shakeTokens = remember { mutableStateMapOf<Int, Int>() }
    val dealOrder = remember { mutableStateMapOf<Int, Long>() }
    var elapsedTick by remember { mutableIntStateOf(0) }
    var recordOutcome by remember { mutableStateOf<RecordOutcome?>(null) }

    // See ui/sound/SoundFx.kt — placeholder ToneGenerator-based feedback, no custom audio
    // assets exist in this repo yet.
    val soundFx = remember { SoundFx() }
    DisposableEffect(Unit) {
        onDispose { soundFx.release() }
    }

    LaunchedEffect(Unit) {
        assignDealOrder(engine, dealOrder)
        while (true) {
            delay(1000)
            elapsedTick++
        }
    }

    fun showToast(message: String) {
        toastMessage = message
        scope.launch {
            delay(2000)
            toastMessage = null
        }
    }

    fun triggerShake(id: Int) {
        shakeTokens[id] = (shakeTokens[id] ?: 0) + 1
    }

    /**
     * Levels mode shows the win modal only once the whole ladder (LEVELS_MAX_LEVEL) is
     * cleared — before that, clearing a level just chains straight into the next (bigger)
     * one, so a run keeps going instead of stopping at every step. Progress (the highest
     * level reached) is saved after every level, not just when the player eventually quits,
     * so it survives the process being killed mid-run.
     */
    fun onInfiniteLevelCleared() {
        val clearedLevel = engine.level
        val isNewRecord = LeaderboardStore.recordInfiniteLevel(context, clearedLevel)
        soundFx.win()

        if (clearedLevel >= LEVELS_MAX_LEVEL) {
            SaveStore.clear(context)
            scope.launch { delay(300); modal = ModalKind.WIN }
            return
        }

        showToast(
            (if (isNewRecord) loc.t("newRecordLevel") else loc.t("levelCleared")).replace("{level}", "$clearedLevel")
        )
        scope.launch {
            delay(900)
            engine.dealNextInfiniteLevel()
            dealOrder.clear()
            assignDealOrder(engine, dealOrder)
            SaveStore.save(context, engine)
        }
    }

    fun handleTap(tile: GameTile) {
        when (val result = engine.select(tile.id)) {
            is SelectResult.Selected -> soundFx.tilePick()
            is SelectResult.Matched -> {
                soundFx.tileMatch()
                SaveStore.save(context, engine)
                if (result.won) {
                    if (engine.difficulty == Difficulty.INFINITE) {
                        onInfiniteLevelCleared()
                    } else {
                        soundFx.win()
                        SaveStore.clear(context)
                        recordOutcome = LeaderboardStore.recordResult(
                            context, engine.difficulty, engine.elapsedSeconds(), engine.moves
                        )
                        scope.launch { delay(300); modal = ModalKind.WIN }
                    }
                } else if (engine.isStuck()) {
                    scope.launch { delay(200); modal = ModalKind.STUCK }
                }
            }
            is SelectResult.Mismatch -> { soundFx.tileMismatch(); triggerShake(result.previous.id) }
            is SelectResult.Blocked -> { soundFx.tileMismatch(); triggerShake(result.tile.id) }
            else -> {}
        }
    }

    fun performHint() {
        val pair = engine.findHint() ?: return
        engine.useHint()
        hintedIds = setOf(pair.first.id, pair.second.id)
        scope.launch {
            delay(1300)
            hintedIds = emptySet()
        }
    }

    fun performShuffle() {
        if (!engine.reshuffle()) {
            showToast(loc.t("shuffleImpossible"))
        } else {
            SaveStore.save(context, engine)
        }
    }

    fun performUndo() {
        if (engine.undo() == null) {
            showToast(loc.t("noMoreUndo"))
        } else {
            SaveStore.save(context, engine)
        }
    }

    fun restartGame() {
        engine.reset()
        SaveStore.clear(context)
        dealOrder.clear()
        assignDealOrder(engine, dealOrder)
        recordOutcome = null
        modal = ModalKind.NONE
    }

    val currentScore = run {
        val pairsCleared = (engine.tiles.size - engine.remaining()) / 2
        (pairsCleared * 100 - engine.hintsUsed * 30).coerceAtLeast(0)
    }

    fun formattedTime(seconds: Long): String {
        val total = seconds.coerceAtLeast(0)
        return "%02d:%02d".format(total / 60, total % 60)
    }

    Box(Modifier.fillMaxSize().background(Theme.bg)) {
        Column(Modifier.fillMaxSize()) {
            GameHeader(
                engine = engine,
                loc = loc,
                elapsedTick = elapsedTick,
                currentScore = currentScore,
                onExit = { SaveStore.save(context, engine); onExit() },
                onHint = ::performHint,
                onShuffle = ::performShuffle,
                onUndo = ::performUndo,
                onRestart = { modal = ModalKind.CONFIRM_RESTART },
                formattedTime = ::formattedTime
            )
            BoardArea(engine, hintedIds, shakeTokens, dealOrder, ::handleTap)
        }

        toastMessage?.let { message ->
            Box(Modifier.fillMaxSize().padding(bottom = 30.dp), contentAlignment = Alignment.BottomCenter) {
                ToastView(message)
            }
        }

        when (modal) {
            ModalKind.WIN -> WinModal(
                loc = loc,
                time = formattedTime(engine.elapsedSeconds()),
                moves = engine.moves,
                score = currentScore,
                leaderboard = LeaderboardStore.get(context, engine.difficulty),
                recordOutcome = recordOutcome,
                formattedBestTime = { formattedTime(it) },
                subtitle = if (engine.difficulty == Difficulty.INFINITE) loc.t("allLevelsComplete") else null,
                onPlayAgain = { restartGame() },
                onMenu = { SaveStore.clear(context); onExit() }
            )
            ModalKind.STUCK -> StuckModal(
                loc = loc,
                onShuffle = { performShuffle(); modal = ModalKind.NONE },
                onUndo = { performUndo(); modal = ModalKind.NONE },
                onMenu = { SaveStore.clear(context); onExit() }
            )
            ModalKind.CONFIRM_RESTART -> ConfirmModal(
                message = loc.t("restartConfirm"),
                loc = loc,
                onYes = { restartGame() },
                onCancel = { modal = ModalKind.NONE }
            )
            ModalKind.NONE -> {}
        }
    }
}

@Composable
private fun GameHeader(
    engine: GameEngine,
    loc: Localization,
    elapsedTick: Int,
    currentScore: Int,
    onExit: () -> Unit,
    onHint: () -> Unit,
    onShuffle: () -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    formattedTime: (Long) -> String
) {
    Column(Modifier.fillMaxWidth().background(Theme.bgPanel.copy(alpha = 0.6f))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit, modifier = Modifier.size(30.dp).background(Theme.bgPanel2, CircleShape)) {
                Icon(Icons.Default.ArrowBack, contentDescription = loc.t("back"), tint = Theme.text, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.weight(1f))
            HeaderActionIcon(Icons.Default.Lightbulb, loc.t("hint"), null, onHint)
            Spacer(Modifier.width(6.dp))
            HeaderActionIcon(Icons.Default.Shuffle, loc.t("shuffle"), null, onShuffle)
            Spacer(Modifier.width(6.dp))
            HeaderActionIcon(Icons.Default.Undo, loc.t("undo"), null, onUndo)
            Spacer(Modifier.width(6.dp))
            HeaderActionIcon(Icons.Default.Refresh, loc.t("restart"), null, onRestart)
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (engine.difficulty == Difficulty.INFINITE) {
                StatGroup(loc.t("level"), "${engine.level}", Modifier.weight(1f))
                StatDivider()
            }
            val timeText = remember(elapsedTick) { formattedTime(engine.elapsedSeconds()) }
            StatGroup(loc.t("time"), timeText, Modifier.weight(1f))
            StatDivider()
            StatGroup(loc.t("moves"), "${engine.moves}", Modifier.weight(1f))
            StatDivider()
            StatGroup(loc.t("score"), "$currentScore", Modifier.weight(1f))
            StatDivider()
            StatGroup(loc.t("left"), "${engine.remaining()}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatGroup(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), color = Theme.textFaint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(4.dp))
        Text(value, color = Theme.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.width(1.dp).height(14.dp).background(Theme.borderStrong))
}

@Composable
private fun HeaderActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tip: String, badge: Int?, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick, modifier = Modifier.size(30.dp).background(Theme.bgPanel2, CircleShape)) {
            Icon(icon, contentDescription = tip, tint = Theme.text, modifier = Modifier.size(14.dp))
        }
        if (badge != null && badge >= 0) {
            Box(
                Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
                    .background(Theme.accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$badge", color = Theme.bg, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BoardArea(
    engine: GameEngine,
    hintedIds: Set<Int>,
    shakeTokens: Map<Int, Int>,
    dealOrder: Map<Int, Long>,
    onTap: (GameTile) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Keyed on level too, not just difficulty: Infinite mode keeps the same difficulty
        // across an entire run while the board itself grows every level, so extents would
        // otherwise stay stale (sized for level 1) after dealNextInfiniteLevel().
        val extents = remember(engine.difficulty, engine.level) { BoardExtents(engine.tiles) }
        // Computed once per recomposition, not once per tile -- see freeTileIds()'s doc
        // comment for why calling engine.isFree(tile) inside the forEach below was a real
        // source of lag: it turned every recomposition into an O(n²) scan.
        val freeIds = engine.freeTileIds()
        val containerWidthDp = maxWidth.value
        val containerHeightDp = maxHeight.value
        val scale = minOf(
            containerWidthDp / extents.width,
            containerHeightDp / extents.height,
            1.5f
        ).coerceAtLeast(0f)

        val offsetX = (containerWidthDp - extents.width * scale) / 2f
        val offsetY = (containerHeightDp - extents.height * scale) / 2f

        // Tiles keep their natural (unscaled) layout size and are visually scaled via
        // graphicsLayer, positioned so their own center lands on the scaled target point —
        // the same "scale + re-center" trick used for the SwiftUI port.
        engine.tiles.filter { !it.removed }.forEach { tile ->
            val point = BoardGeometry.point(tile, extents)
            val centerX = offsetX + point.x * scale
            val centerY = offsetY + point.y * scale
            val left = centerX - BoardGeometry.TILE_W / 2
            val top = centerY - BoardGeometry.TILE_H / 2

            Box(
                Modifier
                    .offset(x = left.dp, y = top.dp)
                    .size(BoardGeometry.TILE_W.dp, BoardGeometry.TILE_H.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .zIndex(BoardGeometry.zIndex(tile))
            ) {
                TileView(
                    tile = tile,
                    isFree = freeIds.contains(tile.id),
                    isSelected = engine.selectedId == tile.id,
                    isHinted = hintedIds.contains(tile.id),
                    shakeToken = shakeTokens[tile.id] ?: 0,
                    dealDelayMs = dealOrder[tile.id] ?: 0L,
                    onTap = { onTap(tile) }
                )
            }
        }
    }
}

@Composable
private fun ToastView(message: String) {
    Box(
        Modifier
            .background(Theme.bgPanel2, androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(message, color = Theme.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
