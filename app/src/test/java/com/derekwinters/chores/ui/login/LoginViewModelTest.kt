package com.derekwinters.chores.ui.login

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.derekwinters.chores.auth.AuthApi
import com.derekwinters.chores.auth.AuthRepository
import com.derekwinters.chores.auth.LoginRequest
import com.derekwinters.chores.auth.LoginResponse
import com.derekwinters.chores.auth.SessionManager
import com.derekwinters.chores.auth.UserInfo
import com.derekwinters.chores.common.UiState
import com.derekwinters.chores.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

/**
 * Behavior: Login screen calls POST /auth/login and persists token + URL; sealed UiState pattern
 * (area: android, ui, network)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAuthApi: FakeAuthApi
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("test_login_view_model_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        sessionManager = SessionManager(prefs)
        fakeAuthApi = FakeAuthApi()
        viewModel = LoginViewModel(AuthRepository(fakeAuthApi, sessionManager))
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(null, viewModel.uiState.value)
    }

    @Test
    fun login_blankFields_setsErrorWithoutCallingApi() {
        viewModel.login(serverUrl = "", username = "alice", password = "hunter2")

        assertTrue(viewModel.uiState.value is UiState.Error)
        assertEquals(0, fakeAuthApi.callCount)
    }

    @Test
    fun login_success_emitsLoadingThenSuccess_andPersistsSession() = runTest {
        fakeAuthApi.result = Result.success(
            LoginResponse("jwt-token", "bearer", UserInfo("alice", false))
        )

        viewModel.uiState.test {
            assertEquals(null, awaitItem())

            viewModel.login("http://example.com:8000", "alice", "hunter2")

            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Success(Unit), awaitItem())
        }

        assertEquals("jwt-token", sessionManager.token)
        assertEquals("http://example.com:8000", sessionManager.serverUrl)
    }

    @Test
    fun login_failure_emitsLoadingThenError_usingStatusCodeFallback() = runTest {
        fakeAuthApi.result = Result.failure(
            HttpException(Response.error<Any>(401, "".toResponseBody(null)))
        )

        viewModel.uiState.test {
            assertEquals(null, awaitItem())

            viewModel.login("http://example.com:8000", "alice", "wrong-password")

            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error("Session expired, please log in"), awaitItem())
        }

        assertEquals(null, sessionManager.token)
    }
}

private class FakeAuthApi : AuthApi {
    var result: Result<LoginResponse> = Result.failure(IllegalStateException("not stubbed"))
    var callCount = 0

    override suspend fun login(request: LoginRequest): LoginResponse {
        callCount++
        return result.getOrThrow()
    }

    override suspend fun me(): UserInfo = throw NotImplementedError()
}
