package com.derekwinters.chores.ui.chores

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.ChoreRepository
import com.derekwinters.chores.data.repository.PeopleRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #16: the chore create/edit form. Edit mode is driven by the `choreId` nav arg (set on
 * the "chores/{choreId}/edit" route, absent on "chores/new"); since there's no
 * `GET /chores/{id}` endpoint, edit mode re-fetches the full list and finds the matching chore
 * client-side (same list the Chores screen already has cached, just not shared across
 * ViewModels).
 */
@HiltViewModel
class ChoreFormViewModel @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val peopleRepository: PeopleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val choreId: Int? = savedStateHandle.get<Int>("choreId")
    val isEditMode: Boolean get() = choreId != null

    private val _formState = MutableStateFlow(ChoreFormState())
    val formState: StateFlow<ChoreFormState> = _formState.asStateFlow()

    private val _availablePeople = MutableStateFlow<List<String>>(emptyList())
    val availablePeople: StateFlow<List<String>> = _availablePeople.asStateFlow()

    private val _loadState = MutableStateFlow<UiState<Unit>>(if (isEditMode) UiState.Loading else UiState.Success(Unit))
    val loadState: StateFlow<UiState<Unit>> = _loadState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState: StateFlow<UiState<Unit>> = _saveState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            peopleRepository.getPeople().onSuccess { people -> _availablePeople.value = people.map { it.username } }

            val id = choreId
            if (id != null) {
                choreRepository.getChores()
                    .onSuccess { chores ->
                        val chore = chores.find { it.id == id }
                        if (chore != null) {
                            _formState.value = chore.toFormState()
                            _loadState.value = UiState.Success(Unit)
                        } else {
                            _loadState.value = UiState.Error("Chore not found")
                        }
                    }
                    .onFailure { error -> _loadState.value = UiState.Error(errorMessage(error)) }
            }
        }
    }

    fun updateForm(update: (ChoreFormState) -> ChoreFormState) {
        _formState.value = update(_formState.value)
    }

    fun save() {
        val state = _formState.value
        val errors = state.validate()
        if (errors.isNotEmpty()) {
            _saveState.value = UiState.Error(errors.joinToString(". "))
            return
        }

        _saveState.value = UiState.Loading
        viewModelScope.launch {
            val result = choreId?.let { choreRepository.updateChore(it, state.toDraft()) }
                ?: choreRepository.createChore(state.toDraft())

            result
                .onSuccess { _saveState.value = UiState.Success(Unit) }
                .onFailure { error -> _saveState.value = UiState.Error(errorMessage(error)) }
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
