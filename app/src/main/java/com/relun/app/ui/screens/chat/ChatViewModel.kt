package com.relun.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relun.app.data.model.ChatMessage
import com.relun.app.data.model.MessageRequest
import com.relun.app.data.model.MessageStatus
import com.relun.app.data.model.Person
import com.relun.app.data.repository.PeopleEvent
import com.relun.app.data.socket.SocketEvent
import com.relun.app.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

data class ChatState(
    val name: String,
    val person: Person? = null,
    val messages: List<ChatMessage> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val otherTyping: Boolean = false,
    val otherOnline: Boolean = false,
    val connected: Boolean = true,
    val draft: String = "",
    /** Set while this chat runs on a message request rather than a match. */
    val request: MessageRequest? = null,
    val declining: Boolean = false,
) {
    /** Why the sender of a message request can't write right now, if they can't. */
    val requestBlock: RequestBlock?
        get() = when {
            request == null || !request.outgoing -> null
            request.declined -> RequestBlock.Declined
            request.remaining <= 0 -> RequestBlock.Waiting
            else -> null
        }

    /** The request was accepted (a reply either way): the two are now a match with the chat open. */
    fun accepted(): ChatState = copy(
        request = null,
        person = person?.copy(isMatch = true, liked = true, messageRequest = null),
    )
}

enum class RequestBlock { Declined, Waiting }

class ChatViewModel(private val c: AppContainer, private val userId: String, initialName: String) : ViewModel() {

    private val _state = MutableStateFlow(ChatState(name = initialName))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var typingSent = false
    private var typingJob: Job? = null
    private var typingTimeout: Job? = null

    init {
        c.chat.openChatUserId = userId
        c.socket.join(userId)
        load()

        viewModelScope.launch { c.socket.connected.collect { live -> _state.update { it.copy(connected = live) } } }
        viewModelScope.launch {
            c.people.events.collect { e ->
                if (e is PeopleEvent.Matched && e.userId == userId) {
                    _state.update { it.copy(person = it.person?.copy(isMatch = true)) }
                }
            }
        }
        viewModelScope.launch { c.socket.events.collect(::onSocketEvent) }
    }

    fun load() {
        _state.update { it.copy(loading = it.messages.isEmpty(), error = null) }
        viewModelScope.launch {
            c.people.person(userId).onSuccess { p ->
                _state.update { it.copy(person = p, name = p.name, otherOnline = p.isOnline) }
            }
            with(c.chat) {
                thread(userId)
                    .onSuccess { thread ->
                        _state.update { s ->
                            // Keep any unsent bubbles from this session.
                            val pending = s.messages.filter { it.status == MessageStatus.Sending || it.status == MessageStatus.Failed }
                            s.copy(messages = thread.messages + pending, request = thread.request, loading = false)
                        }
                        c.socket.markRead(userId)
                        refreshUnread()
                    }
                    .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
            }
        }
    }

    fun setDraft(text: String) {
        _state.update { it.copy(draft = text.take(5000)) }
        signalTyping(text.isNotBlank())
    }

    fun send(textOverride: String? = null) {
        val text = (textOverride ?: _state.value.draft).trim()
        if (text.isEmpty() || _state.value.requestBlock != null) return
        val clientId = UUID.randomUUID().toString()
        val bubble = ChatMessage(
            id = clientId,
            clientId = clientId,
            mine = true,
            text = text,
            sentAt = Instant.now(),
            status = MessageStatus.Sending,
        )
        _state.update {
            it.copy(
                messages = it.messages + bubble,
                draft = if (textOverride == null) "" else it.draft,
                // One fewer message left on a request; the server has the final say.
                request = it.request?.let { r -> if (r.outgoing) r.copy(remaining = r.remaining - 1) else r },
            )
        }
        signalTyping(false)
        dispatch(bubble)
    }

    fun retry(message: ChatMessage) {
        _state.update { s -> s.copy(messages = s.messages.map { if (it.id == message.id) it.copy(status = MessageStatus.Sending) else it }) }
        dispatch(message)
    }

