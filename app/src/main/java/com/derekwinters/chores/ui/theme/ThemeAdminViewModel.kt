package com.derekwinters.chores.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.ThemeRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #24 behaviors: household default theme management — 6 built-in themes (protected:
 * can't rename/delete, only copy) plus custom themes (create via copy, edit 9 colors + name,
 * rename, delete non-built-ins), and setting the household default.
 */
@HiltViewModel
class ThemeAdminViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ThemeOption>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ThemeOption>>> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            themeRepository.getThemes()
                .onSuccess { themes -> _uiState.value = UiState.Success(themes) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun setDefaultTheme(themeId: Int) {
        viewModelScope.launch {
            themeRepository.setDefaultTheme(themeId).onSuccess { load() }
        }
    }

    fun createTheme(name: String, sourceThemeId: Int) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            themeRepository.createTheme(name, sourceThemeId)
                .onSuccess {
                    _actionState.value = UiState.Success(Unit)
                    load()
                }
                .onFailure { error -> _actionState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun updateTheme(theme: ThemeOption) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            themeRepository.updateTheme(
                themeId = theme.id,
                name = theme.name,
                background = theme.background,
                surface = theme.surface,
                surface2 = theme.surface2,
                accent = theme.accent,
                primary = theme.primary,
                secondary = theme.secondary,
                success = theme.success,
                warning = theme.warning,
                error = theme.error
            ).onSuccess {
                _actionState.value = UiState.Success(Unit)
                load()
            }.onFailure { error -> _actionState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** Non-built-in themes only; enforced server-side too. */
    fun deleteTheme(themeId: Int) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            themeRepository.deleteTheme(themeId)
                .onSuccess {
                    _actionState.value = UiState.Success(Unit)
                    load()
                }
                .onFailure { error -> _actionState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun clearActionState() {
        _actionState.value = UiState.Idle
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
