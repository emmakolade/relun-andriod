package com.relun.app.data.repository

import com.relun.app.data.model.ChatMessage
import com.relun.app.data.model.Conversation
import com.relun.app.data.model.MessageDto
import com.relun.app.data.model.MessageStatus
import com.relun.app.data.network.ApiService
import com.relun.app.data.network.apiCall
import com.relun.app.data.session.SessionStore
import com.relun.app.data.socket.ChatSocket
import com.relun.app.data.socket.SocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class ChatRepository(
    private val api: ApiService,
    val socket: ChatSocket,
    private val session: SessionStore,
    private val scope: CoroutineScope,
) {
    private val _unreadTotal = MutableStateFlow(0)
    /** Conversations with unread messages; the Messages tab badge. */
    val unreadTotal: StateFlow<Int> = _unreadTotal.asStateFlow()

    /** The chat currently on screen, so incoming messages there don't count as unread. */
    @Volatile var openChatUserId: String? = null

    init {
        scope.launch {
            socket.events.collect { event ->
                if (event is SocketEvent.MessageNotification && event.message.senderId != openChatUserId) {
                    refreshUnread()
                }
            }
        }
    }

    private val myId: String get() = session.current.userId.orEmpty()

    suspend fun conversations(): Result<List<Conversation>> = apiCall { api.conversations() }.map { res ->
        res.conversations.mapNotNull { conv ->
            val other = conv.otherUser ?: return@mapNotNull null
            Conversation(
                userId = other.id,
                name = other.fullName ?: "Relun user",
                lastMessage = conv.lastMessage.content,
                lastFromMe = conv.lastMessage.senderId == myId,
                lastAt = conv.lastMessage.createdAt.toInstant(),
                unread = conv.unreadCount,
                chatUnlocked = conv.chatUnlocked,
            )
        }
    }.onSuccess { list -> _unreadTotal.value = list.count { it.unread > 0 } }

    fun refreshUnread() {
        scope.launch { conversations() }
    }

    suspend fun history(userId: String): Result<List<ChatMessage>> =
        apiCall { api.messages(userId) }.map { res -> res.messages.map { it.toChatMessage() } }

    fun MessageDto.toChatMessage(clientId: String? = null) = ChatMessage(
        id = id,
        clientId = clientId,
        mine = senderId == myId,
        text = content,
        sentAt = createdAt.toInstant(),
        status = if (senderId == myId && isRead) MessageStatus.Read else MessageStatus.Sent,
    )

    fun clear() {
        _unreadTotal.value = 0
        openChatUserId = null
    }

    private fun String.toInstant(): Instant = runCatching { Instant.parse(this) }.getOrDefault(Instant.now())
}
