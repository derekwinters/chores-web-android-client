package com.derekwinters.chores.chores

import javax.inject.Inject
import javax.inject.Singleton

/** Access to the people list, used for the Completer-picker dialog. */
@Singleton
class PeopleRepository @Inject constructor(
    private val peopleApi: PeopleApi
) {
    suspend fun getPeople(): List<Person> = peopleApi.getPeople()
}
