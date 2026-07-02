package com.derekwinters.chores.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.AuthRepository
import com.derekwinters.chores.data.repository.LoginOutcome
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #5 behavior: "Login screen ... calls POST /auth/login, persists token + URL". Issue #11
 * extends this with the forced-password-reset flow: a 403 login response carrying a reset token
 * surfaces via [resetRequired] instead of [uiState] going to Error, so the screen can show a
 * "set new password" form instead of a dead-end error message.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    private val _resetRequired = MutableStateFlow<String?>(null)
    val resetRequired: StateFlow<String?> = _resetRequired.asStateFlow()

    private var pendingServerUrl: String = ""
    private var pendingUsername: String = ""

    fun login(serverUrl: String, username: String, password: String) {
        _uiState.value = UiState.Loading
        pendingServerUrl = serverUrl.trim()
        pendingUsername = username.trim()
        viewModelScope.launch {
            authRepository.loginWithResetSupport(pendingServerUrl, pendingUsername, password)
                .onSuccess { outcome ->
                    when (outcome) {
                        LoginOutcome.Success -> _uiState.value = UiState.Success(Unit)
                        is LoginOutcome.ResetRequired -> {
                            _resetRequired.value = outcome.resetToken
                            _uiState.value = UiState.Idle
                        }
                    }
                }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** Issue #11: submits the new password using the token captured by [login], then logs in. */
    fun submitPasswordReset(newPassword: String) {
        val token = _resetRequired.value ?: return
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.resetPasswordAndLogin(pendingServerUrl, token, pendingUsername, newPassword)
                .onSuccess {
                    _resetRequired.value = null
                    _uiState.value = UiState.Success(Unit)
                }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun cancelPasswordReset() {
        _resetRequired.value = null
        _uiState.value = UiState.Idle
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
