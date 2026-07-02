package com.derekwinters.chores.ui.users

import androidx.lifecycle.SavedStateHandle
import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.LogEntryDto
import com.derekwinters.chores.data.network.dto.LogPageDto
import com.derekwinters.chores.data.network.dto.PersonStatsDto
import com.derekwinters.chores.data.network.dto.RedemptionDto
import com.derekwinters.chores.data.repository.LogRepository
import com.derekwinters.chores.data.repository.PeopleRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #17 behaviors: stats/redemption-history/activity-feed loading and the redeem-amount
 * validation ("numeric, >0, ≤ available").
 */
class UserDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(api: FakeChoresApi, personId: Int = 1, username: String? = "alice") =
        UserDetailViewModel(
            PeopleRepository(api),
            LogRepository(api),
            SavedStateHandle(mapOf("personId" to personId, "username" to username))
        )

    @Test
    fun load_combinesStatsRedemptionsAndFilteredActivity() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            personStatsResult = PersonStatsDto(available_points = 20, points_7d = 10, points_30d = 40, redeemed_total = 5, completed_count = 8),
            redemptionsResult = listOf(RedemptionDto(id = 1, amount = 5, redeemed_by = "admin", timestamp = "2026-07-01")),
            logResult = LogPageDto(
                items = listOf(
                    LogEntryDto(id = 1, timestamp = "t", target_type = "chore", action = "completed", actor = "alice", target_name = "Dishes"),
                    LogEntryDto(id = 2, timestamp = "t", target_type = "chore", action = "created", actor = "alice", target_name = "Trash")
                ),
                total = 2
            )
        )
        val viewModel = buildViewModel(api)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(20, data.stats.availablePoints)
        assertEquals(1, data.redemptions.size)
        assertEquals(1, data.activity.size)
        assertEquals("completed", data.activity.single().action)
    }

    @Test
    fun validateRedeemAmount_nonNumeric_isError() {
        val viewModel = buildViewModel(FakeChoresApi())
        assertEquals("Enter a valid number", viewModel.validateRedeemAmount("abc"))
    }

    @Test
    fun validateRedeemAmount_zeroOrNegative_isError() {
        val viewModel = buildViewModel(FakeChoresApi())
        assertEquals("Amount must be greater than 0", viewModel.validateRedeemAmount("0"))
    }

    @Test
    fun validateRedeemAmount_exceedsAvailable_isError() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(personStatsResult = PersonStatsDto(available_points = 10))
        val viewModel = buildViewModel(api)
        advanceUntilIdle()

        assertEquals("Amount exceeds available points", viewModel.validateRedeemAmount("11"))
        assertNull(viewModel.validateRedeemAmount("10"))
    }

    @Test
    fun redeem_success_reloadsStats() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            personStatsResult = PersonStatsDto(available_points = 20),
            redeemResult = PersonStatsDto(available_points = 10)
        )
        val viewModel = buildViewModel(api)
        advanceUntilIdle()

        viewModel.redeem(10)
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.redeemState.value)
        assertEquals(10, (viewModel.uiState.value as UiState.Success).data.stats.availablePoints)
    }
}
