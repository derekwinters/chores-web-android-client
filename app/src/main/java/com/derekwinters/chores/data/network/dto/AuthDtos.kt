package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /v1/auth/login`, matching chores-web's `LoginRequest` schema
 * (backend/app/schemas.py).
 */
@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

/**
 * Response body for `POST /v1/auth/login`, matching chores-web's `LoginResponse` schema.
 *
 * `access_token` is the 365-day JWT (see backend/app/routers/auth.py `_create_jwt_token`);
 * `token_type` is expected to be "bearer" and is used verbatim when building the
 * `Authorization` header so we don't hard-code casing/spelling.
 */
@Serializable
data class LoginResponseDto(
    val access_token: String,
    val token_type: String,
    val user: UserInfoDto
)

/** Matches chores-web's `UserInfo` schema, returned by login and `GET /v1/auth/me`. */
@Serializable
data class UserInfoDto(
    val username: String,
    val is_admin: Boolean
)

/**
 * Response for `GET /v1/auth/setup-status`, issue #11: whether the backend has no users yet and
 * should show the first-run "Create Admin Account" flow instead of Login.
 */
@Serializable
data class SetupStatusDto(
    val setup_needed: Boolean = false
)

/**
 * Request body for `POST /v1/auth/setup`, issue #11: creates the first (admin) user. The
 * "Require Authentication" toggle is applied afterwards via a separate `PUT /v1/config` call
 * (see AuthRepository.setup), matching the issue's "creates the first user as admin, then sets
 * auth_enabled via config" sequencing.
 */
@Serializable
data class SetupRequestDto(
    val username: String,
    val password: String
)

/**
 * Request body for `PUT /v1/auth/password/reset`, issue #11's forced-reset flow. Sent with the
 * one-time `reset_token` (from the 403 login response) as bearer auth, not the normal session
 * token.
 */
@Serializable
data class ResetPasswordRequestDto(
    val new_password: String
)

/**
 * Error body shape for a 403 login response that carries a forced-password-reset token
 * (issue #11), parsed alongside the plain `{"detail": ...}` shape that other errors use.
 */
@Serializable
data class LoginResetRequiredDto(
    val detail: String? = null,
    val reset_token: String? = null
)

/** Response for `GET /status/db-status` (not under the `v1/` prefix), issue #11's readiness gate. */
@Serializable
data class DbStatusDto(
    val ready: Boolean = false
)
