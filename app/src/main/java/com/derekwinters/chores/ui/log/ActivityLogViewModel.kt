package com.derekwinters.chores.ui.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.LogRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Issue #19: Activity Log filters — person/chore incl. deep links, action type, date range. */
data class LogFilters(
    val person: String? = null,
    val chore: String? = null,
    val action: String? = null,
    val start: String? = null,
    val end: String? = null
)

/** Issue #19: one page of results plus the filters/page that produced it, for the "N results" UI. */
data class ActivityLogPageState(
    val entries: List<LogEntry>,
    val total: Int,
    val page: Int
)

/**
 * Issue #19 behaviors: filters (person incl. synthetic "system"/"schedule" actors, chore, action
 * type, date range), pagination, and support for being deep-linked with a pre-set chore or person
 * filter (Chore card History action, User Detail activity link).
 */
@HiltViewModel
class ActivityLogViewModel @Inject constructor(
    private val logRepository: LogRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _filters = MutableStateFlow(
        LogFilters(
            person = savedStateHandle.get<String>("person"),
            chore = savedStateHandle.get<String>("chore")
        )
    )
    val filters: StateFlow<LogFilters> = _filters.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<ActivityLogPageState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ActivityLogPageState>> = _uiState.asStateFlow()

    private var currentPage = 1

    init {
        load()
    }

    fun updateFilters(filters: LogFilters) {
        _filters.value = filters
        currentPage = 1
        load()
    }

    fun nextPage() {
        currentPage += 1
        load()
    }

    fun previousPage() {
        if (currentPage > 1) {
            currentPage -= 1
            load()
        }
    }

    private fun load() {
        _uiState.value = UiState.Loading
        val filters = _filters.value
        viewModelScope.launch {
            logRepository.getLog(
                person = filters.person,
                chore = filters.chore,
                action = filters.action,
                start = filters.start,
                end = filters.end,
                page = currentPage
            ).onSuccess { page ->
                _uiState.value = UiState.Success(ActivityLogPageState(page.entries, page.total, currentPage))
            }.onFailure { error ->
                _uiState.value = UiState.Error(errorMessage(error))
            }
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
