package com.vidi.droidmahjong.data

data class BoardPosition(val x: Int, val y: Int, val z: Int)

data class GameTile(
    val id: Int,
    val x: Int,
    val y: Int,
    val z: Int,
    var removed: Boolean = false,
    var typeId: String? = null
)
