package com.derekwinters.chores.ui.chores

import app.cash.turbine.test
import com.derekwinters.chores.chores.Chore
import com.derekwinters.chores.chores.ChoreRepository
import com.derekwinters.chores.chores.ChoresApi
import com.derekwinters.chores.chores.CompleteBody
import com.derekwinters.chores.chores.PeopleApi
import com.derekwinters.chores.chores.PeopleRepository
import com.derekwinters.chores.chores.Person
import com.derekwinters.chores.common.UiState
import com.derekwinters.chores.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Behaviors: `GET /chores` chore list loading; complete-chore action with Completer-picker dialog
 * for null-assignee chores; sealed UiState + StateFlow pattern (area: ui, android, network)
 */
class ChoreListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val assignedChore = Chore(
        id = 1, name = "Dishes", currentAssignee = "alice", points = 5, state = "due", nextDue = "2026-07-05"
    )
    private val unassignedChore = Chore(
        id = 2, name = "Vacuum", currentAssignee = null, points = 3, state = "due", nextDue = null
    )
    private val people = listOf(Person("alice", "Alice"), Person("bob", "Bob"))

    private lateinit var fakeChoresApi: FakeChoresApi
    private lateinit var fakePeopleApi: FakePeopleApi

    private fun buildViewModel(): ChoreListViewModel = ChoreListViewModel(
        ChoreRepository(fakeChoresApi),
        PeopleRepository(fakePeopleApi)
    )

    @Before
    fun setUp() {
        fakeChoresApi = FakeChoresApi(listOf(assignedChore, unassignedChore))
        fakePeopleApi = FakePeopleApi(people)
    }

    @Test
    fun init_loadsChores_intoSuccessState() {
        val viewModel = buildViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(listOf(assignedChore, unassignedChore), (state as UiState.Success).data)
    }

    @Test
    fun init_apiError_setsErrorState() {
        fakeChoresApi.getChoresResult = Result.failure(RuntimeException("boom"))
        val viewModel = buildViewModel()

        assertTrue(viewModel.uiState.value is UiState.Error)
    }

    @Test
    fun loadChores_emitsLoadingThenSuccess() = runTest {
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            assertEquals(UiState.Success(listOf(assignedChore, unassignedChore)), awaitItem())

            viewModel.loadChores()

            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Success(listOf(assignedChore, unassignedChore)), awaitItem())
        }
    }

    @Test
    fun onCompleteClicked_withAssignee_completesImmediately_withNullCompletedBy_andReloads() {
        val viewModel = buildViewModel()

        viewModel.onCompleteClicked(assignedChore)

        assertEquals(assignedChore.id, fakeChoresApi.lastCompleteId)
        assertEquals(CompleteBody(completedBy = null), fakeChoresApi.lastCompleteBody)
        assertNull(viewModel.pendingCompleterPick.value)
    }

    @Test
    fun onCompleteClicked_withNullAssignee_fetchesPeople_andSurfacesPendingCompleterPick() {
        val viewModel = buildViewModel()

        viewModel.onCompleteClicked(unassignedChore)

        val pending = viewModel.pendingCompleterPick.value
        assertEquals(unassignedChore, pending?.chore)
        assertEquals(people, pending?.people)
        assertNull(fakeChoresApi.lastCompleteId) // not completed yet — awaiting picker selection
    }

    @Test
    fun onCompleterSelected_completesChoreWithChosenUsername_andClearsPending() {
        val viewModel = buildViewModel()
        viewModel.onCompleteClicked(unassignedChore)

        viewModel.onCompleterSelected("bob")

        assertEquals(unassignedChore.id, fakeChoresApi.lastCompleteId)
        assertEquals(CompleteBody(completedBy = "bob"), fakeChoresApi.lastCompleteBody)
        assertNull(viewModel.pendingCompleterPick.value)
    }

    @Test
    fun onCompleterPickCancelled_clearsPendingWithoutCompleting() {
        val viewModel = buildViewModel()
        viewModel.onCompleteClicked(unassignedChore)

        viewModel.onCompleterPickCancelled()

        assertNull(viewModel.pendingCompleterPick.value)
        assertNull(fakeChoresApi.lastCompleteId)
    }

    @Test
    fun onCompleteClicked_completeApiError_setsErrorState() {
        val viewModel = buildViewModel()
        fakeChoresApi.completeResult = Result.failure(RuntimeException("boom"))

        viewModel.onCompleteClicked(assignedChore)

        assertTrue(viewModel.uiState.value is UiState.Error)
    }
}

private class FakeChoresApi(
    initialChores: List<Chore> = emptyList()
) : ChoresApi {
    var getChoresResult: Result<List<Chore>> = Result.success(initialChores)
    var completeResult: Result<Unit>? = null
    var lastCompleteId: Int? = null
    var lastCompleteBody: CompleteBody? = null

    override suspend fun getChores(): List<Chore> = getChoresResult.getOrThrow()

    override suspend fun completeChore(id: Int, body: CompleteBody): Chore {
        lastCompleteId = id
        lastCompleteBody = body
        completeResult?.getOrThrow()
        return Chore(id = id, name = "Chore $id", currentAssignee = body.completedBy, points = 1, state = "done")
    }
}

private class FakePeopleApi(private val people: List<Person>) : PeopleApi {
    override suspend fun getPeople(): List<Person> = people
}
