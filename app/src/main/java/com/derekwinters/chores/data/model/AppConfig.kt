package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.ConfigDto

/** Domain model for the admin-configurable settings (issues #12, #20), mirroring `ConfigOut`. */
data class AppConfig(
    val appTitle: String,
    val authEnabled: Boolean,
    val timezone: String,
    val dueSoonDays: Int,
    val dueTimeHour: Int,
    val updateCheckEnabled: Boolean,
    val updateCheckInterval: Int
)

fun ConfigDto.toDomain(): AppConfig = AppConfig(
    appTitle = title,
    authEnabled = auth_enabled,
    timezone = timezone,
    dueSoonDays = due_soon_days,
    dueTimeHour = due_time_hour,
    updateCheckEnabled = update_check_enabled,
    updateCheckInterval = update_check_interval
)

fun AppConfig.toDto(): ConfigDto = ConfigDto(
    title = appTitle,
    auth_enabled = authEnabled,
    timezone = timezone,
    due_soon_days = dueSoonDays,
    due_time_hour = dueTimeHour,
    update_check_enabled = updateCheckEnabled,
    update_check_interval = updateCheckInterval
)
