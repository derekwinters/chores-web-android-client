package com.derekwinters.chores.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.model.UpdateCheckStatus
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
 * Issue #20 behaviors: General/Auth/Chores/About settings forms. General/Auth/Chores read/write
 * the shared `GET/PUT /v1/config` endpoint (`AppConfig`); About's version info comes from the
 * separate `/v1/config/updates/status` and `/v1/config/updates/check` endpoints
 * (`UpdateCheckStatus` isn't part of the config resource on the backend). Issue #21/#22 add nav
 * entries (Auth Event Log, Data settings) from within the Auth/Chores sections respectively.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AppConfig>>(UiState.Loading)
    val uiState: StateFlow<UiState<AppConfig>> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState: StateFlow<UiState<Unit>> = _saveState.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateCheckStatus?>(null)
    val updateStatus: StateFlow<UpdateCheckStatus?> = _updateStatus.asStateFlow()

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
        viewModelScope.launch {
            configRepository.getUpdateCheckStatus().onSuccess { status -> _updateStatus.value = status }
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
            configRepository.checkForUpdates().onSuccess { status -> _updateStatus.value = status }
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
