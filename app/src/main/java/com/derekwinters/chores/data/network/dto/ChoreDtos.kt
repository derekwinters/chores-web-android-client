package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response element for `GET /v1/chores`. Originally trimmed to the fields issue #5 needed; the
 * v1.0.0 milestone (issues #13-#16) extends this with the full `ChoreOut` schedule/assignment/
 * constraint surface so the Chores screen can filter/sort on it and the Chore form can pre-fill
 * an edit. `ignoreUnknownKeys = true` (see NetworkModule) means any further backend fields are
 * simply dropped, and every field added here has a default so older test fixtures that omit them
 * keep deserializing the same as before.
 */
@Serializable
data class ChoreDto(
    val id: Int,
    val name: String,
    val points: Int,
    val state: String,
    val next_due: String? = null,
    val current_assignee: String? = null,
    val eligible_people: List<String> = emptyList(),
    // Issue #13/#14/#15/#16 additions:
    val enabled: Boolean = true,
    val assignment_type: String? = null,
    val schedule_type: String? = null,
    val schedule_summary: String? = null,
    val rotation: List<String> = emptyList(),
    val assigned_to_next: String? = null,
    val weekly_days: List<Int> = emptyList(),
    val every_other_week: Boolean = false,
    val monthly_mode: String? = null,
    val day_of_month: Int? = null,
    val nth_weekday_index: Int? = null,
    val nth_weekday_day: Int? = null,
    val month: Int? = null,
    val interval_days: Int? = null,
    val even_odd_constraint: String? = null,
    val weekday_constraint: List<Int> = emptyList(),
    val constraint_not_met_behavior: String? = null
)

/**
 * Request body for `POST /v1/chores/{id}/complete`, matching chores-web's `CompleteBody` schema.
 * `completed_by` is null for already-assigned chores (server defaults to `current_assignee`) and
 * set to the chosen username when the Completer-picker dialog is used.
 */
@Serializable
data class CompleteChoreRequestDto(
    val completed_by: String? = null
)

/**
 * Shared shape for `POST /v1/chores` (create) and `PUT /v1/chores/{id}` (update), issue #16.
 * Only the fields relevant to the selected assignment/schedule type are meaningful server-side;
 * the rest are sent as null/empty and ignored, matching chores-web's `ChoreForm.jsx` submit
 * payload (it always sends the full shape rather than a sparse diff).
 */
@Serializable
data class ChoreRequestDto(
    val name: String,
    val points: Int,
    val enabled: Boolean = true,
    val next_due: String? = null,
    val assignment_type: String,
    val assignee: String? = null,
    val eligible_people: List<String> = emptyList(),
    val rotation: List<String> = emptyList(),
    val current_assignee: String? = null,
    val assigned_to_next: String? = null,
    val schedule_type: String,
    val weekly_days: List<Int> = emptyList(),
    val every_other_week: Boolean = false,
    val monthly_mode: String? = null,
    val day_of_month: Int? = null,
    val nth_weekday_index: Int? = null,
    val nth_weekday_day: Int? = null,
    val month: Int? = null,
    val interval_days: Int? = null,
    val even_odd_constraint: String? = null,
    val weekday_constraint: List<Int> = emptyList(),
    val constraint_not_met_behavior: String? = null
)

/** Request body for `POST /v1/chores/{id}/reassign`, issue #16 (editing `open`'s assignee IS reassignment). */
@Serializable
data class ReassignRequestDto(
    val assignee: String? = null
)
