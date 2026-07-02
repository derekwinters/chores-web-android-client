package com.derekwinters.chores.chores

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit service for the chores-web chores endpoints (backend/app/routers/chores.py).
 * Read + complete only for issue #5 — create/edit/delete/skip/reassign/mark-due are out of scope.
 */
interface ChoresApi {

    @GET("chores")
    suspend fun getChores(): List<Chore>

    @POST("chores/{id}/complete")
    suspend fun completeChore(@Path("id") id: Int, @Body body: CompleteBody): Chore
}
