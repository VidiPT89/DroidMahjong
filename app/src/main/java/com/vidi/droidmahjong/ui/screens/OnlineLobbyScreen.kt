package com.vidi.droidmahjong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidmahjong.engine.OnlineClient
import com.vidi.droidmahjong.engine.boolOrNull
import com.vidi.droidmahjong.engine.objArray
import com.vidi.droidmahjong.engine.strOrNull
import com.vidi.droidmahjong.i18n.Localization
import com.vidi.droidmahjong.ui.theme.Theme

private val windKeysLobby = listOf("windE", "windS", "windW", "windN")

@Composable
fun OnlineLobbyScreen(
    loc: Localization,
    client: OnlineClient,
    onBack: () -> Unit,
    onStarted: () -> Unit
) {
    val context = LocalContext.current
    var serverUrl by remember { mutableStateOf(OnlineClient.storedServerUrl(context)) }
    var roomCodeInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }

    fun errorText(code: String?): String = when (code) {
        "room-not-found" -> loc.t("roomNotFound")
        "room-full-or-started" -> loc.t("roomFull")
        else -> code ?: ""
    }

    androidx.compose.runtime.LaunchedEffect(client.errorMessage) {
        client.errorMessage?.let {
            isConnecting = false
            statusMessage = errorText(it)
        }
    }
    androidx.compose.runtime.LaunchedEffect(client.stateVersion) {
        if (client.stateVersion == 1) onStarted()
    }

    fun createRoom() {
        if (serverUrl.isBlank()) return
        OnlineClient.storeServerUrl(context, serverUrl)
        statusMessage = ""
        isConnecting = true
        client.connect(serverUrl) { ok ->
            isConnecting = false
            if (ok) client.createRoom() else statusMessage = loc.t("connectionError")
        }
    }

    fun joinRoom() {
        if (serverUrl.isBlank() || roomCodeInput.isBlank()) return
        OnlineClient.storeServerUrl(context, serverUrl)
        statusMessage = ""
        isConnecting = true
        client.connect(serverUrl) { ok ->
            isConnecting = false
            if (ok) client.joinRoom(roomCodeInput.uppercase()) else statusMessage = loc.t("connectionError")
        }
    }

    Box(Modifier.fillMaxSize()) {
        BackgroundGlow()

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.background(Theme.bgPanel2, androidx.compose.foundation.shape.CircleShape)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = loc.t("back"), tint = Theme.text)
                }
                Spacer(Modifier.weight(1f))
                LangToggle(loc, onToggle = { loc.toggle() })
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(loc.t("playOnline"), color = Theme.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    loc.t("onlineIntro"),
                    color = Theme.textDim,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                if (client.roomCode == null) {
                    ConnectCard(
                        loc = loc,
                        serverUrl = serverUrl,
                        onServerUrlChange = { serverUrl = it },
                        roomCodeInput = roomCodeInput,
                        onRoomCodeChange = { roomCodeInput = it },
                        isConnecting = isConnecting,
                        onCreate = ::createRoom,
                        onJoin = ::joinRoom
                    )
                } else {
                    RoomCard(loc = loc, client = client)
                }

                if (statusMessage.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(statusMessage, color = Theme.danger, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun ConnectCard(
    loc: Localization,
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    roomCodeInput: String,
    onRoomCodeChange: (String) -> Unit,
    isConnecting: Boolean,
    onCreate: () -> Unit,
    onJoin: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Theme.bgPanel, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(loc.t("serverUrlLabel"), color = Theme.textFaint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = serverUrl,
            onValueChange = onServerUrlChange,
            placeholder = { Text("wss://your-server.example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Theme.text, unfocusedTextColor = Theme.text,
                focusedContainerColor = Theme.bgPanel2, unfocusedContainerColor = Theme.bgPanel2
            )
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(loc.t("createRoom"), onCreate)

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = roomCodeInput,
                onValueChange = onRoomCodeChange,
                placeholder = { Text(loc.t("roomCodePlaceholder")) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Theme.text, unfocusedTextColor = Theme.text,
                    focusedContainerColor = Theme.bgPanel2, unfocusedContainerColor = Theme.bgPanel2
                )
            )
            Spacer(Modifier.width(8.dp))
            SecondaryButton(loc.t("joinRoom"), onJoin, modifier = Modifier.weight(1f))
        }

        if (isConnecting) {
            Spacer(Modifier.height(10.dp))
            Text(loc.t("connecting"), color = Theme.textFaint, fontSize = 13.sp)
        }
    }
}

@Composable
private fun RoomCard(loc: Localization, client: OnlineClient) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Theme.bgPanel, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(loc.t("yourRoomCode"), color = Theme.textFaint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(client.roomCode ?: "", color = Theme.accent, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
        Spacer(Modifier.height(12.dp))

        val seats = client.lobby?.objArray("seats") ?: emptyList()
        for (i in 0 until 4) {
            val seat = seats.getOrNull(i)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .background(Theme.bgPanel2, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(loc.t(windKeysLobby[i]), color = Theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (seat != null) {
                    Text(" ${seat.strOrNull("name") ?: ""}", color = Theme.textDim, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    val connected = seat.boolOrNull("connected") ?: false
                    Text(
                        if (connected) loc.t("playerConnected") else loc.t("playerWaiting"),
                        color = if (connected) Theme.ok else Theme.textFaint,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                    Text(loc.t("bot"), color = Theme.textFaint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            loc.t("onlineSeatFillNote"),
            color = Theme.textFaint, fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (client.seat == 0) {
            Spacer(Modifier.height(12.dp))
            PrimaryButton(loc.t("startOnlineMatch"), { client.startMatch() })
        }
    }
}
