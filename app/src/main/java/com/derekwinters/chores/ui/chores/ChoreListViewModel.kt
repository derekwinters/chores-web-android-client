package com.derekwinters.chores.ui.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.ChoreRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Issue #5 behaviors: "Chore list screen: GET /chores ..." and "Sealed UiState + StateFlow
 * pattern for ChoreListViewModel". Issue #13 adds live search/filters/sorting; issue #12's
 * Dashboard deep links pre-seed [filters] via [applyInitialFilters].
 */
@HiltViewModel
class ChoreListViewModel @Inject constructor(
    private val choreRepository: ChoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Chore>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Chore>>> = _uiState.asStateFlow()

    /** Tracks which chore is mid-completion so its row can show a progress indicator. */
    private val _completingChoreId = MutableStateFlow<Int?>(null)
    val completingChoreId: StateFlow<Int?> = _completingChoreId.asStateFlow()

    /** Issue #15: tracks which chore is mid Skip/Mark-Due-Now/Delete for its own progress state. */
    private val _pendingActionChoreId = MutableStateFlow<Int?>(null)
    val pendingActionChoreId: StateFlow<Int?> = _pendingActionChoreId.asStateFlow()

    private val _filters = MutableStateFlow(ChoreFilters())
    val filters: StateFlow<ChoreFilters> = _filters.asStateFlow()

    /** Issue #13: the filtered + sorted view of [uiState], recomputed whenever either changes. */
    val visibleChores: StateFlow<UiState<List<Chore>>> = combine(_uiState, _filters) { state, filters ->
        when (state) {
            is UiState.Success -> UiState.Success(state.data.applyFilters(filters).sortedForChoresScreen())
            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    init {
        loadChores()
    }

    fun loadChores() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            choreRepository.getChores()
                .onSuccess { chores -> _uiState.value = UiState.Success(chores) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun updateFilters(filters: ChoreFilters) {
        _filters.value = filters
    }

    fun updateQuery(query: String) {
        _filters.value = _filters.value.copy(query = query)
    }

    fun clearFilters() {
        _filters.value = ChoreFilters()
    }

    /** Issue #12: seeds the assignee/due-within filters from a Dashboard deep link. */
    fun applyInitialFilters(assignee: String?, dueWithin: DueWithinFilter?) {
        _filters.value = _filters.value.copy(
            assignees = assignee?.let { setOf(it) } ?: _filters.value.assignees,
            dueWithin = dueWithin ?: _filters.value.dueWithin
        )
    }

    /**
     * Completes [chore]. [completedBy] must be provided (from the Completer-picker dialog) when
     * [Chore.needsCompleterSelection] is true; the caller is responsible for showing that dialog
     * before calling this for such chores.
     */
    fun completeChore(chore: Chore, completedBy: String? = null) {
        _completingChoreId.value = chore.id
        viewModelScope.launch {
            choreRepository.completeChore(chore.id, completedBy)
                .onSuccess { loadChores() }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
            _completingChoreId.value = null
        }
    }

    /** Issue #15: "Skip (only when due) ... moves to next cycle without awarding points". */
    fun skipChore(chore: Chore) {
        _pendingActionChoreId.value = chore.id
        viewModelScope.launch {
            choreRepository.skipChore(chore.id)
                .onSuccess { loadChores() }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
            _pendingActionChoreId.value = null
        }
    }

    /** Issue #15: "Mark Due Now (only when not due)". */
    fun markChoreDue(chore: Chore) {
        _pendingActionChoreId.value = chore.id
        viewModelScope.launch {
            choreRepository.markChoreDue(chore.id)
                .onSuccess { loadChores() }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
            _pendingActionChoreId.value = null
        }
    }

    /** Issue #15: "also removes all points history for this chore and cannot be undone". */
    fun deleteChore(chore: Chore) {
        _pendingActionChoreId.value = chore.id
        viewModelScope.launch {
            choreRepository.deleteChore(chore.id)
                .onSuccess { loadChores() }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
            _pendingActionChoreId.value = null
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
