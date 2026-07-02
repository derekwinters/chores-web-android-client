package com.derekwinters.chores.chores

import retrofit2.http.GET

/** Retrofit service for `GET /v1/people` — used to populate the Completer-picker dialog. */
interface PeopleApi {

    @GET("people")
    suspend fun getPeople(): List<Person>
}
