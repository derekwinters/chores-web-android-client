package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.PointsLogEntryDto

/** Domain model for one Points Log admin row (issue #23). */
data class PointsLogEntry(
    val id: Int,
    val person: String,
    val points: Int,
    val chore: String,
    val completedAt: String
)

fun PointsLogEntryDto.toDomain(): PointsLogEntry = PointsLogEntry(
    id = id,
    person = person,
    points = points,
    chore = chore,
    completedAt = completed_at
)
