package com.vidi.droidmahjong.riichi

val STANDARD_TYPE_IDS = listOf(
    "m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9",
    "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9",
    "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9",
    "wE", "wS", "wW", "wN", "dR", "dG", "dW"
)

val SUIT_OF: Map<String, String> = buildMap {
    for (r in 1..9) { put("m$r", "m"); put("s$r", "s"); put("p$r", "p") }
}

val RANK_OF: Map<String, Int> = buildMap {
    for (r in 1..9) { put("m$r", r); put("s$r", r); put("p$r", r) }
}

fun rankOf(typeId: String): Int? = RANK_OF[typeId]

fun isHonor(typeId: String): Boolean = typeId.startsWith("w") || typeId.startsWith("d")
fun isTerminal(typeId: String): Boolean = rankOf(typeId) in listOf(1, 9)
fun isTerminalOrHonor(typeId: String): Boolean = isTerminal(typeId) || isHonor(typeId)

/** A full 136-tile wall (34 standard types x 4 copies each, no flowers/seasons), shuffled. */
fun buildWall(): List<String> {
    val wall = mutableListOf<String>()
    for (typeId in STANDARD_TYPE_IDS) repeat(4) { wall.add(typeId) }
    return wall.shuffled()
}
