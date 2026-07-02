package com.derekwinters.chores.ui.chores

import androidx.lifecycle.SavedStateHandle
import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.network.dto.PersonDto
import com.derekwinters.chores.data.repository.ChoreRepository
import com.derekwinters.chores.data.repository.PeopleRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #16 behaviors: create/edit form load + validation-gated save.
 */
class ChoreFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun createMode_startsWithBlankFormAndNotLoading() {
        val api = FakeChoresApi()
        val viewModel = ChoreFormViewModel(ChoreRepository(api), PeopleRepository(api), SavedStateHandle())

        assertEquals(false, viewModel.isEditMode)
        assertEquals(UiState.Success(Unit), viewModel.loadState.value)
    }

    @Test
    fun editMode_loadsMatchingChoreIntoFormState() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            choresResult = listOf(
                ChoreDto(id = 5, name = "Dishes", points = 8, state = "due", current_assignee = "alice", assignment_type = "fixed")
            )
        )
        val viewModel = ChoreFormViewModel(
            ChoreRepository(api),
            PeopleRepository(api),
            SavedStateHandle(mapOf("choreId" to 5))
        )
        advanceUntilIdle()

        assertTrue(viewModel.isEditMode)
        assertEquals(UiState.Success(Unit), viewModel.loadState.value)
        assertEquals("Dishes", viewModel.formState.value.name)
        assertEquals(8, viewModel.formState.value.points)
    }

    @Test
    fun save_invalidForm_setsErrorWithoutCallingRepository() {
        val api = FakeChoresApi()
        val viewModel = ChoreFormViewModel(ChoreRepository(api), PeopleRepository(api), SavedStateHandle())
        viewModel.updateForm { it.copy(name = "") }

        viewModel.save()

        assertTrue(viewModel.saveState.value is UiState.Error)
    }

    @Test
    fun save_validCreateForm_callsCreateChore() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            peopleResult = listOf(PersonDto(1, "alice", "Alice")),
            createChoreResult = ChoreDto(id = 1, name = "Dishes", points = 5, state = "due")
        )
        val viewModel = ChoreFormViewModel(ChoreRepository(api), PeopleRepository(api), SavedStateHandle())
        viewModel.updateForm {
            it.copy(name = "Dishes", assignmentType = AssignmentType.FIXED, assignee = "alice", weeklyDays = setOf(1))
        }

        viewModel.save()
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.saveState.value)
        assertEquals("Dishes", api.lastCreateChoreRequest?.name)
    }

    @Test
    fun save_validEditForm_callsUpdateChore() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            choresResult = listOf(
                ChoreDto(
                    id = 7,
                    name = "Trash",
                    points = 3,
                    state = "due",
                    current_assignee = "bob",
                    schedule_type = "weekly",
                    weekly_days = listOf(1)
                )
            ),
            updateChoreResult = ChoreDto(id = 7, name = "Trash", points = 3, state = "due")
        )
        val viewModel = ChoreFormViewModel(ChoreRepository(api), PeopleRepository(api), SavedStateHandle(mapOf("choreId" to 7)))
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.saveState.value)
        assertEquals(7, api.lastUpdateChoreId)
    }
}
