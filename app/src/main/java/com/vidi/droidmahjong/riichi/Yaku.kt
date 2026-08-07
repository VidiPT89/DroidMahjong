package com.vidi.droidmahjong.riichi

data class YakuEntry(val name: String, val han: Int, val isYakuman: Boolean = false)
data class YakuResult(
    val yakus: List<YakuEntry>,
    val han: Int,
    val fu: Int,
    val total: Int,
    val yakuList: List<Map<String, Any>>,
    val payments: ScorePayments = ScorePayments()
)

val GREEN_TILES = setOf("s2", "s3", "s4", "s6", "s8", "dG")

enum class WinType { TSUMO, RON }

data class WinContext(
    val hand: List<String>,
    val melds: List<Meld>,
    val drawnTile: String?,
    val winType: WinType,
    val doraIndicators: List<String>,
    val uraDora: List<String>,
    val isDealer: Boolean,
    val seatWind: String = if (isDealer) "wE" else "wS",
    val roundWind: String = "wE",
    val riichi: Boolean = false,
    val doubleRiichi: Boolean = false,
    val ippatsu: Boolean = false
)

fun isYakuhaiTile(tile: String): Boolean = tile.startsWith("d") || tile == "wE" || tile == "wS" || tile == "wW" || tile == "wN"

private fun entriesToResult(entries: List<YakuEntry>, fu: Int, isDealer: Boolean, winType: WinType): YakuResult {
    val han = if (entries.any { it.isYakuman }) entries.filter { it.isYakuman }.sumOf { it.han }
    else entries.sumOf { it.han }
    val total = baseScoreFromHanFu(han, fu, isDealer, winType)
    val list = entries.map { mapOf("name" to it.name, "han" to it.han) }
    return YakuResult(entries, han, fu, total, list)
}

/** Kokushi Musou — 13 orphans, always a yakuman regardless of wait shape. */
fun evaluateKokushi(ctx: WinContext): YakuResult =
    entriesToResult(listOf(YakuEntry("Kokushi Musou", 13, isYakuman = true)), 20, ctx.isDealer, ctx.winType)

/** Chiitoitsu — seven pairs, always closed, fixed 25 fu. */
fun evaluateChiitoitsu(ctx: WinContext): YakuResult {
    val entries = mutableListOf(YakuEntry("Chiitoitsu", 2))
    entries += suitPurityYaku(ctx.hand, isOpen = false)
    if (ctx.doubleRiichi) entries.add(YakuEntry("Double Riichi", 2))
    else if (ctx.riichi) entries.add(YakuEntry("Riichi", 1))
    if (ctx.riichi && ctx.ippatsu) entries.add(YakuEntry("Ippatsu", 1))
    if (ctx.winType == WinType.TSUMO) entries.add(YakuEntry("Menzen Tsumo", 1))
    return entriesToResult(entries, 25, ctx.isDealer, ctx.winType)
}

private fun suitPurityYaku(tiles: List<String>, isOpen: Boolean): List<YakuEntry> {
    val suits = tiles.mapNotNull { SUIT_OF[it] }.toSet()
    val hasHonor = tiles.any { isHonor(it) }
    return when {
        suits.size == 1 && !hasHonor -> listOf(YakuEntry(if (isOpen) "Honitsu" else "Chinitsu", if (isOpen) 3 else 6))
        suits.size == 1 && hasHonor -> listOf(YakuEntry(if (isOpen) "Honitsu" else "Chinitsu", if (isOpen) 2 else 5))
        suits.isEmpty() && hasHonor -> listOf(YakuEntry("Tsuuiisou", 13, isYakuman = true))
        else -> emptyList()
    }
}

