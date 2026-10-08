package com.relun.app.data.socket

import android.util.Log
import com.relun.app.data.model.MessageDto
import com.relun.app.data.network.relunJson
import com.relun.app.data.session.SessionStore
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.URI

sealed interface SocketEvent {
    data class NewMessage(val message: MessageDto, val clientId: String?) : SocketEvent
    /** Sent to the recipient's own room, whether or not the chat is open. */
    data class MessageNotification(val message: MessageDto) : SocketEvent
    data class Typing(val userId: String, val isTyping: Boolean) : SocketEvent
    data class Read(val conversationId: String, val readBy: String) : SocketEvent
    data class Presence(val userId: String, val isOnline: Boolean) : SocketEvent
    data class Error(val message: String, val code: String?, val clientId: String?) : SocketEvent
}

/**
 * The one Socket.IO connection for chat. Connected while signed in; screens
 * join and leave conversation rooms and read [events].
 */
class ChatSocket(private val baseUrl: String, private val session: SessionStore) {

    private var socket: Socket? = null
    private val joined = mutableSetOf<String>()

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    @Synchronized
    fun connect() {
        val token = session.current.accessToken ?: return
        if (socket?.connected() == true) return
        socket?.let { it.off(); it.disconnect() }

        val options = IO.Options.builder()
            .setTransports(arrayOf("websocket"))
            .setReconnection(true)
            .setReconnectionDelay(1_000)
            .setReconnectionDelayMax(10_000)
            .setAuth(mapOf("token" to token))
            .build()

        val created = IO.socket(URI.create(baseUrl.trimEnd('/')), options)
        socket = created

        created.on(Socket.EVENT_CONNECT) {
            _connected.value = true
            // Rooms do not survive a reconnect.
            synchronized(joined) { joined.toList() }.forEach { created.emit("join_conversation", it) }
        }
        created.on(Socket.EVENT_DISCONNECT) { _connected.value = false }
        created.on(Socket.EVENT_CONNECT_ERROR) {
            _connected.value = false
            // The access token rotates on refresh, and a socket keeps the token it
            // was built with. Rebuild it with the current one.
            val fresh = session.current.accessToken
            if (fresh != null && fresh != token) {
                synchronized(this) {
                    if (socket === created) {
                        created.off()
                        created.disconnect()
                        socket = null
                    }
                }
                connect()
            }
        }

        created.on("new_message") { args -> parseMessageEvent(args)?.let { (msg, clientId) -> emit(SocketEvent.NewMessage(msg, clientId)) } }
        created.on("message_notification") { args -> parseMessageEvent(args)?.let { (msg, _) -> emit(SocketEvent.MessageNotification(msg)) } }
        created.on("user_typing") { args ->
            (args.firstOrNull() as? JSONObject)?.let {
                emit(SocketEvent.Typing(it.optString("userId"), it.optBoolean("isTyping")))
            }
        }
        created.on("messages_read") { args ->
            (args.firstOrNull() as? JSONObject)?.let {
                emit(SocketEvent.Read(it.optString("conversationId"), it.optString("readBy")))
            }
        }
        created.on("presence_changed") { args ->
            (args.firstOrNull() as? JSONObject)?.let {
                emit(SocketEvent.Presence(it.optString("userId"), it.optBoolean("isOnline")))
            }
        }
        created.on("error") { args ->
            val json = args.firstOrNull() as? JSONObject
            emit(
                SocketEvent.Error(
                    message = json?.optString("message").orEmpty().ifBlank { "Something went wrong" },
                    code = json?.optString("code")?.takeIf { it.isNotBlank() },
                    clientId = json?.optString("clientId")?.takeIf { it.isNotBlank() },
                )
            )
        }

        created.connect()
    }

    /** Signs out: drops the connection and forgets every room. */
    @Synchronized
    fun disconnect() {
        synchronized(joined) { joined.clear() }
        pause()
    }

    /**
     * Backgrounded: drops the connection but remembers open rooms for the next
     * [connect]. The server only sends pushes while no socket is connected.
     */
    @Synchronized
    fun pause() {
        socket?.let { it.off(); it.disconnect() }
        socket = null
        _connected.value = false
    }

    fun join(userId: String) {
        synchronized(joined) { joined += userId }
        socket?.takeIf { it.connected() }?.emit("join_conversation", userId)
    }

    fun leave(userId: String) {
        synchronized(joined) { joined -= userId }
        socket?.emit("leave_conversation", userId)
    }

    /** Returns false when there is no live connection, so the caller can mark the message failed. */
    fun send(recipientId: String, content: String, clientId: String): Boolean {
        val live = socket?.takeIf { it.connected() } ?: return false
        live.emit(
            "send_message",
            JSONObject().put("recipientId", recipientId).put("content", content).put("clientId", clientId),
        )
        return true
    }

    fun typing(recipientId: String, isTyping: Boolean) {
        socket?.emit("typing", JSONObject().put("recipientId", recipientId).put("isTyping", isTyping))
    }

    fun markRead(recipientId: String) {
        socket?.emit("mark_read", JSONObject().put("recipientId", recipientId))
    }

    private fun emit(event: SocketEvent) {
        _events.tryEmit(event)
    }

    private fun parseMessageEvent(args: Array<Any?>): Pair<MessageDto, String?>? {
        val json = args.firstOrNull() as? JSONObject ?: return null
        val message = json.optJSONObject("message") ?: return null
        return runCatching {
            relunJson.decodeFromString(MessageDto.serializer(), message.toString()) to
                json.optString("clientId").takeIf { it.isNotBlank() }
        }.onFailure { Log.w("ChatSocket", "Bad message payload", it) }.getOrNull()
    }
}
