package com.derekwinters.chores.ui.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.chores.Chore
import com.derekwinters.chores.chores.ChoreRepository
import com.derekwinters.chores.chores.Person
import com.derekwinters.chores.chores.PeopleRepository
import com.derekwinters.chores.common.UiState
import com.derekwinters.chores.common.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A chore awaiting a Completer pick (its `current_assignee` is null) and the people to choose from. */
data class PendingCompleterPick(val chore: Chore, val people: List<Person>)

/**
 * First ViewModel pattern in this codebase: sealed [UiState] over [kotlinx.coroutines.flow.StateFlow].
 * Loads all chores (read-only) and drives the complete-chore flow, including the Completer-picker
 * dialog required when a chore has no Assignee (`current_assignee == null`) — see chores-web's
 * CONTEXT.md and CompleteWithActorModal.jsx.
 */
@HiltViewModel
class ChoreListViewModel @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val peopleRepository: PeopleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Chore>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Chore>>> = _uiState.asStateFlow()

    private val _pendingCompleterPick = MutableStateFlow<PendingCompleterPick?>(null)
    val pendingCompleterPick: StateFlow<PendingCompleterPick?> = _pendingCompleterPick.asStateFlow()

    init {
        loadChores()
    }

    fun loadChores() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = try {
                UiState.Success(choreRepository.getChores())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }

    /**
     * Starts the complete-chore flow. If the chore has an Assignee, completes it immediately
     * (attributed to the logged-in user). Otherwise fetches the people list and surfaces
     * [pendingCompleterPick] so the UI can show the Completer-picker dialog.
     */
    fun onCompleteClicked(chore: Chore) {
        if (chore.currentAssignee != null) {
            completeChore(chore.id, completedBy = null)
            return
        }

        viewModelScope.launch {
            try {
                val people = peopleRepository.getPeople()
                _pendingCompleterPick.value = PendingCompleterPick(chore, people)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.toUserMessage())
            }
        }
    }

    /** Called when the user picks a Completer from the dialog for a null-Assignee chore. */
    fun onCompleterSelected(username: String) {
        val pending = _pendingCompleterPick.value ?: return
        _pendingCompleterPick.value = null
        completeChore(pending.chore.id, completedBy = username)
    }

    /** Called when the user dismisses the Completer-picker dialog without selecting anyone. */
    fun onCompleterPickCancelled() {
        _pendingCompleterPick.value = null
    }

    private fun completeChore(choreId: Int, completedBy: String?) {
        viewModelScope.launch {
            try {
                choreRepository.completeChore(choreId, completedBy)
                loadChores()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.toUserMessage())
            }
        }
    }
}
