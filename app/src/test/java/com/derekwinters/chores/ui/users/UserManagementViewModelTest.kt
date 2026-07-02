package com.derekwinters.chores.ui.users

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.PersonDto
import com.derekwinters.chores.data.repository.PeopleRepository
import com.derekwinters.chores.ui.UiState
import java.io.IOException
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Issue #18 behaviors: list, create, edit, delete household members. */
class UserManagementViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_success_populatesPeopleList() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(peopleResult = listOf(PersonDto(1, "alice", "Alice", is_admin = true)))
        val viewModel = UserManagementViewModel(PeopleRepository(api))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(1, (state as UiState.Success).data.size)
    }

    @Test
    fun createUser_success_reloadsList() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(createPersonResult = PersonDto(2, "bob", "Bob"))
        val viewModel = UserManagementViewModel(PeopleRepository(api))
        advanceUntilIdle()

        viewModel.createUser("Bob", "secret123")
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.actionState.value)
        assertEquals("Bob", api.lastCreatePersonRequest?.display_name)
    }

    @Test
    fun deleteUser_success_reloadsList() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(peopleResult = listOf(PersonDto(1, "alice", "Alice")))
        val viewModel = UserManagementViewModel(PeopleRepository(api))
        advanceUntilIdle()

        viewModel.deleteUser(1)
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.actionState.value)
        assertEquals(1, api.lastDeletePersonId)
    }

    @Test
    fun updateUser_failure_setsErrorState() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(updatePersonError = IOException("boom"))
        val viewModel = UserManagementViewModel(PeopleRepository(api))
        advanceUntilIdle()

        viewModel.updateUser(1, "Alice", "alice", 12, 50, "", null)
        advanceUntilIdle()

        assertTrue(viewModel.actionState.value is UiState.Error)
    }
}
