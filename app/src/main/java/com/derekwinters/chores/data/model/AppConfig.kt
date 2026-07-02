package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.ConfigDto

/** Domain model for the admin-configurable settings (issues #12, #20, #22). */
data class AppConfig(
    val appTitle: String,
    val timezone: String,
    val authEnabled: Boolean,
    val notifyDaysBefore: Int,
    val dueHour: Int,
    val dueSoonDays: Int,
    val logRetentionDays: Int,
    val updateCheckEnabled: Boolean,
    val updateCheckIntervalHours: Int,
    val appVersion: String?,
    val latestVersion: String?,
    val lastCheckedAt: String?
) {
    val updateAvailable: Boolean
        get() = appVersion != null && latestVersion != null && appVersion != latestVersion
}

fun ConfigDto.toDomain(): AppConfig = AppConfig(
    appTitle = app_title,
    timezone = timezone,
    authEnabled = auth_enabled,
    notifyDaysBefore = notify_days_before,
    dueHour = due_hour,
    dueSoonDays = due_soon_days,
    logRetentionDays = log_retention_days,
    updateCheckEnabled = update_check_enabled,
    updateCheckIntervalHours = update_check_interval_hours,
    appVersion = app_version,
    latestVersion = latest_version,
    lastCheckedAt = last_checked_at
)

fun AppConfig.toDto(): ConfigDto = ConfigDto(
    app_title = appTitle,
    timezone = timezone,
    auth_enabled = authEnabled,
    notify_days_before = notifyDaysBefore,
    due_hour = dueHour,
    due_soon_days = dueSoonDays,
    log_retention_days = logRetentionDays,
    update_check_enabled = updateCheckEnabled,
    update_check_interval_hours = updateCheckIntervalHours,
    app_version = appVersion,
    latest_version = latestVersion,
    last_checked_at = lastCheckedAt
)
