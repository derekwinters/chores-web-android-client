package com.derekwinters.chores.ui.log

import androidx.lifecycle.SavedStateHandle
import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.LogEntryDto
import com.derekwinters.chores.data.network.dto.LogPageDto
import com.derekwinters.chores.data.repository.LogRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Issue #19 behaviors: filters (incl. deep-linked chore/person), pagination. */
class ActivityLogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_seedsFiltersFromNavArgs() {
        val api = FakeChoresApi()
        val viewModel = ActivityLogViewModel(LogRepository(api), SavedStateHandle(mapOf("chore" to "Dishes")))

        assertEquals("Dishes", viewModel.filters.value.chore)
    }

    @Test
    fun load_success_populatesEntriesAndTotal() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            logResult = LogPageDto(
                items = listOf(LogEntryDto(id = 1, timestamp = "t", target_type = "chore", action = "completed", actor = "alice", target_name = "Dishes")),
                total = 5
            )
        )
        val viewModel = ActivityLogViewModel(LogRepository(api), SavedStateHandle())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(5, (state as UiState.Success).data.total)
        assertEquals(1, state.data.entries.size)
    }

    @Test
    fun nextPage_thenPreviousPage_tracksPageNumber() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(logResult = LogPageDto(items = emptyList(), total = 0))
        val viewModel = ActivityLogViewModel(LogRepository(api), SavedStateHandle())
        advanceUntilIdle()

        viewModel.nextPage()
        advanceUntilIdle()
        assertEquals(2, (viewModel.uiState.value as UiState.Success).data.page)

        viewModel.previousPage()
        advanceUntilIdle()
        assertEquals(1, (viewModel.uiState.value as UiState.Success).data.page)
    }

    @Test
    fun updateFilters_resetsToFirstPage() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(logResult = LogPageDto(items = emptyList(), total = 0))
        val viewModel = ActivityLogViewModel(LogRepository(api), SavedStateHandle())
        advanceUntilIdle()
        viewModel.nextPage()
        advanceUntilIdle()

        viewModel.updateFilters(LogFilters(action = "completed"))
        advanceUntilIdle()

        assertEquals(1, (viewModel.uiState.value as UiState.Success).data.page)
        assertEquals("completed", viewModel.filters.value.action)
    }
}
