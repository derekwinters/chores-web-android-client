package com.derekwinters.chores.ui.chores

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.network.dto.PointsSummaryDto
import com.derekwinters.chores.data.repository.ChoreRepository
import com.derekwinters.chores.data.repository.PeopleRepository
import com.derekwinters.chores.ui.UiState
import java.time.LocalDate
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #14 behavior: "Total (enabled) Chores, Total Points ..., Completed Last 7 Days (sum from
 * points-summary), Due Next 7 Days (sum of points for enabled chores due within 7 days)".
 */
class ChoresStatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun chore(id: Int, points: Int, enabled: Boolean, nextDue: String?, state: String = "due") = ChoreDto(
        id = id,
        name = "Chore$id",
        points = points,
        state = state,
        next_due = nextDue,
        current_assignee = "alice",
        disabled = !enabled
    )

    @Test
    fun load_computesStatsFromEnabledChoresAndPointsSummary() = runTest(mainDispatcherRule.testDispatcher) {
        val today = LocalDate.now()
        val api = FakeChoresApi(
            choresResult = listOf(
                chore(1, points = 5, enabled = true, nextDue = today.plusDays(1).toString()), // within 7 days
                chore(2, points = 8, enabled = true, nextDue = today.plusDays(30).toString()), // outside 7 days
                chore(3, points = 13, enabled = false, nextDue = today.plusDays(1).toString()) // disabled, excluded
            ),
            pointsSummaryResult = listOf(
                PointsSummaryDto(person = "alice", points_7d = 10),
                PointsSummaryDto(person = "bob", points_7d = 4)
            )
        )

        val viewModel = ChoresStatsViewModel(ChoreRepository(api), PeopleRepository(api))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val stats = (state as UiState.Success).data
        assertEquals(2, stats.totalEnabledChores)
        assertEquals(13, stats.totalPoints)
        assertEquals(14, stats.completedLast7Days)
        assertEquals(5, stats.dueNext7DaysPoints)
    }
}
