package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response element for `GET /v1/log`, chores-web's unified Activity Log (issue #19), also reused
 * (filtered) for the User Detail chore-activity feed (issue #17) and the Chore card History link
 * (issue #15).
 */
@Serializable
data class LogEntryDto(
    val id: Int,
    val timestamp: String,
    val target_type: String,
    val action: String,
    val actor: String,
    val target_name: String,
    val reassigned_to: String? = null,
    val field_name: String? = null,
    val old_value: String? = null,
    val new_value: String? = null
)

/** Paginated response shape for `GET /v1/log`. */
@Serializable
data class LogPageDto(
    val items: List<LogEntryDto> = emptyList(),
    val total: Int = 0
)

/** Response element for `GET /v1/auth/log`, issue #21's separate admin auth audit trail. */
@Serializable
data class AuthLogEntryDto(
    val id: Int,
    val timestamp: String,
    val username: String,
    val action: String,
    val changed_by: String? = null
)

/** Paginated response shape for `GET /v1/auth/log`. */
@Serializable
data class AuthLogPageDto(
    val items: List<AuthLogEntryDto> = emptyList(),
    val total: Int = 0
)
