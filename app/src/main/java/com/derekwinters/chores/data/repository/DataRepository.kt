package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** Issue #22: config export/import backup. */
@Singleton
class DataRepository @Inject constructor(
    private val api: ChoresApi
) {
    /** Raw backup JSON bytes, written to a user-picked file untouched by the caller. */
    suspend fun exportConfig(): Result<String> =
        safeApiCall { api.exportConfig().string() }

    /** [json] is the raw contents of the user-picked `.json` file. */
    suspend fun importConfig(json: String): Result<ImportSummary> =
        safeApiCall { api.importConfig(json.toRequestBody("application/json".toMediaType())) }
            .map { ImportSummary(it.people_count, it.chores_count, it.settings_count) }
}

/** Issue #22: reported counts shown in the post-import success message. */
data class ImportSummary(val peopleCount: Int, val choresCount: Int, val settingsCount: Int)
