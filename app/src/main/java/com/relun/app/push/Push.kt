package com.relun.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.relun.app.MainActivity
import com.relun.app.R
import com.relun.app.RelunApp
import com.relun.app.data.model.PushTokenBody
import com.relun.app.data.network.ApiService
import com.relun.app.data.network.apiCall
import com.relun.app.data.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

const val CHANNEL_MESSAGES = "messages"
const val EXTRA_PUSH_TYPE = "relun.push.type"
const val EXTRA_PUSH_USER = "relun.push.userId"

fun createNotificationChannels(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_MESSAGES,
        context.getString(R.string.channel_messages),
        NotificationManager.IMPORTANCE_HIGH,
    ).apply { description = context.getString(R.string.channel_messages_desc) }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

/**
 * Registers this device's FCM token with the backend. Does nothing when
 * Firebase is not configured (no google-services.json), so the app still runs.
 */
class PushRegistrar(
    private val context: Context,
    private val api: ApiService,
    private val session: SessionStore,
    private val scope: CoroutineScope,
) {
    private val firebaseReady: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty()

    fun register() {
        if (!firebaseReady || !session.current.isSignedIn) return
        scope.launch {
            val token = runCatching { FirebaseMessaging.getInstance().token.await() }
                .onFailure { Log.w("Push", "No FCM token", it) }
                .getOrNull() ?: return@launch
            send(token)
        }
    }

    fun onNewToken(token: String) {
        if (session.current.isSignedIn) scope.launch { send(token) }
    }

    private suspend fun send(token: String) {
        apiCall { api.registerPushToken(PushTokenBody(token)) }
            .onSuccess { session.savePushToken(token) }
    }

    /** Called before the session is cleared, while the request can still authenticate. */
    suspend fun unregister() {
        val token = session.current.pushToken ?: return
        apiCall { api.deletePushToken(PushTokenBody(token)) }
        session.savePushToken(null)
    }
}

class RelunMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        (application as RelunApp).container.push.onNewToken(token)
    }

    /**
     * Notification messages are drawn by the system when the app is in the
     * background. In the foreground they arrive here; chat and match updates
     * already reach the open app over the socket, so only show what it can't.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val container = (application as RelunApp).container
        val type = message.data["type"]
        val userId = message.data["userId"]

        if (type == "message" && userId != null && container.chat.openChatUserId == userId) return
        container.chat.refreshUnread()

        val title = message.notification?.title ?: return
        val body = message.notification?.body.orEmpty()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PUSH_TYPE, type)
            putExtra(EXTRA_PUSH_USER, userId)
        }
        val pending = PendingIntent.getActivity(
            this,
            (userId ?: title).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(getColor(R.color.relun_rose))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        runCatching {
            NotificationManagerCompat.from(this).notify((userId ?: title).hashCode(), notification)
        }
    }
}
