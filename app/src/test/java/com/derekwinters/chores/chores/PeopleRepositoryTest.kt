package com.derekwinters.chores.chores

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Behavior: people list backing the Completer-picker dialog (area: network)
 */
class PeopleRepositoryTest {

    @Test
    fun getPeople_delegatesToApi() = runTest {
        val people = listOf(Person(username = "alice", name = "Alice"), Person(username = "bob", name = "Bob"))
        val repository = PeopleRepository(FakePeopleApi(people))

        val result = repository.getPeople()

        assertEquals(people, result)
    }
}

private class FakePeopleApi(private val people: List<Person>) : PeopleApi {
    override suspend fun getPeople(): List<Person> = people
}
