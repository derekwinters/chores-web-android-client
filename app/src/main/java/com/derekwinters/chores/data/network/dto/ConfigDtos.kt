package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response/request body for `GET/PUT /v1/config`, backing the four Settings forms (issue #20),
 * the Dashboard/Chores due-soon window (issue #12, `due_soon_days`), and log retention
 * (issue #22). Sent back in full on `PUT` (no partial-update endpoint per the issue references),
 * so every field the client doesn't render still round-trips via `ignoreUnknownKeys` + these
 * defaults.
 */
@Serializable
data class ConfigDto(
    val app_title: String = "Chores",
    val timezone: String = "UTC",
    val auth_enabled: Boolean = false,
    val notify_days_before: Int = 3,
    val due_hour: Int = 0,
    val due_soon_days: Int = 3,
    val log_retention_days: Int = 90,
    val update_check_enabled: Boolean = true,
    val update_check_interval_hours: Int = 24,
    val app_version: String? = null,
    val latest_version: String? = null,
    val last_checked_at: String? = null
)
