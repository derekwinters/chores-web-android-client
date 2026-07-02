package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response for `POST /v1/import/config`, issue #22: reports the counts of what was imported so
 * the confirmation screen can show a success summary. The export side (`GET /v1/export/config`)
 * is fetched as a raw JSON blob (see ChoresApi.exportConfig) rather than a typed DTO, since the
 * client's only job is to write the bytes to a file untouched.
 */
@Serializable
data class ImportResultDto(
    val people_count: Int = 0,
    val chores_count: Int = 0,
    val settings_count: Int = 0
)
