package com.relun.app.data.repository

import com.relun.app.data.model.NotificationDto
import com.relun.app.data.network.ApiService
import com.relun.app.data.network.apiCall
import com.relun.app.data.socket.ChatSocket
import com.relun.app.data.socket.SocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The notification list behind the bell. Chat messages have their own badge. */
class InboxRepository(
    private val api: ApiService,
    socket: ChatSocket,
    private val scope: CoroutineScope,
) {
    private val _unread = MutableStateFlow(0)
    /** Unread notifications; the bell's badge. */
    val unread: StateFlow<Int> = _unread.asStateFlow()

    init {
        scope.launch {
            socket.events.collect { if (it is SocketEvent.Notification) _unread.value = it.unreadCount }
        }
    }

    suspend fun list(): Result<List<NotificationDto>> = apiCall { api.notifications() }.map { res ->
        _unread.value = res.unreadCount
        res.notifications
    }

    fun refreshUnread() {
        scope.launch { apiCall { api.notificationsUnread() }.onSuccess { _unread.value = it.unreadCount } }
    }

    /** Opening the list reads everything. */
    fun markAllRead() {
        _unread.value = 0
        scope.launch { apiCall { api.markNotificationsRead() } }
    }

    fun clear() {
        _unread.value = 0
    }
}
