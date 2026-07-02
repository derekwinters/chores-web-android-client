package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.model.PointsLogEntry
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.dto.UpdatePointsLogRequestDto
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/** A page of Points Log admin results plus the total row count (issue #23, 20/page). */
data class PointsLogPage(val entries: List<PointsLogEntry>, val total: Int)

/** Issue #23: admin table for directly correcting historical point credits. */
@Singleton
class PointsLogRepository @Inject constructor(
    private val api: ChoresApi
) {
    suspend fun getPointsLog(page: Int = 1): Result<PointsLogPage> =
        safeApiCall { api.getPointsLog(page) }.map { PointsLogPage(it.items.map { e -> e.toDomain() }, it.total) }

    suspend fun updateEntry(entryId: Int, person: String? = null, points: Int? = null): Result<PointsLogEntry> =
        safeApiCall { api.updatePointsLogEntry(entryId, UpdatePointsLogRequestDto(person, points)) }
            .map { it.toDomain() }

    /** Reverses the points on the person (floored at 0) server-side; cannot be undone. */
    suspend fun deleteEntry(entryId: Int): Result<Unit> = safeApiCall { api.deletePointsLogEntry(entryId) }
}