    private fun dispatch(message: ChatMessage) {
        val clientId = message.clientId ?: return
        if (!c.socket.send(userId, message.text, clientId)) {
            setStatus(clientId, MessageStatus.Failed)
            return
        }
        // No echo within 10s means it didn't land.
        viewModelScope.launch {
            delay(10_000)
            val still = _state.value.messages.firstOrNull { it.clientId == clientId }
            if (still?.status == MessageStatus.Sending) setStatus(clientId, MessageStatus.Failed)
        }
    }

    private fun onSocketEvent(event: SocketEvent) {
        when (event) {
            is SocketEvent.NewMessage -> {
                val m = event.message
                if (m.senderId != userId && m.receiverId != userId) return
                with(c.chat) {
                    val incoming = m.toChatMessage(event.clientId)
                    // A reply on a request accepts it, whichever side this device is.
                    val req = _state.value.request
                    val replied = req != null && !req.declined && incoming.mine != req.outgoing
                    _state.update { s ->
                        val mine = s.messages.indexOfFirst { event.clientId != null && it.clientId == event.clientId }
                        val messages = when {
                            mine >= 0 -> s.messages.toMutableList().also { it[mine] = incoming.copy(status = MessageStatus.Sent) }
                            s.messages.any { it.id == incoming.id } -> s.messages
                            else -> s.messages + incoming
                        }
                        val next = s.copy(messages = messages, otherTyping = if (!incoming.mine) false else s.otherTyping)
                        if (replied) next.accepted() else next
                    }
                    if (!incoming.mine) c.socket.markRead(userId)
                    if (replied) c.people.emit(PeopleEvent.Matched(userId))
                }
            }
            is SocketEvent.Typing -> if (event.userId == userId) {
                _state.update { it.copy(otherTyping = event.isTyping) }
                typingTimeout?.cancel()
                if (event.isTyping) typingTimeout = viewModelScope.launch {
                    delay(6_000)
                    _state.update { it.copy(otherTyping = false) }
                }
            }
            is SocketEvent.Read -> if (event.readBy == userId) {
                _state.update { s -> s.copy(messages = s.messages.map { if (it.mine && it.status == MessageStatus.Sent) it.copy(status = MessageStatus.Read) else it }) }
            }
            is SocketEvent.Presence -> if (event.userId == userId) _state.update { it.copy(otherOnline = event.isOnline) }
            is SocketEvent.Error -> {
                event.clientId?.let { setStatus(it, MessageStatus.Failed) }
                when (event.code) {
                    "REQUEST_LIMIT" -> _state.update { it.copy(request = it.request?.copy(remaining = 0)) }
                    "REQUEST_DECLINED" -> _state.update { it.copy(request = it.request?.copy(declined = true, remaining = 0)) }
                }
            }
            is SocketEvent.MessageNotification -> Unit
        }
    }

    /** Turns down an incoming message request; [onDone] runs once it's gone. */
    fun decline(onDone: () -> Unit) {
        if (_state.value.declining) return
        _state.update { it.copy(declining = true) }
        viewModelScope.launch {
            c.chat.declineRequest(userId)
                .onSuccess {
                    c.people.emit(PeopleEvent.RequestDeclined(userId))
                    c.chat.refreshUnread()
                    onDone()
                }
                .onFailure { e ->
                    _state.update { it.copy(declining = false) }
                    c.messenger.error(e.message ?: "Couldn’t decline. Try again.")
                }
        }
    }

    private fun setStatus(clientId: String, status: MessageStatus) = _state.update { s ->
        s.copy(messages = s.messages.map { if (it.clientId == clientId && it.status != MessageStatus.Sent && it.status != MessageStatus.Read) it.copy(status = status) else it })
    }

    private fun signalTyping(typing: Boolean) {
        typingJob?.cancel()
        if (typing) {
            if (!typingSent) {
                c.socket.typing(userId, true)
                typingSent = true
            }
            typingJob = viewModelScope.launch {
                delay(2_500)
                c.socket.typing(userId, false)
                typingSent = false
            }
        } else if (typingSent) {
            c.socket.typing(userId, false)
            typingSent = false
        }
    }

    override fun onCleared() {
        if (typingSent) c.socket.typing(userId, false)
        c.socket.leave(userId)
        if (c.chat.openChatUserId == userId) c.chat.openChatUserId = null
        c.chat.refreshUnread()
    }
}
