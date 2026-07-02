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
 * Issue #23 behaviors: paginated (20/page) Points Log admin table with inline edit (person and/
 * or points) and delete (server reverses the points on the person, floored at 0).
 */
@HiltViewModel
class PointsLogViewModel @Inject constructor(
    private val pointsLogRepository: PointsLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<PointsLogPage>>(UiState.Loading)
    val uiState: StateFlow<UiState<PointsLogPage>> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    private var currentPage = 1

    init {
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
        viewModelScope.launch {
            pointsLogRepository.getPointsLog(currentPage)
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
