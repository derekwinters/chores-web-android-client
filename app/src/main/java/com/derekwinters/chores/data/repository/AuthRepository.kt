package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.auth.CredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.model.CurrentUser
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.network.dto.LoginRequestDto
import com.derekwinters.chores.data.network.dto.LoginResetRequiredDto
import com.derekwinters.chores.data.network.dto.ResetPasswordRequestDto
import com.derekwinters.chores.data.network.dto.SetupRequestDto
import com.derekwinters.chores.data.network.safeApiCall
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Outcome of [AuthRepository.loginWithResetSupport] (issue #11). */
sealed interface LoginOutcome {
    data object Success : LoginOutcome
    data class ResetRequired(val resetToken: String) : LoginOutcome
}

private val loginErrorJson = Json { ignoreUnknownKeys = true }

/**
 * Issue #5 behavior: "Login screen ... calls POST /auth/login, persists token + URL". Extended
 * by issue #10 (logout) and issue #11 (first-run setup, forced password reset, DB-readiness
 * gate).
 *
 * [serverUrl] is persisted *before* any call is made, because
 * [com.derekwinters.chores.data.network.BaseUrlInterceptor] rewrites every request's host from
 * whatever is currently in [CredentialStore] — the request itself needs to target the server the
 * user just typed in.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: ChoresApi,
    private val credentialStore: CredentialStore,
    private val sessionManager: SessionManager
) {
    suspend fun login(serverUrl: String, username: String, password: String): Result<Unit> {
        credentialStore.setServerUrl(serverUrl)

        return safeApiCall { api.login(LoginRequestDto(username, password)) }
            .map { response ->
                credentialStore.saveToken(token = response.access_token, tokenType = response.token_type)
                sessionManager.onLoginSuccess()
            }
    }

    /**
     * Issue #11: like [login], but a 403 response carrying a `reset_token` resolves to
     * [LoginOutcome.ResetRequired] instead of a plain failure, so the caller can show the
     * "set new password" form rather than a dead-end error.
     */
    suspend fun loginWithResetSupport(serverUrl: String, username: String, password: String): Result<LoginOutcome> {
        credentialStore.setServerUrl(serverUrl)

        return try {
            val response = api.login(LoginRequestDto(username, password))
            credentialStore.saveToken(token = response.access_token, tokenType = response.token_type)
            sessionManager.onLoginSuccess()
            Result.success(LoginOutcome.Success)
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            val parsed = body?.let { runCatching { loginErrorJson.decodeFromString<LoginResetRequiredDto>(it) }.getOrNull() }
            val resetToken = parsed?.reset_token
            if (e.code() == 403 && !resetToken.isNullOrBlank()) {
                Result.success(LoginOutcome.ResetRequired(resetToken))
            } else {
                Result.failure(ApiException(e.code(), parsed?.detail ?: HttpErrorMessages.forStatusCode(e.code())))
            }
        } catch (e: IOException) {
            Result.failure(ApiException(-1, HttpErrorMessages.NETWORK_ERROR))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ApiException(-1, HttpErrorMessages.NETWORK_ERROR))
        }
    }

    /**
     * Issue #11: submits the new password using [resetToken] as bearer auth, then logs in with
     * it, matching the web flow's two-step "reset, then log in with the new password".
     */
    suspend fun resetPasswordAndLogin(
        serverUrl: String,
        resetToken: String,
        username: String,
        newPassword: String
    ): Result<Unit> {
        val resetResult = safeApiCall {
            api.resetPassword("Bearer $resetToken", ResetPasswordRequestDto(newPassword))
        }
        return resetResult.fold(
            onSuccess = { login(serverUrl, username, newPassword) },
            onFailure = { Result.failure(it) }
        )
    }

    /** Issue #11: first-run setup gate, checked once the user has entered a server URL. */
    suspend fun isSetupNeeded(serverUrl: String): Result<Boolean> {
        credentialStore.setServerUrl(serverUrl)
        return safeApiCall { api.getSetupStatus() }.map { it.setup_needed }
    }

    /**
     * Issue #11: creates the first (admin) user and logs them in, then best-effort applies the
     * "Require Authentication" toggle via config — a failure on that second call shouldn't strand
     * the freshly-created admin without a session.
     */
    suspend fun setup(serverUrl: String, username: String, password: String, requireAuth: Boolean): Result<Unit> {
        credentialStore.setServerUrl(serverUrl)

        val result = safeApiCall { api.setup(SetupRequestDto(username, password)) }
            .map { response ->
                credentialStore.saveToken(token = response.access_token, tokenType = response.token_type)
                sessionManager.onLoginSuccess()
            }

        if (result.isSuccess) {
            runCatching {
                val current = api.getConfig()
                api.updateConfig(current.copy(auth_enabled = requireAuth))
            }
        }
        return result
    }

    /** Issue #11: polled before rendering the authenticated app shell. */
    suspend fun isDatabaseReady(): Result<Boolean> = safeApiCall { api.getDbStatus() }.map { it.ready }

    /** Issue #10: current user's identity, driving admin-only nav visibility. */
    suspend fun getCurrentUser(): Result<CurrentUser> = safeApiCall { api.getCurrentUser() }.map { it.toDomain() }

    /**
     * Issue #10: manual Logout action. Always clears the local session even if the remote call
     * fails (e.g. offline) — there is no useful "undo logout" state to preserve.
     */
    suspend fun logout() {
        runCatching { api.logout() }
        sessionManager.onUnauthorized()
    }
}
