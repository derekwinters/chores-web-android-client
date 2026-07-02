package com.derekwinters.chores.auth

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit service for the chores-web auth endpoints (backend/app/routers/auth.py).
 * Base URL is a placeholder rewritten per-request by
 * [com.derekwinters.chores.network.ServerUrlInterceptor].
 */
interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun me(): UserInfo
}
