package com.derekwinters.chores.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behaviors: token + server URL persistence in (Encrypted)SharedPreferences, and reactive
 * AuthState used by the global-401 handling and root nav gating (area: android, network)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SessionManagerTest {

    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_session_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        sessionManager = SessionManager(prefs)
    }

    @Test
    fun initialState_withNoStoredToken_isLoggedOut() {
        assertEquals(AuthState.LOGGED_OUT, sessionManager.authState.value)
        assertNull(sessionManager.token)
    }

    @Test
    fun updateServerUrl_persistsImmediately_withoutAffectingAuthState() {
        sessionManager.updateServerUrl("http://192.168.1.20:8000")

        assertEquals("http://192.168.1.20:8000", sessionManager.serverUrl)
        assertEquals(AuthState.LOGGED_OUT, sessionManager.authState.value)
    }

    @Test
    fun saveToken_persistsTokenAndFlipsAuthStateToLoggedIn() {
        sessionManager.updateServerUrl("http://example.com")
        sessionManager.saveToken("jwt-token")

        assertEquals("jwt-token", sessionManager.token)
        assertEquals(AuthState.LOGGED_IN, sessionManager.authState.value)
    }

    @Test
    fun clearToken_removesTokenButKeepsServerUrl_andFlipsAuthStateToLoggedOut() {
        sessionManager.updateServerUrl("http://example.com")
        sessionManager.saveToken("jwt-token")

        sessionManager.clearToken()

        assertNull(sessionManager.token)
        assertEquals("http://example.com", sessionManager.serverUrl)
        assertEquals(AuthState.LOGGED_OUT, sessionManager.authState.value)
    }
}
