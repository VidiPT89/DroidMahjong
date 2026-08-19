package com.vidi.droidmahjong.engine

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the JSONObject helpers OnlineClient.kt adds (strOrNull/intOrNull/etc) --
 * pure JSON parsing logic, no Android framework or network involved, exercised against the
 * exact shape the real server sends (see MahjongWeb/server/room.js's buildStateFor), so a key
 * typo here would be caught the same way it would in the actual client.
 */
class OnlineClientJsonTest {

    private fun sampleState(): JSONObject = JSONObject(
        """
        {
          "type": "state",
          "you": {"seat": 0, "hand": ["m1", "m2", "m3"], "melds": [], "discards": [{"tile": "p5"}], "riichi": false, "points": 25000},
          "seats": [
            {"seat": 0, "wind": "wE", "points": 25000, "handCount": 13, "melds": [], "discards": [], "riichi": false, "isBot": false, "isConnected": true, "name": "Vidi"},
            {"seat": 1, "wind": "wS", "points": 25000, "handCount": 13, "melds": [], "discards": [], "riichi": false, "isBot": true, "isConnected": false, "name": null}
          ],
          "dealerSeat": 0,
          "roundWind": "wE",
          "handNumber": 1,
          "dora": ["s3"],
          "wallCount": 70,
          "phase": "discard",
          "currentSeat": 0,
          "turnDrawnTile": null,
          "matchOver": false,
          "points": [25000, 25000, 25000, 25000],
          "riichiSticksOnTable": 0,
          "result": null,
          "canTsumo": false,
          "canRiichi": true,
          "ankanOptions": []
        }
        """.trimIndent()
    )

    @Test
    fun `strOrNull reads present string fields and returns null for missing ones`() {
        val state = sampleState()
        assertEquals("state", state.strOrNull("type"))
        assertNull(state.strOrNull("nope"))
    }

    @Test
    fun `strOrNull returns null for a JSON null value, not the literal string`() {
        val seats = sampleState().objArray("seats")
        assertNull(seats[1].strOrNull("name"))
    }

    @Test
    fun `intOrNull and boolOrNull read nested primitives correctly`() {
        val state = sampleState()
        assertEquals(70, state.intOrNull("wallCount"))
        assertEquals(true, state.boolOrNull("canRiichi"))
        assertEquals(false, state.boolOrNull("matchOver"))
    }

    @Test
    fun `objArray and strArray decode nested arrays`() {
        val state = sampleState()
        val seats = state.objArray("seats")
        assertEquals(2, seats.size)
        assertEquals("wE", seats[0].strOrNull("wind"))

        val you = state.objOrNull("you")!!
        assertEquals(listOf("m1", "m2", "m3"), you.strArray("hand"))
    }

    @Test
    fun `intArray decodes the points array`() {
        val points = sampleState().intArray("points")
        assertEquals(listOf(25000, 25000, 25000, 25000), points)
    }

    @Test
    fun `missing array fields decode as empty lists, not crash`() {
        val you = sampleState().objOrNull("you")!!
        assertTrue(you.objArray("melds").isEmpty())
    }
}
