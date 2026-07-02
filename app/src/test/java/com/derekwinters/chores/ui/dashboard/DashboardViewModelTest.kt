package com.derekwinters.chores.ui.dashboard

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.network.dto.PersonDto
import com.derekwinters.chores.data.network.dto.PointsSummaryDto
import com.derekwinters.chores.data.repository.ChoreRepository
import com.derekwinters.chores.data.repository.ConfigRepository
import com.derekwinters.chores.data.repository.PeopleRepository
import com.derekwinters.chores.ui.UiState
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #12 behavior: per-person Dashboard cards from GET /people + points-summary + GET
 * /chores. The ViewModel's 60s auto-refresh loop never completes on its own, and `runTest`
 * performs an implicit full drain of the scheduler after the test body returns regardless of
 * using [runCurrent] instead of `advanceUntilIdle` inside the body — so that drain would still
 * hang forever advancing through the endless series of virtual-time delays unless the loop's
 * job is cancelled before the test body ends.
 */
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refresh_buildsOneCardPerPerson() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            choresResult = listOf(
                ChoreDto(id = 1, name = "Dishes", points = 5, state = "due", current_assignee = "alice")
            ),
            pointsSummaryResult = listOf(PointsSummaryDto(person_id = 1, points_7d = 10, points_30d = 40)),
            peopleResult = listOf(PersonDto(id = 1, username = "alice", display_name = "Alice", goal_7d = 12, goal_30d = 50))
        )
        val viewModel = DashboardViewModel(PeopleRepository(api), ChoreRepository(api), ConfigRepository(api))
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val cards = (state as UiState.Success).data
        assertEquals(1, cards.size)
        assertEquals("Alice", cards.single().displayName)
        assertEquals(10, cards.single().points7d)

        viewModel.viewModelScope.cancel()
    }
}
