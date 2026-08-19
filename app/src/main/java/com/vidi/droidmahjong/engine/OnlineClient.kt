package com.vidi.droidmahjong.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebSocket client for the traditional 4-player Riichi online mode. Talks to the same
 * authoritative Node server used by the web port (see MahjongWeb/server/index.js's protocol
 * comment) -- the server owns all game rules and just pushes down a fully-resolved "state"
 * after every change, so this client is intentionally a thin, mostly untyped relay rather
 * than a second implementation of the rules: messages stay as [JSONObject] instead of being
 * modeled with typed data classes, mirroring the web client's own dynamic property access,
 * since the `result` field's shape varies by hand outcome (tsumo/ron/exhaustive) in a way a
 * fixed data class would model awkwardly.
 */
class OnlineClient(context: Context) {
    private val prefs = context.getSharedPreferences("droidmahjong-prefs", Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val http = OkHttpClient()
    private var socket: WebSocket? = null

    val playerId: String = prefs.getString("online-player-id", null) ?: run {
        val id = "p-" + UUID.randomUUID().toString()
        prefs.edit().putString("online-player-id", id).apply()
        id
    }

    var isConnected by mutableStateOf(false)
        private set
    var seat by mutableStateOf<Int?>(null)
        private set
    var roomCode by mutableStateOf<String?>(null)
        private set
    var lobby by mutableStateOf<JSONObject?>(null)
        private set
    var state by mutableStateOf<JSONObject?>(null)
        private set

    /** `state` changing identity isn't itself a reliable Compose trigger for "a new message
     *  just arrived" (two consecutive states could be structurally similar), so this ticks
     *  up on every state message purely to give call sites something to key off of. */
    var stateVersion by mutableStateOf(0)
        private set
    var awaitReactionOpts by mutableStateOf<JSONObject?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var connectionLost by mutableStateOf(false)
        private set

    companion object {
        private const val PREFS = "droidmahjong-prefs"
        fun storedServerUrl(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("online-server-url", "") ?: ""
        fun storeServerUrl(context: Context, url: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("online-server-url", url).apply()
        }
    }

    /** Opens the socket and waits for it to actually establish (or fail) before returning,
     *  so callers can show a connecting/error state instead of firing messages at a socket
     *  that might never open. */
    fun connect(url: String, onResult: (Boolean) -> Unit) {
        val settled = AtomicBoolean(false)
        val request = try {
            Request.Builder().url(url).build()
        } catch (e: IllegalArgumentException) {
            onResult(false)
            return
        }
        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                mainHandler.post {
                    isConnected = true
                    connectionLost = false
                }
                if (settled.compareAndSet(false, true)) mainHandler.post { onResult(true) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                mainHandler.post { handle(text) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                mainHandler.post {
                    isConnected = false
                    connectionLost = true
                }
                if (settled.compareAndSet(false, true)) mainHandler.post { onResult(false) }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                mainHandler.post { isConnected = false }
            }
        })
    }

    fun disconnect() {
        socket?.close(1000, null)
        socket = null
        isConnected = false
    }

    private fun send(payload: JSONObject) {
        socket?.send(payload.toString())
    }

    fun createRoom() = send(JSONObject().put("type", "create").put("playerId", playerId))
    fun joinRoom(code: String) = send(JSONObject().put("type", "join").put("code", code).put("playerId", playerId))
    fun startMatch() = send(JSONObject().put("type", "start"))
    fun discard(tile: String) = send(JSONObject().put("type", "discard").put("tile", tile))
    fun reactPass() = send(JSONObject().put("type", "react").put("action", "pass"))
    fun reactRon() = send(JSONObject().put("type", "react").put("action", "ron"))
    fun reactPon() = send(JSONObject().put("type", "react").put("action", "pon"))
    fun reactKan() = send(JSONObject().put("type", "react").put("action", "kan"))
    fun reactChi(pair: List<String>) {
        val action = JSONObject().put("chi", JSONArray(pair))
        send(JSONObject().put("type", "react").put("action", action))
    }
    fun declareRiichi() = send(JSONObject().put("type", "riichi"))
    fun declareTsumo() = send(JSONObject().put("type", "tsumo"))
    fun ankan(tileType: String) = send(JSONObject().put("type", "ankan").put("tileType", tileType))
    fun nextHand() = send(JSONObject().put("type", "nextHand"))

    private fun handle(text: String) {
        val msg = try { JSONObject(text) } catch (e: Exception) { return }
        when (msg.optString("type")) {
            "joined" -> {
                seat = if (msg.has("seat")) msg.getInt("seat") else null
                roomCode = msg.optString("code")
            }
            "error" -> errorMessage = msg.optString("message")
            "lobby" -> lobby = msg
            "state" -> {
                state = msg
                stateVersion += 1
                awaitReactionOpts = null
            }
            "awaitReaction" -> {
                if (msg.has("seat") && msg.getInt("seat") == seat) awaitReactionOpts = msg.optJSONObject("opts")
            }
        }
    }
}

// MARK: - Small helpers for reading the loosely-typed state/lobby JSON

fun JSONObject.strOrNull(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
fun JSONObject.intOrNull(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null
fun JSONObject.boolOrNull(key: String): Boolean? = if (has(key) && !isNull(key)) getBoolean(key) else null
fun JSONObject.objOrNull(key: String): JSONObject? = optJSONObject(key)
fun JSONObject.objArray(key: String): List<JSONObject> {
    val arr = optJSONArray(key) ?: return emptyList()
    return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
}
fun JSONObject.strArray(key: String): List<String> {
    val arr = optJSONArray(key) ?: return emptyList()
    return (0 until arr.length()).mapNotNull { arr.optString(it, null) }
}
fun JSONObject.intArray(key: String): List<Int> {
    val arr = optJSONArray(key) ?: return emptyList()
    return (0 until arr.length()).map { arr.optInt(it) }
}
