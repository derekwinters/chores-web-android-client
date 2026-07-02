package com.derekwinters.chores.ui.chores

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.repository.ChoreRepository
import com.derekwinters.chores.ui.UiState
import java.io.IOException
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Behaviors: "Chore list screen: GET /chores, render name/assignee-or-Completer/points/state/
 * next_due", "Complete-chore action ... with Completer-picker dialog when
 * current_assignee == null", and "Sealed UiState + StateFlow pattern for ChoreListViewModel"
 * (area: android, ui, network).
 */
class ChoreListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val assignedChoreDto = ChoreDto(
        id = 1,
        name = "Dishes",
        points = 5,
        state = "due",
        next_due = "2026-07-05",
        current_assignee = "alice",
        eligible_people = listOf("alice", "bob")
    )
    private val unassignedChoreDto = ChoreDto(
        id = 2,
        name = "Trash",
        points = 3,
        state = "due",
        next_due = null,
        current_assignee = null,
        eligible_people = listOf("alice", "bob")
    )

    @Test
    fun init_loadsChores_andStartsInLoadingState() {
        val viewModel = ChoreListViewModel(ChoreRepository(FakeChoresApi(choresResult = listOf(assignedChoreDto))))
        assertEquals(UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun loadChores_success_mapsDtosToDomainChores() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ChoreListViewModel(
            ChoreRepository(FakeChoresApi(choresResult = listOf(assignedChoreDto, unassignedChoreDto)))
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val chores = (state as UiState.Success).data
        assertEquals(2, chores.size)
        assertEquals("alice", chores[0].currentAssignee)
        assertTrue(chores[1].needsCompleterSelection)
        assertEquals(listOf("alice", "bob"), chores[1].eligiblePeople)
    }

    @Test
    fun loadChores_failure_updatesStateToError() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ChoreListViewModel(ChoreRepository(FakeChoresApi(choresError = IOException("boom"))))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals(HttpErrorMessages.NETWORK_ERROR, (state as UiState.Error).message)
    }

    @Test
    fun completeChore_assignedChore_sendsNullCompletedBy_andReloadsList() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            choresResult = listOf(assignedChoreDto),
            completeResult = assignedChoreDto.copy(state = "done")
        )
        val viewModel = ChoreListViewModel(ChoreRepository(api))
        advanceUntilIdle()

        val chore = Chore(1, "Dishes", 5, "due", "2026-07-05", "alice", listOf("alice", "bob"))
        viewModel.completeChore(chore)
        advanceUntilIdle()

        assertEquals(1, api.lastCompleteChoreId)
        assertNull(api.lastCompleteRequest?.completed_by)
        assertNull(viewModel.completingChoreId.value)
    }

    @Test
    fun completeChore_withCompleter_sendsChosenUsername() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            choresResult = listOf(unassignedChoreDto),
            completeResult = unassignedChoreDto.copy(state = "done", current_assignee = "bob")
        )
        val viewModel = ChoreListViewModel(ChoreRepository(api))
        advanceUntilIdle()

        val chore = Chore(2, "Trash", 3, "due", null, null, listOf("alice", "bob"))
        assertTrue(chore.needsCompleterSelection)
        viewModel.completeChore(chore, completedBy = "bob")
        advanceUntilIdle()

        assertEquals(2, api.lastCompleteChoreId)
        assertEquals("bob", api.lastCompleteRequest?.completed_by)
    }

    @Test
    fun visibleChores_appliesQueryFilter_andSortsIndependentlyOfUiState() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ChoreListViewModel(
            ChoreRepository(FakeChoresApi(choresResult = listOf(unassignedChoreDto, assignedChoreDto)))
        )
        advanceUntilIdle()

        // Raw uiState keeps API order; visibleChores is sorted (due-before-complete, then name).
        assertEquals(listOf("Trash", "Dishes"), (viewModel.uiState.value as UiState.Success).data.map { it.name })
        assertEquals(listOf("Dishes", "Trash"), (viewModel.visibleChores.value as UiState.Success).data.map { it.name })

        viewModel.updateQuery("dish")
        advanceUntilIdle()

        assertEquals(listOf("Dishes"), (viewModel.visibleChores.value as UiState.Success).data.map { it.name })
    }

    @Test
    fun applyInitialFilters_seedsAssigneeAndDueWithin_forDashboardDeepLinks() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ChoreListViewModel(ChoreRepository(FakeChoresApi(choresResult = emptyList())))
        advanceUntilIdle()

        viewModel.applyInitialFilters(assignee = "alice", dueWithin = DueWithinFilter.NEXT_7_DAYS)

        // Includes the synthetic "Unassigned" option alongside the named assignee, matching
        // buildDashboardCards' "assigned to them or unassigned/open" definition of relevant
        // chores — an assignee-only filter would silently exclude open chores the Dashboard's
        // own count included.
        assertEquals(setOf("alice", UNASSIGNED_FILTER_VALUE), viewModel.filters.value.assignees)
        assertEquals(DueWithinFilter.NEXT_7_DAYS, viewModel.filters.value.dueWithin)
        assertEquals(ChoreStateFilter.ALL, viewModel.filters.value.state)
    }

    @Test
    fun applyInitialFilters_dueNow_alsoConstrainsToDueState() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ChoreListViewModel(ChoreRepository(FakeChoresApi(choresResult = emptyList())))
        advanceUntilIdle()

        // A null dueWithin alongside a non-null assignee is the Dashboard's "Due Now" deep link
        // (as opposed to a plain Chores-tab navigation, which doesn't call this at all) — it must
        // also filter to the due state, or "Due Now" would show every relevant chore regardless
        // of completion.
        viewModel.applyInitialFilters(assignee = "alice", dueWithin = null)

        assertEquals(setOf("alice", UNASSIGNED_FILTER_VALUE), viewModel.filters.value.assignees)
        assertEquals(ChoreStateFilter.DUE, viewModel.filters.value.state)
    }

    @Test
    fun clearFilters_resetsToDefault() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ChoreListViewModel(ChoreRepository(FakeChoresApi(choresResult = emptyList())))
        viewModel.updateQuery("dish")

        viewModel.clearFilters()

        assertEquals(ChoreFilters(), viewModel.filters.value)
    }

    @Test
    fun skipChore_reloadsListOnSuccess_andClearsPendingAction() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(choresResult = listOf(assignedChoreDto), skipResult = assignedChoreDto.copy(state = "not_due"))
        val viewModel = ChoreListViewModel(ChoreRepository(api))
        advanceUntilIdle()

        viewModel.skipChore(Chore(1, "Dishes", 5, "due", "2026-07-05", "alice", listOf("alice", "bob")))
        advanceUntilIdle()

        assertEquals(1, api.lastSkipChoreId)
        assertNull(viewModel.pendingActionChoreId.value)
    }

    @Test
    fun markChoreDue_callsRepositoryAndReloads() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(choresResult = listOf(assignedChoreDto), markDueResult = assignedChoreDto.copy(state = "due"))
        val viewModel = ChoreListViewModel(ChoreRepository(api))
        advanceUntilIdle()

        viewModel.markChoreDue(Chore(1, "Dishes", 5, "due", "2026-07-05", "alice", listOf("alice", "bob")))
        advanceUntilIdle()

        assertEquals(1, api.lastMarkDueChoreId)
    }

    @Test
    fun deleteChore_callsRepositoryAndReloads() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(choresResult = listOf(assignedChoreDto))
        val viewModel = ChoreListViewModel(ChoreRepository(api))
        advanceUntilIdle()

        viewModel.deleteChore(Chore(1, "Dishes", 5, "due", "2026-07-05", "alice", listOf("alice", "bob")))
        advanceUntilIdle()

        assertEquals(1, api.lastDeleteChoreId)
    }
}
