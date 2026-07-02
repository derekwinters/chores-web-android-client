package com.derekwinters.chores.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.auth.SessionManager
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behavior: OkHttp interceptor rewrites request URL from stored server-URL preference (area: network)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ServerUrlInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var sessionManager: SessionManager
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_server_url_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        sessionManager = SessionManager(prefs)

        client = OkHttpClient.Builder()
            .addInterceptor(ServerUrlInterceptor(sessionManager))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun intercept_rewritesSchemeHostAndPort_toStoredServerUrl_keepingPath() {
        sessionManager.updateServerUrl(server.url("/").toString())
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val request = Request.Builder()
            .url("http://placeholder.invalid/v1/chores")
            .build()

        val response = client.newCall(request).execute()
        val recorded = server.takeRequest()

        assertEquals(200, response.code)
        assertEquals("/v1/chores", recorded.path)
    }

    @Test
    fun intercept_withUnschemedServerUrl_defaultsToHttp() {
        val hostAndPort = "${server.hostName}:${server.port}"
        sessionManager.updateServerUrl(hostAndPort)
        server.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url("http://placeholder.invalid/v1/chores")
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
    }

    @Test
    fun intercept_withNoStoredServerUrl_leavesRequestUnchanged() {
        val request = Request.Builder()
            .url("http://placeholder.invalid/v1/chores")
            .build()

        // ".invalid" never resolves (RFC 2606); a successful rewrite to the mock server would
        // succeed, so failure here proves the interceptor left the original host alone.
        try {
            client.newCall(request).execute()
            fail("Expected connection failure to unreachable placeholder host")
        } catch (expected: IOException) {
            // expected
        }
    }
}
