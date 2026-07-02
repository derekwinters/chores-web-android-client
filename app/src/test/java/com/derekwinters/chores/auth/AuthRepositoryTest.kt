package com.derekwinters.chores.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behavior: Login screen calls POST /auth/login and persists token + URL (area: android, ui, network)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class AuthRepositoryTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var fakeAuthApi: FakeAuthApi
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_auth_repository_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        sessionManager = SessionManager(prefs)
        fakeAuthApi = FakeAuthApi()
        repository = AuthRepository(fakeAuthApi, sessionManager)
    }

    @Test
    fun login_success_persistsServerUrlAndTokenAndReturnsUser() = runTest {
        fakeAuthApi.loginResult = Result.success(
            LoginResponse(
                accessToken = "jwt-token",
                tokenType = "bearer",
                user = UserInfo(username = "alice", isAdmin = false)
            )
        )

        val user = repository.login("http://example.com:8000", "alice", "hunter2")

        assertEquals("alice", user.username)
        assertEquals("jwt-token", sessionManager.token)
        assertEquals("http://example.com:8000", sessionManager.serverUrl)
        assertEquals(AuthState.LOGGED_IN, sessionManager.authState.value)
        assertEquals(LoginRequest("alice", "hunter2"), fakeAuthApi.lastRequest)
    }

    @Test
    fun login_failure_persistsServerUrlButNotToken() = runTest {
        fakeAuthApi.loginResult = Result.failure(IllegalStateException("401"))

        try {
            repository.login("http://example.com:8000", "alice", "wrong-password")
        } catch (expected: IllegalStateException) {
            // expected
        }

        assertEquals("http://example.com:8000", sessionManager.serverUrl)
        assertEquals(null, sessionManager.token)
        assertEquals(AuthState.LOGGED_OUT, sessionManager.authState.value)
    }

    @Test
    fun logout_clearsToken() = runTest {
        fakeAuthApi.loginResult = Result.success(
            LoginResponse("jwt-token", "bearer", UserInfo("alice", false))
        )
        repository.login("http://example.com:8000", "alice", "hunter2")

        repository.logout()

        assertEquals(null, sessionManager.token)
        assertEquals(AuthState.LOGGED_OUT, sessionManager.authState.value)
    }
}

private class FakeAuthApi : AuthApi {
    var loginResult: Result<LoginResponse> = Result.failure(IllegalStateException("not stubbed"))
    var lastRequest: LoginRequest? = null

    override suspend fun login(request: LoginRequest): LoginResponse {
        lastRequest = request
        return loginResult.getOrThrow()
    }

    override suspend fun me(): UserInfo = throw NotImplementedError()
}
