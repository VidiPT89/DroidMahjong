package com.vidi.droidmahjong.engine

import android.content.Context
import com.vidi.droidmahjong.data.GameTile
import org.json.JSONArray
import org.json.JSONObject

object SaveStore {
    private const val PREFS = "droidmahjong-prefs"
    private const val KEY_SAVE = "save"

    fun save(context: Context, engine: GameEngine) {
        val snapshot = engine.makeSnapshot()
        val json = JSONObject()

        val tilesArray = JSONArray()
        snapshot.tiles.forEach { t ->
            val o = JSONObject()
            o.put("id", t.id); o.put("x", t.x); o.put("y", t.y); o.put("z", t.z)
            o.put("removed", t.removed); o.put("typeId", t.typeId ?: JSONObject.NULL)
            tilesArray.put(o)
        }
        json.put("tiles", tilesArray)
        json.put("selectedId", snapshot.selectedId ?: JSONObject.NULL)

        val historyArray = JSONArray()
        snapshot.historyPairs.forEach { pair ->
            val a = JSONArray(); a.put(pair[0]); a.put(pair[1])
            historyArray.put(a)
        }
        json.put("history", historyArray)
        json.put("moves", snapshot.moves)
        json.put("hintsUsed", snapshot.hintsUsed)
        json.put("elapsedSeconds", snapshot.elapsedSeconds)
        json.put("difficulty", snapshot.difficulty)
        json.put("level", snapshot.level)

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_SAVE, json.toString())
            .apply()
    }

    fun load(context: Context): GameSnapshot? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SAVE, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val tilesArray = json.getJSONArray("tiles")
            val tiles = (0 until tilesArray.length()).map { i ->
                val o = tilesArray.getJSONObject(i)
                GameTile(
                    id = o.getInt("id"), x = o.getInt("x"), y = o.getInt("y"), z = o.getInt("z"),
                    removed = o.getBoolean("removed"),
                    typeId = if (o.isNull("typeId")) null else o.getString("typeId")
                )
            }
            val historyArray = json.getJSONArray("history")
            val history = (0 until historyArray.length()).map { i ->
                val a = historyArray.getJSONArray(i)
                listOf(a.getInt(0), a.getInt(1))
            }
            GameSnapshot(
                tiles = tiles,
                selectedId = if (json.isNull("selectedId")) null else json.getInt("selectedId"),
                historyPairs = history,
                moves = json.getInt("moves"),
                hintsUsed = json.getInt("hintsUsed"),
                elapsedSeconds = json.getLong("elapsedSeconds"),
                difficulty = json.optString("difficulty", Difficulty.MEDIUM.name),
                level = json.optInt("level", 1)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun hasSave(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY_SAVE)

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_SAVE).apply()
    }
}
