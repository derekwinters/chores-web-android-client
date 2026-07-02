package com.derekwinters.chores.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.AuthLogEntry
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.AuthLogRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Issue #21: Auth Event Log filters (username, action, date range). */
data class AuthLogFilters(
    val username: String? = null,
    val action: String? = null,
    val start: String? = null,
    val end: String? = null
)

/**
 * Issue #21: one page of Auth Event Log results plus paging info. The backend's `GET /v1/auth/log`
 * returns a bare (unpaginated) array, so [total]/[page]/[totalPages] are all computed client-side
 * over the full filtered result set, matching ActivityLogViewModel's pattern.
 */
data class AuthLogPageState(
    val entries: List<AuthLogEntry>,
    val total: Int,
    val page: Int,
    val totalPages: Int = 1
)

/**
 * Issue #21 behavior: separate admin-only audit log for auth-related events (login_succeeded,
 * login_failed, password_changed, password_reset, user_created), distinct from the chore
 * Activity Log.
 */
@HiltViewModel
class AuthLogViewModel @Inject constructor(
    private val authLogRepository: AuthLogRepository
) : ViewModel() {

    private val _filters = MutableStateFlow(AuthLogFilters())
    val filters: StateFlow<AuthLogFilters> = _filters.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<AuthLogPageState>>(UiState.Loading)
    val uiState: StateFlow<UiState<AuthLogPageState>> = _uiState.asStateFlow()

    /** The full filtered result set from the last network fetch; paging slices this in memory. */
    private var allEntries: List<AuthLogEntry> = emptyList()
    private var currentPage = 1

    init {
        load()
    }

    fun updateFilters(filters: AuthLogFilters) {
        _filters.value = filters
        currentPage = 1
        load()
    }

    /** Client-side paging (issue #21) — no re-fetch needed, [allEntries] already holds everything. */
    fun nextPage() {
        if (currentPage < totalPages()) {
            currentPage += 1
            emitPage()
        }
    }

    fun previousPage() {
        if (currentPage > 1) {
            currentPage -= 1
            emitPage()
        }
    }

    private fun load() {
        _uiState.value = UiState.Loading
        val filters = _filters.value
        viewModelScope.launch {
            authLogRepository.getAuthLog(filters.username, filters.action, filters.start, filters.end)
                .onSuccess { entries ->
                    allEntries = entries
                    emitPage()
                }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    private fun totalPages(): Int = maxOf(1, ceil(allEntries.size / PAGE_SIZE.toDouble()).toInt())

    private fun emitPage() {
        val fromIndex = (currentPage - 1) * PAGE_SIZE
        val pageEntries = if (fromIndex >= allEntries.size) {
            emptyList()
        } else {
            allEntries.subList(fromIndex, minOf(fromIndex + PAGE_SIZE, allEntries.size))
        }
        _uiState.value = UiState.Success(
            AuthLogPageState(pageEntries, allEntries.size, currentPage, totalPages())
        )
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR

    private companion object {
        const val PAGE_SIZE = 20
    }
}
