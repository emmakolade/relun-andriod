package com.relun.app.data.network

import com.relun.app.data.model.ApiErrorBody
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** A failed call, reduced to what screens need: a message to show and a few flags. */
class ApiException(
    override val message: String,
    val status: Int? = null,
    val code: String? = null,
    val isNetwork: Boolean = false,
    cause: Throwable? = null,
) : Exception(message, cause) {
    val isInsufficientCoins: Boolean get() = code == "INSUFFICIENT_COINS" || status == 402
    val isUnauthorized: Boolean get() = status == 401
}

private val errorJson = Json { ignoreUnknownKeys = true }

fun Throwable.toApiException(): ApiException = when (this) {
    is ApiException -> this
    is HttpException -> {
        val body = response()?.errorBody()?.string()
        val parsed = body?.let { runCatching { errorJson.decodeFromString<ApiErrorBody>(it) }.getOrNull() }
        val message = parsed?.error
            ?: parsed?.errors?.firstOrNull()?.msg
            ?: when (code()) {
                401 -> "Your session has ended. Please sign in again."
                403 -> "You can’t do that."
                404 -> "We couldn’t find that."
                in 500..599 -> "Something went wrong on our side. Try again."
                else -> "Something went wrong. Try again."
            }
        ApiException(message, status = code(), code = parsed?.code, cause = this)
    }
    is IOException -> ApiException(
        "You’re offline. Check your connection and try again.",
        isNetwork = true,
        cause = this,
    )
    else -> ApiException(message ?: "Something went wrong. Try again.", cause = this)
}

/** Runs an API call and converts any failure into [ApiException]. */
suspend inline fun <T> apiCall(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancel: kotlinx.coroutines.CancellationException) {
        throw cancel
    } catch (error: Throwable) {
        Result.failure(error.toApiException())
    }
