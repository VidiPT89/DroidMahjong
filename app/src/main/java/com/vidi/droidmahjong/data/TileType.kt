package com.vidi.droidmahjong.data

enum class TileCategory { SUIT, WIND, DRAGON, FLOWER, SEASON }

enum class Suit { CHARACTERS, BAMBOO, CIRCLE }

data class TileType(
    val id: String,
    val category: TileCategory,
    val suit: Suit? = null,
    val rank: Int? = null,
    val count: Int
)

val PIP_ROWS: Map<Int, List<Int>> = mapOf(
    1 to listOf(1), 2 to listOf(1, 1), 3 to listOf(1, 1, 1), 4 to listOf(2, 2), 5 to listOf(2, 1, 2),
    6 to listOf(2, 2, 2), 7 to listOf(2, 2, 2, 1), 8 to listOf(2, 2, 2, 2), 9 to listOf(3, 3, 3)
)

val NUMERALS = listOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九")

/**
 * Builds the flat list of 34 tile "types" used across the game (144 instances total).
 * Standard Mahjong notation (m=man/characters, s=sou/bamboo, p=pin/circle) — using the
 * suit's first letter would collide, since "characters" and "circle" both start with "c".
 */
fun buildTileTypes(): List<TileType> {
    val types = mutableListOf<TileType>()

    val suits = listOf(Suit.CHARACTERS to "m", Suit.BAMBOO to "s", Suit.CIRCLE to "p")
    for ((suit, prefix) in suits) {
        for (rank in 1..9) {
            types.add(TileType(id = "$prefix$rank", category = TileCategory.SUIT, suit = suit, rank = rank, count = 4))
        }
    }

    for (code in listOf("E", "S", "W", "N")) {
        types.add(TileType(id = "w$code", category = TileCategory.WIND, count = 4))
    }

    types.add(TileType(id = "dR", category = TileCategory.DRAGON, count = 4))
    types.add(TileType(id = "dG", category = TileCategory.DRAGON, count = 4))
    types.add(TileType(id = "dW", category = TileCategory.DRAGON, count = 4))

    for (i in 1..4) types.add(TileType(id = "fl$i", category = TileCategory.FLOWER, rank = i, count = 1))
    for (i in 1..4) types.add(TileType(id = "se$i", category = TileCategory.SEASON, rank = i, count = 1))

    return types
}

val TILE_TYPES = buildTileTypes()
val TILE_TYPES_BY_ID = TILE_TYPES.associateBy { it.id }

/** Two tile instances match if they can legally form a pair. */
fun tilesMatch(typeIdA: String, typeIdB: String): Boolean {
    val a = TILE_TYPES_BY_ID[typeIdA] ?: return false
    val b = TILE_TYPES_BY_ID[typeIdB] ?: return false
    if (typeIdA == typeIdB) {
        return a.category != TileCategory.FLOWER && a.category != TileCategory.SEASON
    }
    if (a.category == TileCategory.FLOWER && b.category == TileCategory.FLOWER) return true
    if (a.category == TileCategory.SEASON && b.category == TileCategory.SEASON) return true
    return false
}

/**
 * Builds 54 shuffled pair units (108 tiles) for the Easy layout: 27 of the 34 standard suit/
 * wind/dragon types, each contributing both of its pairs — no flowers or seasons, keeping the
 * flat single-layer board simple to read.
 */
fun buildEasyPairUnits(): List<Pair<String, String>> {
    val standardIds = TILE_TYPES
        .filter { it.category != TileCategory.FLOWER && it.category != TileCategory.SEASON }
        .map { it.id }
        .shuffled()
        .take(27)

    val units = mutableListOf<Pair<String, String>>()
    for (id in standardIds) { units.add(id to id); units.add(id to id) }
    units.shuffle()
    return units.map { if (kotlin.random.Random.nextBoolean()) it.second to it.first else it }
}

/**
 * Builds 72 shuffled "pair units" (each a matching pair of typeIds) covering all 144 tiles.
 * Consumed by the engine, which assigns each unit to a pair of board positions guaranteed to
 * be simultaneously free at some point during a full solve.
 */
fun buildShuffledPairUnits(): List<Pair<String, String>> {
    val units = mutableListOf<Pair<String, String>>()
    for (t in TILE_TYPES) {
        if (t.category == TileCategory.FLOWER || t.category == TileCategory.SEASON) continue
        units.add(t.id to t.id)
        units.add(t.id to t.id)
    }

    val flowerIds = TILE_TYPES.filter { it.category == TileCategory.FLOWER }.map { it.id }.shuffled().toMutableList()
    val seasonIds = TILE_TYPES.filter { it.category == TileCategory.SEASON }.map { it.id }.shuffled().toMutableList()
    units.add(flowerIds[0] to flowerIds[1])
    units.add(flowerIds[2] to flowerIds[3])
    units.add(seasonIds[0] to seasonIds[1])
    units.add(seasonIds[2] to seasonIds[3])

    units.shuffle()
    return units.map { if (kotlin.random.Random.nextBoolean()) it.second to it.first else it }
}

/**
 * Builds matching pair-units from whatever type inventory is passed in (e.g. the types still
 * on the board when reshuffling mid-game).
 */
fun buildPairUnitsFromInventory(typeCounts: Map<String, Int>): List<Pair<String, String>> {
    val units = mutableListOf<Pair<String, String>>()
    val bonus = mutableMapOf(TileCategory.FLOWER to mutableListOf<String>(), TileCategory.SEASON to mutableListOf())

    for ((id, count) in typeCounts) {
        val t = TILE_TYPES_BY_ID[id] ?: continue
        if (t.category == TileCategory.FLOWER || t.category == TileCategory.SEASON) {
            repeat(count) { bonus[t.category]!!.add(id) }
        } else {
            var n = count
            while (n >= 2) { units.add(id to id); n -= 2 }
        }
    }

    for (cat in listOf(TileCategory.FLOWER, TileCategory.SEASON)) {
        val list = bonus[cat]!!
        list.shuffle()
        while (list.size >= 2) {
            val a = list.removeAt(list.size - 1)
            val b = list.removeAt(list.size - 1)
            units.add(a to b)
        }
    }

    units.shuffle()
    return units.map { if (kotlin.random.Random.nextBoolean()) it.second to it.first else it }
}

/**
 * Infinite mode's pool: unlike the fixed difficulties (which deal from one real 144-tile
 * set), the board can grow past 144 tiles, so this just cycles through the 34 non-bonus
 * types as many times as needed to produce exactly `pairsNeeded` pairs — there's no "only 4
 * copies of each tile" constraint to honor here, it's an extended variant, not a real set.
 */
fun buildInfinitePairUnits(totalTiles: Int): List<Pair<String, String>> {
    val pairsNeeded = totalTiles / 2
    val types = TILE_TYPES
        .filter { it.category != TileCategory.FLOWER && it.category != TileCategory.SEASON }
        .map { it.id }
    val units = (0 until pairsNeeded).map { val id = types[it % types.size]; id to id }.toMutableList()
    units.shuffle()
    return units.map { if (kotlin.random.Random.nextBoolean()) it.second to it.first else it }
}
