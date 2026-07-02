package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.AuthLogEntryDto
import com.derekwinters.chores.data.network.dto.LogEntryDto

/**
 * Domain model for one Activity Log row (issue #19), also used (via filtered queries) by the
 * Chore card History link (issue #15) and the User Detail activity feed (issue #17).
 */
data class LogEntry(
    val id: Int,
    val timestamp: String,
    val targetType: String,
    val action: String,
    val actor: String,
    val targetName: String,
    val reassignedTo: String?,
    val fieldName: String?,
    val oldValue: String?,
    val newValue: String?
) {
    /** Amendments/updates surface as `updated` entries with a field-level diff (issue #19). */
    val isAmendment: Boolean get() = action == "updated" && fieldName != null
}

fun LogEntryDto.toDomain(): LogEntry = LogEntry(
    id = id,
    timestamp = timestamp,
    targetType = target_type,
    action = action,
    actor = actor,
    targetName = target_name,
    reassignedTo = reassigned_to,
    fieldName = field_name,
    oldValue = old_value,
    newValue = new_value
)

/** Domain model for one Auth Event Log row (issue #21). */
data class AuthLogEntry(
    val id: Int,
    val timestamp: String,
    val username: String,
    val action: String,
    val changedBy: String?
)

fun AuthLogEntryDto.toDomain(): AuthLogEntry = AuthLogEntry(
    id = id,
    timestamp = timestamp,
    username = username,
    action = action,
    changedBy = changed_by
)
