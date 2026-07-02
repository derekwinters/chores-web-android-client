package com.derekwinters.chores.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.ConfigRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #20 behaviors: General/Auth/Chores/About settings forms, all against the same
 * `GET/PUT /config` endpoint. Issue #21/#22 add nav entries (Auth Event Log, Data settings) from
 * within the Auth/Chores sections respectively.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AppConfig>>(UiState.Loading)
    val uiState: StateFlow<UiState<AppConfig>> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState: StateFlow<UiState<Unit>> = _saveState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            configRepository.getConfig()
                .onSuccess { config -> _uiState.value = UiState.Success(config) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun save(updated: AppConfig) {
        _saveState.value = UiState.Loading
        viewModelScope.launch {
            configRepository.updateConfig(updated)
                .onSuccess { config ->
                    _uiState.value = UiState.Success(config)
                    _saveState.value = UiState.Success(Unit)
                }
                .onFailure { error -> _saveState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** Issue #20: "About" tab's "Check Now" manual update check. */
    fun checkForUpdates() {
        viewModelScope.launch {
            configRepository.checkForUpdates().onSuccess { config -> _uiState.value = UiState.Success(config) }
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
