package com.derekwinters.chores.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which form [AuthGateScreen] should render (issue #11). */
sealed interface AuthGateState {
    data object EnterServerUrl : AuthGateState
    data object Checking : AuthGateState
    data class ShowSetup(val serverUrl: String) : AuthGateState
    data class ShowLogin(val serverUrl: String) : AuthGateState
    data class Error(val message: String) : AuthGateState
}

/**
 * Issue #11 behavior: "First-run setup: if the backend reports setup_needed, show a
 * 'Create Admin Account' flow instead of Login" — this is the pre-check that decides which one
 * to show, since neither is known until the user has entered a server URL.
 */
@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthGateState>(AuthGateState.EnterServerUrl)
    val state: StateFlow<AuthGateState> = _state.asStateFlow()

    fun checkServer(serverUrl: String) {
        val trimmed = serverUrl.trim()
        _state.value = AuthGateState.Checking
        viewModelScope.launch {
            authRepository.isSetupNeeded(trimmed)
                .onSuccess { setupNeeded ->
                    _state.value = if (setupNeeded) {
                        AuthGateState.ShowSetup(trimmed)
                    } else {
                        AuthGateState.ShowLogin(trimmed)
                    }
                }
                .onFailure { error -> _state.value = AuthGateState.Error(errorMessage(error)) }
        }
    }

    fun retry() {
        _state.value = AuthGateState.EnterServerUrl
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
