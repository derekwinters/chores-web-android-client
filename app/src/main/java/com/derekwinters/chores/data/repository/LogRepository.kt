package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.model.AuthLogEntry
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.dto.RetentionSettingsDto
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * chores-web's unified Activity Log (issue #19), also used (filtered) by the Chore card History
 * action (issue #15) and the User Detail activity feed (issue #17), plus the log-retention setting
 * (issue #22) — a real standalone backend resource (`GET`/`POST /v1/log/retention`), not part of
 * `ConfigOut`/`ConfigUpdate`.
 */
@Singleton
class LogRepository @Inject constructor(
    private val api: ChoresApi
) {
    /**
     * Returns the full filtered result set — the backend has no server-side pagination for this
     * endpoint (`GET /v1/log` responds with a bare array), so any paging is done client-side over
     * this list. [choreId] matches the real API's `chore_id` query param, which is typed as a
     * string despite being conceptually an integer id.
     */
    suspend fun getLog(
        person: String? = null,
        choreId: String? = null,
        action: String? = null,
        actions: List<String>? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<List<LogEntry>> = safeApiCall {
        api.getLog(person, choreId, action, actions, startDate, endDate)
    }.map { entries -> entries.map { it.toDomain() } }

    /** Issue #22: current log-retention setting, in days. */
    suspend fun getRetentionDays(): Result<Int> =
        safeApiCall { api.getRetention() }.map { it.retention_days }

    /** Issue #22: persists a new log-retention setting; returns the value the backend reports back. */
    suspend fun setRetentionDays(days: Int): Result<Int> =
        safeApiCall { api.setRetention(RetentionSettingsDto(retention_days = days)) }.map { it.retention_days }
}

/** A page of Auth Event Log results plus the total row count (issue #21). */
data class AuthLogPage(val entries: List<AuthLogEntry>, val total: Int)

/** Issue #21: separate admin-only audit log for auth-related events. */
@Singleton
class AuthLogRepository @Inject constructor(
    private val api: ChoresApi
) {
    suspend fun getAuthLog(
        username: String? = null,
        action: String? = null,
        start: String? = null,
        end: String? = null,
        page: Int = 1
    ): Result<AuthLogPage> = safeApiCall { api.getAuthLog(username, action, start, end, page) }
        .map { AuthLogPage(it.items.map { entry -> entry.toDomain() }, it.total) }
}
