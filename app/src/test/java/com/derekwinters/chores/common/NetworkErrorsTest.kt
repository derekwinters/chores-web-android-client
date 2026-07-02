package com.derekwinters.chores.common

import java.io.IOException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Behavior: error messages reuse the same per-status-code fallback text as chores-web's
 * client.js STATUS_CODE_FALLBACK (area: android, ui)
 */
class NetworkErrorsTest {

    @Test
    fun statusCodeFallback_matchesWebClientMessages() {
        assertEquals("Invalid input — check your values", StatusCodeFallback.messageFor(400))
        assertEquals("Session expired, please log in", StatusCodeFallback.messageFor(401))
        assertEquals("You don't have permission to do that", StatusCodeFallback.messageFor(403))
        assertEquals("Not found", StatusCodeFallback.messageFor(404))
        assertEquals("Already exists", StatusCodeFallback.messageFor(409))
        assertEquals("Invalid input — check your values", StatusCodeFallback.messageFor(422))
        assertEquals("Something went wrong. Please try again.", StatusCodeFallback.messageFor(500))
        assertEquals("Service unavailable, please try again later", StatusCodeFallback.messageFor(503))
    }

    @Test
    fun statusCodeFallback_unmappedCode_returnsGenericMessage() {
        assertEquals("Unexpected error (code 418)", StatusCodeFallback.messageFor(418))
    }

    @Test
    fun toUserMessage_httpException_usesStatusCodeFallback() {
        val httpException = HttpException(
            Response.error<Any>(404, "".toResponseBody(null))
        )

        assertEquals("Not found", httpException.toUserMessage())
    }

    @Test
    fun toUserMessage_ioException_usesConnectivityMessage() {
        val error: Throwable = IOException("timeout")

        assertEquals(
            "Couldn't reach the server — check your connection and server URL",
            error.toUserMessage()
        )
    }

    @Test
    fun toUserMessage_otherException_fallsBackToExceptionMessageOrGeneric() {
        val error: Throwable = IllegalStateException("boom")

        assertEquals("boom", error.toUserMessage())
    }
}
