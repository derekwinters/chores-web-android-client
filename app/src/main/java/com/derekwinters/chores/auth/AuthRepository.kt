package com.derekwinters.chores.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles login: applies the entered server URL immediately (so the interceptor rewrites the
 * login request itself to the right host — see ADR 0002), then calls `POST /auth/login` and
 * persists the returned token on success.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) {

    suspend fun login(serverUrl: String, username: String, password: String): UserInfo {
        sessionManager.updateServerUrl(serverUrl)
        val response = authApi.login(LoginRequest(username = username, password = password))
        sessionManager.saveToken(response.accessToken)
        return response.user
    }

    fun logout() {
        sessionManager.clearToken()
    }
}
