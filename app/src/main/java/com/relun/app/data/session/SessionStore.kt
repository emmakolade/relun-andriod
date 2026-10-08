package com.relun.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.sessionDataStore by preferencesDataStore(name = "session")

data class Session(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    // How the user signed in: "phone" or "email". Decides which contact the
    // details step asks for as the recovery method.
    val authMethod: String? = null,
    val pushToken: String? = null,
) {
    val isSignedIn: Boolean get() = !accessToken.isNullOrBlank()
}

/**
 * Tokens and identity, persisted in DataStore and mirrored in memory so the
 * OkHttp interceptor can read them synchronously.
 */
class SessionStore(private val context: Context, @Suppress("unused") private val scope: CoroutineScope) {

    private object Keys {
        val access = stringPreferencesKey("access_token")
        val refresh = stringPreferencesKey("refresh_token")
        val userId = stringPreferencesKey("user_id")
        val method = stringPreferencesKey("auth_method")
        val push = stringPreferencesKey("push_token")
    }

    private val _session = MutableStateFlow(
        // One blocking read at startup, before any request can need a token.
        runBlocking { read(context.sessionDataStore.data.first()) }
    )
    val session: StateFlow<Session> = _session.asStateFlow()

    val current: Session get() = _session.value

    private fun read(prefs: Preferences) = Session(
        accessToken = prefs[Keys.access],
        refreshToken = prefs[Keys.refresh],
        userId = prefs[Keys.userId],
        authMethod = prefs[Keys.method],
        pushToken = prefs[Keys.push],
    )

    suspend fun saveSignIn(accessToken: String, refreshToken: String, userId: String, method: String) {
        context.sessionDataStore.edit {
            it[Keys.access] = accessToken
            it[Keys.refresh] = refreshToken
            it[Keys.userId] = userId
            it[Keys.method] = method
        }
        _session.value = _session.value.copy(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            authMethod = method,
        )
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.sessionDataStore.edit {
            it[Keys.access] = accessToken
            it[Keys.refresh] = refreshToken
        }
        _session.value = _session.value.copy(accessToken = accessToken, refreshToken = refreshToken)
    }

    suspend fun savePushToken(token: String?) {
        context.sessionDataStore.edit {
            if (token == null) it.remove(Keys.push) else it[Keys.push] = token
        }
        _session.value = _session.value.copy(pushToken = token)
    }

    suspend fun clear() {
        context.sessionDataStore.edit {
            it.remove(Keys.access)
            it.remove(Keys.refresh)
            it.remove(Keys.userId)
            it.remove(Keys.method)
        }
        _session.value = Session(pushToken = _session.value.pushToken)
    }
}
