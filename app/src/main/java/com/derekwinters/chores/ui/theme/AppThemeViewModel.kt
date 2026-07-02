package com.derekwinters.chores.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #25 behavior: "Theme resolution order on app start: personal theme (if set) → household
 * default → hardcoded fallback (used if the initial theme fetch fails, e.g. offline)". Scoped to
 * the authenticated part of the composable tree (see ChoresApp), so it re-resolves on every
 * login and can be refreshed after issue #24/#25's screens change the effective theme.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    /** Null means "hardcoded fallback" — [ChoresTheme] treats null as MaterialTheme's own default. */
    private val _currentTheme = MutableStateFlow<ThemeOption?>(null)
    val currentTheme: StateFlow<ThemeOption?> = _currentTheme.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            themeRepository.getCurrentTheme()
                .onSuccess { current -> _currentTheme.value = current.theme }
                .onFailure { _currentTheme.value = null }
        }
    }
}
