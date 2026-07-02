package com.derekwinters.chores.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.auth.SessionManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behavior: OkHttp interceptor attaches `Authorization: Bearer <token>` header (area: network)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var sessionManager: SessionManager
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_auth_interceptor_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        sessionManager = SessionManager(prefs)

        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun intercept_withStoredToken_attachesBearerAuthorizationHeader() {
        sessionManager.saveToken("jwt-token")
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/v1/chores")).build()).execute()
        val recorded = server.takeRequest()

        assertEquals("Bearer jwt-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun intercept_withNoStoredToken_doesNotAttachAuthorizationHeader() {
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/v1/auth/login")).build()).execute()
        val recorded = server.takeRequest()

        assertNull(recorded.getHeader("Authorization"))
    }
}
