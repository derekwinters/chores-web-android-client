package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response element for `GET /v1/people` (issues #12, #17, #18). `goal_7d`/`goal_30d` default to
 * the chores-web values (12/50) per issue #12's scope when the backend omits them.
 */
@Serializable
data class PersonDto(
    val id: Int,
    val username: String,
    val display_name: String,
    val is_admin: Boolean = false,
    val goal_7d: Int = 12,
    val goal_30d: Int = 50
)

/** Per-person rolling points totals, issue #12: "points-summary/stats endpoint". */
@Serializable
data class PointsSummaryDto(
    val person_id: Int,
    val points_7d: Int = 0,
    val points_30d: Int = 0
)

/** Response for `GET /v1/people/{id}/stats`, issue #17. */
@Serializable
data class PersonStatsDto(
    val available_points: Int = 0,
    val points_7d: Int = 0,
    val points_30d: Int = 0,
    val redeemed_total: Int = 0,
    val completed_count: Int = 0
)

/** Request body for `POST /v1/people` (create user), issue #18: display name + password only. */
@Serializable
data class CreatePersonRequestDto(
    val display_name: String,
    val password: String
)

/**
 * Request body for `PATCH /v1/people/{id}` (edit user), issue #18. All fields optional/nullable
 * so the client only sends what changed; `password` blank/null means "unchanged".
 */
@Serializable
data class UpdatePersonRequestDto(
    val display_name: String? = null,
    val username: String? = null,
    val goal_7d: Int? = null,
    val goal_30d: Int? = null,
    val password: String? = null,
    val is_admin: Boolean? = null
)

/** Request body for `POST /v1/people/{id}/redeem`, issue #17. */
@Serializable
data class RedeemRequestDto(
    val amount: Int
)

/** Response element for `GET /v1/people/{id}/redemptions`, issue #17. */
@Serializable
data class RedemptionDto(
    val id: Int,
    val amount: Int,
    val redeemed_by: String,
    val timestamp: String
)
