package com.derekwinters.chores.ui.settings

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.PointsLogEntryDto
import com.derekwinters.chores.data.network.dto.PointsLogPageDto
import com.derekwinters.chores.data.repository.PointsLogRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Issue #23 behaviors: paginated points-log table, inline edit, delete. */
class PointsLogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_populatesEntries() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            pointsLogResult = PointsLogPageDto(
                items = listOf(PointsLogEntryDto(id = 1, person = "alice", points = 5, chore_id = 4, completed_at = "2026-07-01")),
                total = 1,
                offset = 0,
                limit = 20
            )
        )
        val viewModel = PointsLogViewModel(PointsLogRepository(api))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(1, (state as UiState.Success).data.total)
        assertEquals(0, api.lastGetPointsLogOffset)
    }

    @Test
    fun nextPage_requestsOffsetPastCurrentPage() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            pointsLogResult = PointsLogPageDto(
                items = listOf(PointsLogEntryDto(id = 1, person = "alice", points = 5, chore_id = 4, completed_at = "2026-07-01")),
                total = 50,
                offset = 0,
                limit = 20
            )
        )
        val viewModel = PointsLogViewModel(PointsLogRepository(api))
        advanceUntilIdle()

        viewModel.nextPage()
        advanceUntilIdle()

        assertEquals(20, api.lastGetPointsLogOffset)
    }

    @Test
    fun nextPage_noOpPastLastPage() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            pointsLogResult = PointsLogPageDto(
                items = listOf(PointsLogEntryDto(id = 1, person = "alice", points = 5, chore_id = 4, completed_at = "2026-07-01")),
                total = 1,
                offset = 0,
                limit = 20
            )
        )
        val viewModel = PointsLogViewModel(PointsLogRepository(api))
        advanceUntilIdle()

        viewModel.nextPage()
        advanceUntilIdle()

        assertEquals(0, api.lastGetPointsLogOffset)
    }

    @Test
    fun updateEntry_success_reloadsList() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(updatePointsLogResult = PointsLogEntryDto(id = 1, person = "bob", points = 8, chore_id = 4, completed_at = "2026-07-01"))
        val viewModel = PointsLogViewModel(PointsLogRepository(api))
        advanceUntilIdle()

        viewModel.updateEntry(1, "bob", 8)
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.actionState.value)
        assertEquals(1, api.lastUpdatePointsLogEntryId)
        assertEquals("bob", api.lastUpdatePointsLogRequest?.person)
        assertEquals(8, api.lastUpdatePointsLogRequest?.points)
    }

    @Test
    fun deleteEntry_success_reloadsList() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi()
        val viewModel = PointsLogViewModel(PointsLogRepository(api))
        advanceUntilIdle()

        viewModel.deleteEntry(3)
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.actionState.value)
        assertEquals(3, api.lastDeletePointsLogEntryId)
    }
}
