package com.derekwinters.chores.ui.settings

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.AuthLogEntryDto
import com.derekwinters.chores.data.network.dto.AuthLogPageDto
import com.derekwinters.chores.data.repository.AuthLogRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Issue #21 behaviors: username/action/date filters, pagination. */
class AuthLogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_populatesEntries() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            authLogResult = AuthLogPageDto(
                items = listOf(AuthLogEntryDto(id = 1, timestamp = "t", username = "alice", action = "login_succeeded")),
                total = 1
            )
        )
        val viewModel = AuthLogViewModel(AuthLogRepository(api))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(1, (state as UiState.Success).data.total)
    }

    @Test
    fun updateFilters_resetsPageAndAppliesUsernameFilter() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(authLogResult = AuthLogPageDto())
        val viewModel = AuthLogViewModel(AuthLogRepository(api))
        advanceUntilIdle()
        viewModel.nextPage()
        advanceUntilIdle()

        viewModel.updateFilters(AuthLogFilters(username = "alice"))
        advanceUntilIdle()

        assertEquals(1, (viewModel.uiState.value as UiState.Success).data.page)
        assertEquals("alice", viewModel.filters.value.username)
    }
}
