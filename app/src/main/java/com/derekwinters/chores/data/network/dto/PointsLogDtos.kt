package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response element for `GET /v1/admin/db/points-log` (issue #23's admin correction table) and
 * `GET /v1/points/{person}` (per-person raw history); matches the real backend's
 * `PointsLogAdminOut`/`PointsLogOut` schemas exactly (same shape for both). `person` is the
 * username, not a numeric person id, and there is no `chore` name field — only `chore_id`.
 */
@Serializable
data class PointsLogEntryDto(
    val id: Int,
    val person: String,
    val points: Int,
    val chore_id: Int,
    val completed_at: String
)

/**
 * Paginated response shape for `GET /v1/admin/db/points-log`, matching the real `AdminDbPage`
 * schema. Pagination is offset-based (`offset`/`limit`), not page-number-based, and the backend
 * echoes back the `offset`/`limit` it actually applied.
 */
@Serializable
data class PointsLogPageDto(
    val items: List<PointsLogEntryDto> = emptyList(),
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = 20
)

/**
 * Request body for `PATCH /v1/admin/db/points-log/{entry_id}`, matching the real `PointsLogUpdate`
 * schema. Both fields are required server-side — there is no partial-update support, so editing
 * an entry always resends its current point value and person (username), possibly changed.
 */
@Serializable
data class UpdatePointsLogRequestDto(
    val points: Int,
    val person: String
)
