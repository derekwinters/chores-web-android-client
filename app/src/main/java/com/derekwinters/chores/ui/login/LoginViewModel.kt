package com.derekwinters.chores.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.auth.AuthRepository
import com.derekwinters.chores.common.UiState
import com.derekwinters.chores.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Login screen state, using the same [UiState] pattern as [com.derekwinters.chores.ui.chores.ChoreListViewModel].
 * `null` represents the initial/idle state, before the user has submitted the form.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>?>(null)
    val uiState: StateFlow<UiState<Unit>?> = _uiState.asStateFlow()

    fun login(serverUrl: String, username: String, password: String) {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.value = UiState.Error("Server URL, username, and password are all required")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = try {
                authRepository.login(serverUrl.trim(), username.trim(), password)
                UiState.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }
}
