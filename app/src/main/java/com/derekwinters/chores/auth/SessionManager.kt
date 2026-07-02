package com.derekwinters.chores.auth

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the app currently holds a valid session token.
 */
enum class AuthState { LOGGED_IN, LOGGED_OUT }

/**
 * Persists the auth token (365-day JWT) and user-entered server URL in EncryptedSharedPreferences,
 * and exposes the current [AuthState] as a [StateFlow] so the UI layer can react to login/logout —
 * including the global-401 case, where [clearToken] is called from a background OkHttp interceptor
 * and the root composable (observing [authState]) reactively navigates back to the Login screen.
 *
 * See docs/adr/0002-network-auth-architecture.md.
 */
@Singleton
class SessionManager @Inject constructor(
    private val securePrefs: SharedPreferences
) {

    private val _authState = MutableStateFlow(if (token != null) AuthState.LOGGED_IN else AuthState.LOGGED_OUT)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** The user-entered self-hosted server URL, e.g. "http://192.168.1.20:8000". Null until set. */
    val serverUrl: String?
        get() = securePrefs.getString(KEY_SERVER_URL, null)

    /** The persisted auth token, or null if logged out. */
    val token: String?
        get() = securePrefs.getString(KEY_TOKEN, null)

    /**
     * Persists the server URL immediately, independent of login success, so the
     * [com.derekwinters.chores.network.ServerUrlInterceptor] can rewrite the login request itself
     * to the entered address.
     */
    fun updateServerUrl(url: String) {
        securePrefs.edit { putString(KEY_SERVER_URL, url) }
    }

    /** Persists the token on successful login and flips [authState] to [AuthState.LOGGED_IN]. */
    fun saveToken(token: String) {
        securePrefs.edit { putString(KEY_TOKEN, token) }
        _authState.value = AuthState.LOGGED_IN
    }

    /**
     * Clears only the token (keeping the server URL, so it's prefilled next time), and flips
     * [authState] to [AuthState.LOGGED_OUT]. Called on explicit logout and on global 401 handling.
     */
    fun clearToken() {
        securePrefs.edit { remove(KEY_TOKEN) }
        _authState.value = AuthState.LOGGED_OUT
    }

    private companion object {
        const val KEY_TOKEN = "auth_token"
        const val KEY_SERVER_URL = "server_url"
    }
}
