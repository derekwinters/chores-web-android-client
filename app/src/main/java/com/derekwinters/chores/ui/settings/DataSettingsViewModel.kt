package com.derekwinters.chores.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.DataRepository
import com.derekwinters.chores.data.repository.ImportSummary
import com.derekwinters.chores.data.repository.LogRepository
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
 * counts on success", and the log-retention setting.
 *
 * The log-retention setting was originally modeled as a field on the shared config (issue #20's
 * `log_retention_days`), and a since-corrected assumption treated it as local-only UI state with
 * no backend to persist to. The real backend has a dedicated `GET`/`POST /v1/log/retention`
 * endpoint (`RetentionSettings { retention_days }`) — a real, separate resource, not part of
 * `ConfigOut`/`ConfigUpdate` — so [logRetentionDays] now round-trips through [logRepository].
 */
@HiltViewModel
class DataSettingsViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val logRepository: LogRepository
) : ViewModel() {

    private val _logRetentionDays = MutableStateFlow<Int?>(null)
    val logRetentionDays: StateFlow<Int?> = _logRetentionDays.asStateFlow()

    private val _logRetentionInput = MutableStateFlow<String>("")
    val logRetentionInput: StateFlow<String> = _logRetentionInput.asStateFlow()

    private val _logRetentionState = MutableStateFlow<UiState<Int>>(UiState.Idle)
    val logRetentionState: StateFlow<UiState<Int>> = _logRetentionState.asStateFlow()

    private val _exportState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val exportState: StateFlow<UiState<String>> = _exportState.asStateFlow()

    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()

    private val _importState = MutableStateFlow<UiState<ImportSummary>>(UiState.Idle)
    val importState: StateFlow<UiState<ImportSummary>> = _importState.asStateFlow()

    private val _selectedImportFilename = MutableStateFlow<String?>(null)
    val selectedImportFilename: StateFlow<String?> = _selectedImportFilename.asStateFlow()

    private val _exportFilename = MutableStateFlow<String?>(null)
    val exportFilename: StateFlow<String?> = _exportFilename.asStateFlow()

    init {
        loadRetention()
    }

    private fun loadRetention() {
        viewModelScope.launch {
            logRepository.getRetentionDays().onSuccess { days ->
                _logRetentionDays.value = days
                _logRetentionInput.value = days.toString()
            }
        }
    }

    fun updateLogRetentionInput(input: String) {
        _logRetentionInput.value = input
    }

    fun saveLogRetentionDays() {
        val days = _logRetentionInput.value.toIntOrNull() ?: return
        _logRetentionState.value = UiState.Loading
        viewModelScope.launch {
            logRepository.setRetentionDays(days)
                .onSuccess { saved ->
                    _logRetentionDays.value = saved
                    _logRetentionInput.value = saved.toString()
                    _logRetentionState.value = UiState.Success(saved)
                }
                .onFailure { error ->
                    _logRetentionState.value = UiState.Error(errorMessage(error))
                }
        }
    }

    fun clearLogRetentionState() {
        _logRetentionState.value = UiState.Idle
    }

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

    fun setSelectedImportFilename(filename: String?) {
        _selectedImportFilename.value = filename
    }

    fun setExportFilename(filename: String?) {
        _exportFilename.value = filename
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
