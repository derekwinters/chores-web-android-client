package com.derekwinters.chores.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.CurrentTheme
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

/** Issue #25: everything the Preferences screen needs — all themes plus the resolved current one. */
data class ThemePreferenceData(val themes: List<ThemeOption>, val current: CurrentTheme)

/**
 * Issue #25 behaviors: "Grid of 'Default (household theme name)' (clears personal override) plus
 * all available themes; tapping applies immediately, no separate save step."
 */
@HiltViewModel
class ThemePreferenceViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ThemePreferenceData>>(UiState.Loading)
    val uiState: StateFlow<UiState<ThemePreferenceData>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val themes = themeRepository.getThemes().getOrElse { error ->
                _uiState.value = UiState.Error(errorMessage(error))
                return@launch
            }
            themeRepository.getCurrentTheme()
                .onSuccess { current -> _uiState.value = UiState.Success(ThemePreferenceData(themes, current)) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** [themeId] null selects "Default (household theme)", clearing the personal override. */
    fun selectTheme(themeId: Int?) {
        viewModelScope.launch {
            themeRepository.setPersonalTheme(themeId).onSuccess { load() }
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
