package com.derekwinters.chores.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.Person
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.PeopleRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #18: admin-only household member management — list (grouped Administrators/Members),
 * create, edit, delete.
 */
@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Person>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Person>>> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            peopleRepository.getPeople()
                .onSuccess { people -> _uiState.value = UiState.Success(people) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** Issue #18: "username auto-derived from the name ... no separate username field". */
    fun createUser(displayName: String, password: String) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            peopleRepository.createPerson(displayName, password)
                .onSuccess {
                    _actionState.value = UiState.Success(Unit)
                    load()
                }
                .onFailure { error -> _actionState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** [password] blank means "unchanged". [isAdmin] should be null (unchanged) when disabled by the UI. */
    fun updateUser(
        personId: Int,
        displayName: String,
        username: String,
        goal7d: Int,
        goal30d: Int,
        password: String,
        isAdmin: Boolean?
    ) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            peopleRepository.updatePerson(personId, displayName, username, goal7d, goal30d, password, isAdmin)
                .onSuccess {
                    _actionState.value = UiState.Success(Unit)
                    load()
                }
                .onFailure { error -> _actionState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** Issue #18: "history/points/log entries are not cascade-deleted". */
    fun deleteUser(personId: Int) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            peopleRepository.deletePerson(personId)
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
