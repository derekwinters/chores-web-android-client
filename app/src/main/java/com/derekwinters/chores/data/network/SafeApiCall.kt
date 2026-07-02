package com.derekwinters.chores.data.network

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private const val TAG = "ChoresApi"

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
    Log.w(TAG, "Network I/O failure", e)
    Result.failure(ApiException(-1, HttpErrorMessages.NETWORK_ERROR))
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // Most plausibly a response body that doesn't parse into the expected DTO shape (see class
    // doc) rather than a real connectivity problem — logged distinctly from the IOException case
    // above since both surface the same generic NETWORK_ERROR message to the user.
    Log.e(TAG, "Unexpected ${e.javaClass.simpleName}, likely a response shape mismatch", e)
    Result.failure(ApiException(-1, HttpErrorMessages.NETWORK_ERROR))
}
