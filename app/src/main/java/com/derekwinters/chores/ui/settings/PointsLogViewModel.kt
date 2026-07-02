package com.derekwinters.chores.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.PointsLogPage
import com.derekwinters.chores.data.repository.PointsLogRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #23 behaviors: paginated Points Log admin table with inline edit (person and/or points)
 * and delete (server reverses the points on the person, floored at 0).
 *
 * The real backend paginates by `offset`/`limit`, not by a page number (there's no server-side
 * page count). The UI still presents Previous/Next controls, but [nextPage]/[previousPage]
 * compute the next `offset` from the most recently loaded [PointsLogPage]'s own `offset`/`limit`/
 * `total` rather than tracking a client-side page index — that keeps paging correct even if the
 * server's effective `limit` ever differs from what was requested.
 */
@HiltViewModel
class PointsLogViewModel @Inject constructor(
    private val pointsLogRepository: PointsLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<PointsLogPage>>(UiState.Loading)
    val uiState: StateFlow<UiState<PointsLogPage>> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    /** 0-based row offset to request on the next [load]; advanced/retreated by the page size. */
    private var currentOffset = 0

    init {
        load()
    }

    fun nextPage() {
        val page = currentPageOrNull() ?: return
        if (page.offset + page.limit >= page.total) return
        currentOffset = page.offset + page.limit
        load()
    }

    fun previousPage() {
        val page = currentPageOrNull() ?: return
        if (page.offset <= 0) return
        currentOffset = (page.offset - page.limit).coerceAtLeast(0)
        load()
    }

    private fun currentPageOrNull(): PointsLogPage? = (_uiState.value as? UiState.Success)?.data

    private fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            pointsLogRepository.getPointsLog(offset = currentOffset)
                .onSuccess { page -> _uiState.value = UiState.Success(page) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun updateEntry(entryId: Int, person: String, points: Int) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            pointsLogRepository.updateEntry(entryId, person, points)
                .onSuccess {
                    _actionState.value = UiState.Success(Unit)
                    load()
                }
                .onFailure { error -> _actionState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** "This will reverse the points on the person, floored at 0, and cannot be undone." */
    fun deleteEntry(entryId: Int) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            pointsLogRepository.deleteEntry(entryId)
                .onSuccess {
                    _actionState.value = UiState.Success(Unit)
                    load()
                }
                .onFailure { error -> _actionState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun clearActionState() {
        _actionState.value = UiState.Idle
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
