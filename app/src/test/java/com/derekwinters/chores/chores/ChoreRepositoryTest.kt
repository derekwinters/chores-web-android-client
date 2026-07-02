package com.derekwinters.chores.chores

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Behaviors: GET /chores and POST /chores/{id}/complete (area: network)
 */
class ChoreRepositoryTest {

    @Test
    fun getChores_delegatesToApi() = runTest {
        val chores = listOf(
            Chore(id = 1, name = "Dishes", currentAssignee = "alice", points = 5, state = "due", nextDue = "2026-07-05")
        )
        val fakeApi = FakeChoresApi(choresResult = chores)
        val repository = ChoreRepository(fakeApi)

        val result = repository.getChores()

        assertEquals(chores, result)
    }

    @Test
    fun completeChore_withAssignee_sendsNullCompletedBy() = runTest {
        val fakeApi = FakeChoresApi()
        val repository = ChoreRepository(fakeApi)

        repository.completeChore(choreId = 1)

        assertEquals(1, fakeApi.lastCompleteId)
        assertEquals(CompleteBody(completedBy = null), fakeApi.lastCompleteBody)
    }

    @Test
    fun completeChore_withExplicitCompleter_sendsCompletedByUsername() = runTest {
        val fakeApi = FakeChoresApi()
        val repository = ChoreRepository(fakeApi)

        repository.completeChore(choreId = 2, completedBy = "bob")

        assertEquals(2, fakeApi.lastCompleteId)
        assertEquals(CompleteBody(completedBy = "bob"), fakeApi.lastCompleteBody)
    }
}

private class FakeChoresApi(
    private val choresResult: List<Chore> = emptyList()
) : ChoresApi {
    var lastCompleteId: Int? = null
    var lastCompleteBody: CompleteBody? = null

    override suspend fun getChores(): List<Chore> = choresResult

    override suspend fun completeChore(id: Int, body: CompleteBody): Chore {
        lastCompleteId = id
        lastCompleteBody = body
        return Chore(id = id, name = "Chore $id", currentAssignee = body.completedBy, points = 1, state = "done")
    }
}
