package com.derekwinters.chores.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.AuthRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #11 behavior: "First-run setup: if the backend reports setup_needed, show a 'Create
 * Admin Account' flow ... Creates the first user as admin, then sets auth_enabled via config."
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    fun createAdminAccount(serverUrl: String, username: String, password: String, requireAuth: Boolean) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.setup(serverUrl.trim(), username.trim(), password, requireAuth)
                .onSuccess { _uiState.value = UiState.Success(Unit) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
