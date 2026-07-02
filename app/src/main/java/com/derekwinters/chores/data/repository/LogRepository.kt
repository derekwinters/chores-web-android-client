package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.model.AuthLogEntry
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/** A page of Activity Log results plus the total row count, for pagination (issue #19). */
data class LogPage(val entries: List<LogEntry>, val total: Int)

/**
 * chores-web's unified Activity Log (issue #19), also used (filtered) by the Chore card History
 * action (issue #15) and the User Detail activity feed (issue #17).
 */
@Singleton
class LogRepository @Inject constructor(
    private val api: ChoresApi
) {
    suspend fun getLog(
        person: String? = null,
        chore: String? = null,
        action: String? = null,
        start: String? = null,
        end: String? = null,
        page: Int = 1
    ): Result<LogPage> = safeApiCall { api.getLog(person, chore, action, start, end, page) }
        .map { LogPage(it.items.map { entry -> entry.toDomain() }, it.total) }
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
