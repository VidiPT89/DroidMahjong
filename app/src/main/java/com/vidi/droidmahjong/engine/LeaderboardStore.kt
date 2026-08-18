package com.vidi.droidmahjong.engine

import android.content.Context

/**
 * Local best-time / fewest-moves leaderboard, kept per difficulty. No backend — everything is
 * stored on-device via SharedPreferences, following the same storage pattern as [SaveStore].
 */
data class LeaderboardEntry(
    val bestTimeSeconds: Long?,
    val bestMoves: Int?
)

object LeaderboardStore {
    private const val PREFS = "droidmahjong-prefs"
    private fun timeKey(difficulty: Difficulty) = "leaderboard_${difficulty.name}_bestTime"
    private fun movesKey(difficulty: Difficulty) = "leaderboard_${difficulty.name}_bestMoves"

    /**
     * Records the result of a completed (won) game, keeping only the best time and the best
     * move count seen so far for that difficulty — independently, since the fastest win and the
     * most efficient win aren't necessarily the same game.
     *
     * Returns which categories improved, so the UI can show a "new record" badge.
     */
    fun recordResult(context: Context, difficulty: Difficulty, timeSeconds: Long, moves: Int): RecordOutcome {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = get(context, difficulty)

        val isNewBestTime = existing.bestTimeSeconds == null || timeSeconds < existing.bestTimeSeconds
        val isNewBestMoves = existing.bestMoves == null || moves < existing.bestMoves

        val editor = prefs.edit()
        if (isNewBestTime) editor.putLong(timeKey(difficulty), timeSeconds)
        if (isNewBestMoves) editor.putInt(movesKey(difficulty), moves)
        editor.apply()

        return RecordOutcome(isNewBestTime, isNewBestMoves)
    }

    fun get(context: Context, difficulty: Difficulty): LeaderboardEntry {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val time = if (prefs.contains(timeKey(difficulty))) prefs.getLong(timeKey(difficulty), 0L) else null
        val moves = if (prefs.contains(movesKey(difficulty))) prefs.getInt(movesKey(difficulty), 0) else null
        return LeaderboardEntry(bestTimeSeconds = time, bestMoves = moves)
    }

    fun getAll(context: Context): Map<Difficulty, LeaderboardEntry> =
        Difficulty.entries.associateWith { get(context, it) }
}

data class RecordOutcome(val isNewBestTime: Boolean, val isNewBestMoves: Boolean) {
    val isAnyRecord: Boolean get() = isNewBestTime || isNewBestMoves
}
