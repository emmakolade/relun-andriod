package com.relun.app.push

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Why the notifications card is showing; picks its wording. */
enum class PushAskReason { Login, Match, Request, Date }

/**
 * When to show our own "turn on notifications" card before Android's prompt.
 * The system prompt can be refused for good, so we ask ourselves first, at a
 * moment it obviously helps, and back off: once a week at most, three times
 * in all.
 */
class PushAskPolicy(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("relun_push_ask", Context.MODE_PRIVATE)

    private val _requests = MutableSharedFlow<PushAskReason>(extraBufferCapacity = 4)
    /** Screens ask for the card here; the shell decides if it's a good time. */
    val requests: SharedFlow<PushAskReason> = _requests.asSharedFlow()

    fun request(reason: PushAskReason) {
        _requests.tryEmit(reason)
    }

    val notificationsEnabled: Boolean get() = NotificationManagerCompat.from(appContext).areNotificationsEnabled()

    /** Whether the system prompt was shown before; after that a refusal may be final. */
    var systemPromptShown: Boolean
        get() = prefs.getBoolean(KEY_SYSTEM, false)
        set(value) = prefs.edit().putBoolean(KEY_SYSTEM, value).apply()

    /** True if notifications are off and the weekly / three-times limits allow asking. */
    fun shouldAsk(): Boolean {
        if (notificationsEnabled) return false
        val count = prefs.getInt(KEY_COUNT, 0)
        val last = prefs.getLong(KEY_LAST, 0L)
        return count < MAX_ASKS && System.currentTimeMillis() - last >= WEEK_MS
    }

    fun markAsked() {
        prefs.edit()
            .putInt(KEY_COUNT, prefs.getInt(KEY_COUNT, 0) + 1)
            .putLong(KEY_LAST, System.currentTimeMillis())
            .apply()
    }

    private companion object {
        const val KEY_COUNT = "count"
        const val KEY_LAST = "last"
        const val KEY_SYSTEM = "system_prompt_shown"
        const val MAX_ASKS = 3
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    }
}
