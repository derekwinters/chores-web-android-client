package com.derekwinters.chores.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request body for `POST /v1/auth/login`. */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/** Response body for `POST /v1/auth/login` — see backend/app/routers/auth.py in chores-web. */
@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    val user: UserInfo
)

/** User info embedded in [LoginResponse] and returned by `GET /v1/auth/me`. */
@Serializable
data class UserInfo(
    val username: String,
    @SerialName("is_admin") val isAdmin: Boolean = false
)
