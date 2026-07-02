package com.derekwinters.chores.common

import java.io.IOException
import retrofit2.HttpException

/**
 * Per-status-code fallback error text, mirroring chores-web's `STATUS_CODE_FALLBACK` in
 * `frontend/src/api/client.js` so error messaging is consistent between the web and Android
 * clients (see docs/adr/0002-network-auth-architecture.md).
 */
object StatusCodeFallback {
    private val messages = mapOf(
        400 to "Invalid input — check your values",
        401 to "Session expired, please log in",
        403 to "You don't have permission to do that",
        404 to "Not found",
        409 to "Already exists",
        422 to "Invalid input — check your values",
        500 to "Something went wrong. Please try again.",
        503 to "Service unavailable, please try again later",
    )

    /** Returns the fallback message for [code], or a generic message if the code is unmapped. */
    fun messageFor(code: Int): String = messages[code] ?: "Unexpected error (code $code)"
}

/**
 * Maps a [Throwable] caught around a network call to a user-facing message, using
 * [StatusCodeFallback] for HTTP error responses and a generic message for connectivity failures.
 */
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> StatusCodeFallback.messageFor(code())
    is IOException -> "Couldn't reach the server — check your connection and server URL"
    else -> message ?: "Something went wrong. Please try again."
}
