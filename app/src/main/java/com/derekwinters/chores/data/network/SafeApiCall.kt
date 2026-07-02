package com.derekwinters.chores.data.network

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Serializable
private data class ErrorDetailDto(val detail: String? = null)

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Runs [block] and converts any failure into an [ApiException], preferring the backend's
 * `{"detail": "..."}` body (FastAPI's default error shape) and falling back to
 * [HttpErrorMessages] by status code, matching client.js's error-handling strategy.
 *
 * Several backend endpoint response shapes used by this app were inferred from issue text
 * rather than the real `chores-web` server source, so a body that doesn't parse the way its DTO
 * expects is a real, observed failure mode (not just theoretical) — [SerializationException] (and
 * any other unexpected exception) is caught here too so a shape mismatch surfaces as a normal
 * [ApiException] Result instead of escaping uncaught from a `viewModelScope.launch` and crashing
 * the app. [CancellationException] is rethrown since swallowing it would break coroutine
 * cancellation/structured concurrency.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    val detail = e.response()?.errorBody()?.string()
        ?.let { runCatching { errorJson.decodeFromString<ErrorDetailDto>(it) }.getOrNull() }
        ?.detail
    Result.failure(ApiException(e.code(), detail ?: HttpErrorMessages.forStatusCode(e.code())))
} catch (e: IOException) {
    Result.failure(ApiException(-1, HttpErrorMessages.NETWORK_ERROR))
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(ApiException(-1, HttpErrorMessages.NETWORK_ERROR))
}
