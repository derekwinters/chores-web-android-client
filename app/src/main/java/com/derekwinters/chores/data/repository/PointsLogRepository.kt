package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.model.PointsLogEntry
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.dto.UpdatePointsLogRequestDto
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A page of Points Log admin results (issue #23). Pagination is offset-based, matching the real
 * `AdminDbPage` response: [offset]/[limit] are the values the backend actually applied (its
 * defaults if the caller didn't specify them), so callers should use these — not the values they
 * requested — to compute the next/previous offset.
 */
data class PointsLogPage(val entries: List<PointsLogEntry>, val total: Int, val offset: Int, val limit: Int)

/** Issue #23: admin table for directly correcting historical point credits. */
@Singleton
class PointsLogRepository @Inject constructor(
    private val api: ChoresApi
) {
    suspend fun getPointsLog(limit: Int = PAGE_SIZE, offset: Int = 0): Result<PointsLogPage> =
        safeApiCall { api.getPointsLog(limit, offset) }
            .map { PointsLogPage(it.items.map { e -> e.toDomain() }, it.total, it.offset, it.limit) }

    /**
     * [person] and [points] are both required by the real `PointsLogUpdate` schema — there's no
     * partial-update support, so callers must resend the entry's current value for whichever of
     * the two fields isn't changing.
     */
    suspend fun updateEntry(entryId: Int, person: String, points: Int): Result<PointsLogEntry> =
        safeApiCall { api.updatePointsLogEntry(entryId, UpdatePointsLogRequestDto(points = points, person = person)) }
            .map { it.toDomain() }

    /** Reverses the points on the person (floored at 0) server-side; cannot be undone. */
    suspend fun deleteEntry(entryId: Int): Result<Unit> = safeApiCall { api.deletePointsLogEntry(entryId) }

    /** Per-person raw points history at `GET /v1/points/{person}`; not yet used by any screen. */
    suspend fun getPersonHistory(username: String): Result<List<PointsLogEntry>> =
        safeApiCall { api.getPersonPointsHistory(username) }.map { list -> list.map { it.toDomain() } }

    companion object {
        const val PAGE_SIZE = 20
    }
}
