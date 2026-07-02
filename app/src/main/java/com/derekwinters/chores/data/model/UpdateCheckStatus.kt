package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.UpdateCheckStatusDto

/**
 * Domain model for `GET /v1/config/updates/status` / `POST /v1/config/updates/check` (issue #20's
 * "About" section version info + manual "Check Now" action). Kept separate from [AppConfig]
 * since the backend serves this from its own endpoints, not from `GET/PUT /v1/config`.
 */
data class UpdateCheckStatus(
    val currentVersion: String,
    val latestVersion: String?,
    val lastCheckedAt: String?,
    val checkEnabled: Boolean,
    val checkIntervalHours: Int,
    val updateAvailable: Boolean
)

fun UpdateCheckStatusDto.toDomain(): UpdateCheckStatus = UpdateCheckStatus(
    currentVersion = current_version,
    latestVersion = latest_version,
    lastCheckedAt = last_checked_at,
    checkEnabled = check_enabled,
    checkIntervalHours = check_interval_hours,
    updateAvailable = update_available
)
