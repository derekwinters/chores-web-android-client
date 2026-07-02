package com.derekwinters.chores.ui.log

import androidx.lifecycle.SavedStateHandle
import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.LogEntryDto
import com.derekwinters.chores.data.repository.LogRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #19 behaviors: filters (incl. deep-linked chore/person), pagination. The backend's
 * `GET /v1/log` returns a bare array (no server-side pagination), so paging here is entirely
 * client-side over the full filtered result set.
 */
class ActivityLogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun logEntry(id: Int, action: String = "completed", choreName: String = "Dishes") = LogEntryDto(
        id = id,
        chore_id = id,
        chore_name = choreName,
        person = "alice",
        action = action,
        timestamp = "t"
    )

    @Test
    fun init_seedsFiltersFromNavArgs() {
        val api = FakeChoresApi()
        val viewModel = ActivityLogViewModel(LogRepository(api), SavedStateHandle(mapOf("chore" to "Dishes")))

        assertEquals("Dishes", viewModel.filters.value.chore)
    }

    @Test
    fun load_success_populatesEntriesAndTotal() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(logResult = listOf(logEntry(id = 1)))
        val viewModel = ActivityLogViewModel(LogRepository(api), SavedStateHandle())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(1, (state as UiState.Success).data.total)
        assertEquals(1, state.data.entries.size)
    }

    @Test
    fun nextPage_thenPreviousPage_tracksPageNumber() = runTest(mainDispatcherRule.testDispatcher) {
        // 25 entries at PAGE_SIZE 20 spans two pages.
        val api = FakeChoresApi(logResult = (1..25).map { logEntry(id = it) })
        val viewModel = ActivityLogViewModel(LogRepository(api), SavedStateHandle())
        advanceUntilIdle()

        viewModel.nextPage()
        assertEquals(2, (viewModel.uiState.value as UiState.Success).data.page)
        assertEquals(5, (viewModel.uiState.value as UiState.Success).data.entries.size)

        viewModel.previousPage()
        assertEquals(1, (viewModel.uiState.value as UiState.Success).data.page)
        assertEquals(20, (viewModel.uiState.value as UiState.Success).data.entries.size)
    }

    @Test
    fun nextPage_onLastPage_isNoOp() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(logResult = listOf(logEntry(id = 1)))
        val viewModel = ActivityLogViewModel(LogRepository(api), SavedStateHandle())
        advanceUntilIdle()

        viewModel.nextPage()

        assertEquals(1, (viewModel.uiState.value as UiState.Success).data.page)
    }

    @Test
    fun updateFilters_resetsToFirstPage() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(logResult = (1..25).map { logEntry(id = it) })
        val viewModel = ActivityLogViewModel(LogRepository(api), SavedStateHandle())
        advanceUntilIdle()
        viewModel.nextPage()
        assertEquals(2, (viewModel.uiState.value as UiState.Success).data.page)

        viewModel.updateFilters(LogFilters(action = "completed"))
        advanceUntilIdle()

        assertEquals(1, (viewModel.uiState.value as UiState.Success).data.page)
        assertEquals("completed", viewModel.filters.value.action)
    }
}