fun evaluateDecomposition(decomp: Decomposition, ctx: WinContext): YakuResult? {
    val entries = mutableListOf<YakuEntry>()
    val isOpenHand = ctx.melds.isNotEmpty()
    val isClosed = !isOpenHand
    val groups = decomp.groups
    val allGroupTiles = groups.flatMap { it.tiles } + decomp.head

    if (allGroupTiles.all { isTerminalOrHonor(it) }) {
        if (allGroupTiles.all { isHonor(it) }) {
            entries.add(YakuEntry("Tsuuiisou", 13, isYakuman = true))
        } else if (allGroupTiles.all { isTerminal(it) }) {
            entries.add(YakuEntry("Chinroutou", 13, isYakuman = true))
        } else if (groups.all { it.kind == "triplet" }) {
            entries.add(YakuEntry("Honroutou", 2))
        }
    }

    val allTriplets = groups.isNotEmpty() && groups.all { it.kind == "triplet" }
    if (allTriplets && !entries.any { it.isYakuman }) entries.add(YakuEntry("Toitoi", 2))

    val ankouCount = groups.count { it.kind == "triplet" && it.meld == null }
    if (ankouCount >= 4) entries.add(YakuEntry("Suuankou", 13, isYakuman = true))

    val kanCount = ctx.melds.count { it.kind.endsWith("kan") }
    if (kanCount >= 4) entries.add(YakuEntry("Suukantsu", 13, isYakuman = true))

    val dragonTripletCount = groups.count { it.kind == "triplet" && it.tiles[0].startsWith("d") }
    if (dragonTripletCount == 3) entries.add(YakuEntry("Daisangen", 13, isYakuman = true))

    if (allGroupTiles.toSet() == GREEN_TILES.intersect(allGroupTiles.toSet()) && allGroupTiles.all { it in GREEN_TILES }) {
        entries.add(YakuEntry("Ryuuiisou", 13, isYakuman = true))
    }

    // Context yaku (riichi/ippatsu/tsumo) don't apply once a yakuman is already in the
    // list — a yakuman hand scores on its own, ignoring regular yaku entirely.
    if (entries.none { it.isYakuman }) {
        if (ctx.doubleRiichi) entries.add(YakuEntry("Double Riichi", 2))
        else if (ctx.riichi) entries.add(YakuEntry("Riichi", 1))
        if (ctx.riichi && ctx.ippatsu) entries.add(YakuEntry("Ippatsu", 1))
        if (isClosed && ctx.winType == WinType.TSUMO) entries.add(YakuEntry("Menzen Tsumo", 1))

        if (dragonTripletCount == 2 && decomp.head.startsWith("d")) entries.add(YakuEntry("Shousangen", 2))
        if (isClosed && ankouCount == 3) entries.add(YakuEntry("Sanankou", 2))
    }

    // Yakuhai — one han per triplet/kan of a dragon, the round wind, or the seat wind
    // (double for a wind that is both, e.g. dealer's round-wind triplet).
    for (g in groups) {
        if (g.kind != "triplet") continue
        val t = g.tiles[0]
        if (t.startsWith("d")) entries.add(YakuEntry(dragonName(t), 1))
        if (t == ctx.roundWind) entries.add(YakuEntry("Round Wind", 1))
        if (t == ctx.seatWind) entries.add(YakuEntry("Seat Wind", 1))
    }

    // Chanta / Junchan — every group (and the pair) contains a terminal or honor.
    val everyGroupHasTerminalOrHonor = groups.all { g -> g.tiles.any { isTerminalOrHonor(it) } } && isTerminalOrHonor(decomp.head)
    if (everyGroupHasTerminalOrHonor && !entries.any { it.isYakuman }) {
        val anyHonor = allGroupTiles.any { isHonor(it) }
        val hasSequence = groups.any { it.kind == "sequence" }
        if (hasSequence) {
            entries.add(YakuEntry(if (anyHonor) "Chanta" else "Junchan", if (anyHonor) (if (isOpenHand) 1 else 2) else (if (isOpenHand) 2 else 3)))
        }
    }

    // Ittsuu — 1-9 straight of one suit using three sequences.
    val sequences = groups.filter { it.kind == "sequence" }
    for (suit in listOf("m", "s", "p")) {
        val suitSeqStarts = sequences.filter { SUIT_OF[it.tiles[0]] == suit }.map { rankOf(it.tiles[0]) }
        if (listOf(1, 4, 7).all { it in suitSeqStarts }) {
            entries.add(YakuEntry("Ittsuu", if (isOpenHand) 1 else 2))
        }
    }

    // Sanshoku Doujun — the same sequence shape across all three suits.
    val seqStartRanksBySuit = listOf("m", "s", "p").map { suit -> sequences.filter { SUIT_OF[it.tiles[0]] == suit }.map { rankOf(it.tiles[0]) }.toSet() }
    val commonStarts = seqStartRanksBySuit.reduceOrNull { a, b -> a.intersect(b) } ?: emptySet()
    if (commonStarts.isNotEmpty()) entries.add(YakuEntry("Sanshoku Doujun", if (isOpenHand) 1 else 2))

    // Sanshoku Doukou — the same triplet rank across all three suits.
    val triplets = groups.filter { it.kind == "triplet" }
    val tripletRanksBySuit = listOf("m", "s", "p").map { suit -> triplets.filter { SUIT_OF[it.tiles[0]] == suit }.map { rankOf(it.tiles[0]) }.toSet() }
    val commonTripletRanks = tripletRanksBySuit.reduceOrNull { a, b -> a.intersect(b) } ?: emptySet()
    if (commonTripletRanks.isNotEmpty()) entries.add(YakuEntry("Sanshoku Doukou", 2))

    // Iipeikou — closed hand with two identical sequences.
    if (isClosed) {
        val seqKeys = sequences.map { it.tiles.sorted() }
        val duplicated = seqKeys.groupingBy { it }.eachCount().values.count { it >= 2 }
        if (duplicated > 0) entries.add(YakuEntry(if (duplicated >= 2 && sequences.size >= 4) "Ryanpeikou" else "Iipeikou", if (duplicated >= 2 && sequences.size >= 4) 3 else 1))
    }

    entries += suitPurityYaku(allGroupTiles, isOpen = isOpenHand)

    // Pinfu — closed, all sequences, non-yakuhai pair, two-sided wait.
    val isPinfu = isClosed && groups.all { it.kind == "sequence" } && !isYakuhaiTile(decomp.head)
    if (isPinfu) entries.add(YakuEntry("Pinfu", 1))

    // Tanyao — every tile (including the pair) is a simple, 2 through 8.
    if (allGroupTiles.none { isTerminalOrHonor(it) }) entries.add(YakuEntry("Tanyao", 1))

    if (entries.isEmpty()) return null

    val fu = calculateFu(decomp, ctx, isPinfu)
    return entriesToResult(entries, fu, ctx.isDealer, ctx.winType)
}

