package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.model.toDto
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Admin-configurable settings backing the Settings screens (issue #20), the Dashboard/Chores
 * due-soon window (issue #12), and log retention (issue #22).
 */
@Singleton
class ConfigRepository @Inject constructor(
    private val api: ChoresApi
) {
    suspend fun getConfig(): Result<AppConfig> = safeApiCall { api.getConfig() }.map { it.toDomain() }

    /** No partial-update endpoint per the issue references, so the full config is round-tripped. */
    suspend fun updateConfig(config: AppConfig): Result<AppConfig> =
        safeApiCall { api.updateConfig(config.toDto()) }.map { it.toDomain() }

    /** Issue #20: "Check Now" manual update check. */
    suspend fun checkForUpdates(): Result<AppConfig> = safeApiCall { api.checkForUpdates() }.map { it.toDomain() }
}
