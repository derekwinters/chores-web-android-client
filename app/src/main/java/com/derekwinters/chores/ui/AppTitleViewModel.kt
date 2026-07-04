package com.derekwinters.chores.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #58: fetches the household's configurable `config.title` (defaults to "Family Chores")
 * for the nav-shell branding shown in [ChoresAppContent]'s `TopAppBar`, matching web's
 * `.app-title`/`.topnav-title`.
 *
 * Null means "not yet loaded / fetch failed" — the TopAppBar falls back to `R.string.app_name`.
 */
@HiltViewModel
class AppTitleViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _appTitle = MutableStateFlow<String?>(null)
    val appTitle: StateFlow<String?> = _appTitle.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            configRepository.getConfig()
                .onSuccess { config -> _appTitle.value = config.appTitle }
                .onFailure { _appTitle.value = null }
        }
    }
}