private fun dragonName(tile: String): String = when (tile) {
    "dR" -> "Chun (Red Dragon)"
    "dG" -> "Hatsu (Green Dragon)"
    "dW" -> "Haku (White Dragon)"
    else -> "Dragon"
}

fun calculateFu(decomp: Decomposition, ctx: WinContext, isPinfu: Boolean): Int {
    if (isPinfu && ctx.winType == WinType.TSUMO) return 20
    if (isPinfu && ctx.winType == WinType.RON) return 30

    var fu = 20
    for (group in decomp.groups) {
        val isTerminalGroup = group.tiles.any { isTerminalOrHonor(it) }
        fu += when {
            group.kind != "triplet" -> 0
            group.meld?.kind == "ankan" -> if (isTerminalGroup) 32 else 16
            group.meld?.kind == "daiminkan" || group.meld?.kind == "shouminkan" -> if (isTerminalGroup) 16 else 8
            group.meld == null -> if (isTerminalGroup) 8 else 4 // concealed triplet (ankou)
            else -> if (isTerminalGroup) 4 else 2 // open triplet (minkou)
        }
    }

    if (isYakuhaiTile(decomp.head)) fu += 2
    if (ctx.melds.isEmpty() && ctx.winType == WinType.RON) fu += 10 // menzen ron
    if (ctx.winType == WinType.TSUMO) fu += 2

    return if (fu % 10 == 0) fu else ((fu / 10) + 1) * 10
}

fun bestYakuResult(win: WinCheck, ctx: WinContext): YakuResult? = when (win.kind) {
    HandKind.KOKUSHI -> evaluateKokushi(ctx)
    HandKind.CHIITOITSU -> evaluateChiitoitsu(ctx)
    HandKind.REGULAR -> win.decompositions.mapNotNull { evaluateDecomposition(it, ctx) }
        .maxByOrNull { it.han * 1000 + it.fu }
}

fun compareResults(a: YakuResult, b: YakuResult): Int {
    if (a.han != b.han) return b.han - a.han
    return b.fu - a.fu
}
