package com.derekwinters.chores.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.auth.AuthState
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
 * Behavior: global 401 handling clears the stored token, mirroring chores-web's
 * frontend/src/api/client.js (area: network, android)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class UnauthorizedInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var sessionManager: SessionManager
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_unauthorized_interceptor_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        sessionManager = SessionManager(prefs)
        sessionManager.updateServerUrl(server.url("/").toString())

        client = OkHttpClient.Builder()
            .addInterceptor(UnauthorizedInterceptor(sessionManager))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun intercept_on401Response_clearsTokenAndFlipsAuthStateToLoggedOut() {
        sessionManager.saveToken("jwt-token")
        server.enqueue(MockResponse().setResponseCode(401))

        client.newCall(Request.Builder().url(server.url("/v1/chores")).build()).execute()

        assertNull(sessionManager.token)
        assertEquals(AuthState.LOGGED_OUT, sessionManager.authState.value)
    }

    @Test
    fun intercept_onSuccessfulResponse_leavesTokenIntact() {
        sessionManager.saveToken("jwt-token")
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/v1/chores")).build()).execute()

        assertEquals("jwt-token", sessionManager.token)
        assertEquals(AuthState.LOGGED_IN, sessionManager.authState.value)
    }
}
