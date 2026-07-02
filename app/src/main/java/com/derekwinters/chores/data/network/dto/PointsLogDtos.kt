package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/** Response element for `GET /v1/admin/db/points-log`, issue #23's admin correction table. */
@Serializable
data class PointsLogEntryDto(
    val id: Int,
    val person: String,
    val points: Int,
    val chore: String,
    val completed_at: String
)

/** Paginated (20/page per issue #23) response shape for `GET /v1/admin/db/points-log`. */
@Serializable
data class PointsLogPageDto(
    val items: List<PointsLogEntryDto> = emptyList(),
    val total: Int = 0
)

/** Request body for `PATCH /v1/admin/db/points-log/{id}`, issue #23 inline edit. */
@Serializable
data class UpdatePointsLogRequestDto(
    val person: String? = null,
    val points: Int? = null
)
