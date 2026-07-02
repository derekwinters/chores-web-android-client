package com.derekwinters.chores.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.CurrentUser
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #10 behavior: "Determine the current user's admin status (GET /auth/me ... unused
 * today) to drive admin-only nav visibility". Scoped to the authenticated part of the composable
 * tree (see ChoresApp), so a fresh fetch runs on every login.
 */
@HiltViewModel
class CurrentUserViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<CurrentUser>>(UiState.Loading)
    val uiState: StateFlow<UiState<CurrentUser>> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.getCurrentUser()
                .onSuccess { user -> _uiState.value = UiState.Success(user) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
