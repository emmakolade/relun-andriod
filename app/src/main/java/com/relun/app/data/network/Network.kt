package com.relun.app.data.network

import com.relun.app.BuildConfig
import com.relun.app.data.model.RefreshBody
import com.relun.app.data.model.TokenPair
import com.relun.app.data.session.SessionStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val relunJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

/** Adds the bearer token to every request except the auth endpoints. */
private class AuthInterceptor(private val session: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = session.current.accessToken
        if (token.isNullOrBlank() || request.header("Authorization") != null) {
            return chain.proceed(request)
        }
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
    }
}

/**
 * On a 401, swaps the refresh token for a new pair once and retries. If that
 * fails the session is over: [onSessionExpired] signs the user out.
 */
private class TokenAuthenticator(
    private val session: SessionStore,
    private val baseUrl: String,
    private val onSessionExpired: () -> Unit,
) : Authenticator {

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Only one retry per request.
        if (response.priorResponse != null) return null
        if (response.request.url.encodedPath.startsWith("/api/auth/")) return null

        synchronized(this) {
            val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
            val current = session.current

            // Another request already refreshed while this one waited.
            if (!current.accessToken.isNullOrBlank() && current.accessToken != sent) {
                return response.request.withToken(current.accessToken)
            }

            val refreshToken = current.refreshToken ?: return expire()
            val newPair = refresh(refreshToken) ?: return expire()
            runBlocking { session.saveTokens(newPair.accessToken, newPair.refreshToken) }
            return response.request.withToken(newPair.accessToken)
        }
    }

    private fun refresh(refreshToken: String): TokenPair? = runCatching {
        val body = relunJson.encodeToString(RefreshBody.serializer(), RefreshBody(refreshToken))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(baseUrl + "api/auth/refresh").post(body).build()
        refreshClient.newCall(request).execute().use { res ->
            if (!res.isSuccessful) return null
            relunJson.decodeFromString(TokenPair.serializer(), res.body!!.string())
        }
    }.getOrNull()

    private fun expire(): Request? {
        onSessionExpired()
        return null
    }

    private fun Request.withToken(token: String) =
        newBuilder().header("Authorization", "Bearer $token").build()
}

class Network(session: SessionStore, onSessionExpired: () -> Unit) {

    val baseUrl: String = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // Photo uploads go to Cloudinary through the API and can be slow.
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(session))
        .authenticator(TokenAuthenticator(session, baseUrl, onSessionExpired))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(relunJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ApiService::class.java)
}
