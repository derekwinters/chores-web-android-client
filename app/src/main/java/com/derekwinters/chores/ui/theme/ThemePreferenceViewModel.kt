package com.derekwinters.chores.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.CurrentTheme
import com.derekwinters.chores.data.model.ThemeDefaultInfo
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
 * Issue #25: everything the Preferences screen needs — all themes, the resolved current one, and
 * the true household default's id/name (from the non-admin-gated `/v1/theme/default-info`),
 * which is what lets the "Default (name)" tile be labeled/highlighted correctly even while
 * [current] reflects a *different* theme due to a personal override.
 */
data class ThemePreferenceData(
    val themes: List<ThemeOption>,
    val current: CurrentTheme,
    val defaultInfo: ThemeDefaultInfo
)

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
            val current = themeRepository.getCurrentTheme().getOrElse { error ->
                _uiState.value = UiState.Error(errorMessage(error))
                return@launch
            }
            themeRepository.getDefaultThemeInfo()
                .onSuccess { defaultInfo -> _uiState.value = UiState.Success(ThemePreferenceData(themes, current, defaultInfo)) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** [themeId] null selects "Default (household theme)", clearing the personal override. */
    fun selectTheme(themeId: String?) {
        viewModelScope.launch {
            themeRepository.setPersonalTheme(themeId).onSuccess { load() }
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
