package com.derekwinters.chores.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.ConfigRepository
import com.derekwinters.chores.data.repository.DataRepository
import com.derekwinters.chores.data.repository.ImportSummary
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject

/** Issue #22: client-side preview counts shown before the "this replaces all existing data" confirm. */
data class ImportPreview(val peopleCount: Int, val choresCount: Int, val settingsCount: Int, val rawJson: String)

private val previewJson = Json { ignoreUnknownKeys = true }

/**
 * Issue #22 behaviors: "Export: download a full backup ... as a timestamped JSON file",
 * "Import: pick a .json file, show a confirmation summary ... then submit; report imported
 * counts on success", and log retention (a field on the shared config, see issue #20).
 */
@HiltViewModel
class DataSettingsViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _logRetentionDays = MutableStateFlow<Int?>(null)
    val logRetentionDays: StateFlow<Int?> = _logRetentionDays.asStateFlow()

    private val _exportState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val exportState: StateFlow<UiState<String>> = _exportState.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.getConfig().onSuccess { config -> _logRetentionDays.value = config.logRetentionDays }
        }
    }

    fun updateLogRetentionDays(days: Int) {
        _logRetentionDays.value = days
        viewModelScope.launch {
            configRepository.getConfig().onSuccess { config ->
                configRepository.updateConfig(config.copy(logRetentionDays = days))
            }
        }
    }

    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()

    private val _importState = MutableStateFlow<UiState<ImportSummary>>(UiState.Idle)
    val importState: StateFlow<UiState<ImportSummary>> = _importState.asStateFlow()

    fun exportConfig() {
        _exportState.value = UiState.Loading
        viewModelScope.launch {
            dataRepository.exportConfig()
                .onSuccess { json -> _exportState.value = UiState.Success(json) }
                .onFailure { error -> _exportState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun clearExportState() {
        _exportState.value = UiState.Idle
    }

    /** Issue #22: builds the pre-submit confirmation summary from the picked file's contents. */
    fun previewImport(json: String) {
        val counts = runCatching {
            val root = previewJson.parseToJsonElement(json).jsonObject
            Triple(
                (root["people"] as? JsonArray)?.size ?: 0,
                (root["chores"] as? JsonArray)?.size ?: 0,
                (root["settings"] as? JsonArray)?.size ?: 0
            )
        }.getOrElse { Triple(0, 0, 0) }

        _importPreview.value = ImportPreview(counts.first, counts.second, counts.third, json)
    }

    fun confirmImport() {
        val json = _importPreview.value?.rawJson ?: return
        _importState.value = UiState.Loading
        viewModelScope.launch {
            dataRepository.importConfig(json)
                .onSuccess { summary ->
                    _importState.value = UiState.Success(summary)
                    _importPreview.value = null
                }
                .onFailure { error -> _importState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun cancelImport() {
        _importPreview.value = null
    }

    fun clearImportState() {
        _importState.value = UiState.Idle
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
