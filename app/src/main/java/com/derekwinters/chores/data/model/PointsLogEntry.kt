package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.PointsLogEntryDto

/**
 * Domain model for one Points Log row (issue #23's admin correction table, and the per-person
 * raw history at `GET /v1/points/{person}`). `person` is a username, and `choreId` is the chore's
 * numeric id — the real backend doesn't return a chore name/description on this row.
 */
data class PointsLogEntry(
    val id: Int,
    val person: String,
    val points: Int,
    val choreId: Int,
    val completedAt: String
)

fun PointsLogEntryDto.toDomain(): PointsLogEntry = PointsLogEntry(
    id = id,
    person = person,
    points = points,
    choreId = chore_id,
    completedAt = completed_at
)
