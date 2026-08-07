package com.vidi.droidmahjong.riichi

data class ScorePayments(
    val fromEachNonDealer: Int? = null,
    val fromDealer: Int? = null
)

data class ScoreResult(
    val han: Int,
    val fu: Int,
    val payments: ScorePayments,
    val total: Int
)

fun baseScoreFromHanFu(han: Int, fu: Int, isDealer: Boolean, winType: WinType): Int {
    if (han >= 13) return if (isDealer) 12000 else 8000
    if (han >= 11) return if (isDealer) 9000 else 6000
    if (han >= 8) return if (isDealer) 6000 else 4000
    if (han >= 6) return if (isDealer) 4000 else 3000
    if (han >= 5) return if (isDealer) 4000 else 3000

    val base = when {
        fu >= 50 && han >= 3 -> 2000
        fu >= 40 && han >= 4 -> 2000
        fu >= 30 && han >= 5 -> 2000
        else -> (fu * (1 shl (han + 2))) / 100
    }

    return minOf(base * 100, 2000)
}

fun roundUpTo100(n: Int): Int = ((n + 99) / 100) * 100

fun scorePoints(win: ScoreResult, isDealer: Boolean, winType: WinType): ScoreResult {
    val base = baseScoreFromHanFu(win.han, win.fu, isDealer, winType)

    if (winType == WinType.TSUMO) {
        if (isDealer) {
            val each = roundUpTo100(base * 2)
            return win.copy(
                payments = ScorePayments(fromEachNonDealer = each),
                total = each * 3
            )
        } else {
            val fromNonDealer = roundUpTo100(base)
            val fromDealer = roundUpTo100(base * 2)
            return win.copy(
                payments = ScorePayments(fromEachNonDealer = fromNonDealer, fromDealer = fromDealer),
                total = fromNonDealer * 2 + fromDealer
            )
        }
    }

    val total = if (isDealer) roundUpTo100(base * 6) else roundUpTo100(base * 4)
    return win.copy(
        payments = ScorePayments(fromEachNonDealer = total),
        total = total
    )
}
