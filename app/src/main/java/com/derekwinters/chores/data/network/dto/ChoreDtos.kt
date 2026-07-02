package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Nested value of `ChoreOut`/`ChoreCreate`/`ChoreUpdate`'s `schedule_config` field.
 *
 * IMPORTANT: the real backend OpenAPI spec declares `schedule_config` as
 * `{"type": "object", "additionalProperties": true}` — i.e. **fully opaque**, with no documented
 * inner keys anywhere in the schema. The field names below are a best-effort guess, not a
 * confirmed shape: they're carried over verbatim (snake_case included) from this app's earlier,
 * incorrectly-flattened `ChoreDto`/`ChoreRequestDto`, which were themselves derived from real
 * chores-web issue descriptions of its schedule-editing UI (weekly days, interval-days, monthly
 * day-of-month vs. nth-weekday, yearly month, even/odd-day and weekday constraints, and
 * constraint-not-met skip/delay behavior all clearly exist as product concepts) — they are just
 * nested here under `schedule_config` instead of left flat on the chore, matching the real
 * `ChoreOut`/`ChoreCreate`/`ChoreUpdate` schemas.
 *
 * TODO(follow-up): confirm/correct these field names against a real chores-web browser
 * network-tab capture of a create/edit chore request before relying on this for anything beyond
 * best-effort round-tripping.
 */
@Serializable
data class ScheduleConfig(
    val weekly_days: List<Int>? = null,
    val every_other_week: Boolean? = null,
    val monthly_mode: String? = null,
    val day_of_month: Int? = null,
    val nth_weekday_index: Int? = null,
    val nth_weekday_day: Int? = null,
    val month: Int? = null,
    val interval_days: Int? = null,
    val even_odd_constraint: String? = null,
    val weekday_constraint: List<Int>? = null,
    val constraint_not_met_behavior: String? = null
)

/**
 * Response element for `GET /v1/chores` / `GET /v1/chores/{id}` / the chore action endpoints,
 * matching the real backend's `ChoreOut` schema. `ignoreUnknownKeys = true` (see NetworkModule)
 * means any further backend fields are simply dropped, and every field here has a default so
 * test fixtures that omit some of them (most do, since ChoreOut's `required` list is long) keep
 * deserializing the same as before.
 *
 * `disabled` matches the wire format directly (chores-web's real field name is `disabled`, not
 * `enabled`) to avoid maintaining an inverted mapping at this boundary — see [Chore.enabled] in
 * the domain model for the inverted convenience getter used at UI call sites.
 */
@Serializable
data class ChoreDto(
    val id: Int,
    val name: String,
    val schedule_type: String = "",
    val schedule_config: ScheduleConfig = ScheduleConfig(),
    val assignment_type: String = "open",
    val eligible_people: List<String> = emptyList(),
    val assignee: String? = null,
    val points: Int = 0,
    val state: String = "",
    val disabled: Boolean = false,
    val next_due: String? = null,
    val current_assignee: String? = null,
    val rotation_index: Int = 0,
    val last_changed_at: String? = null,
    val last_changed_by: String? = null,
    val last_change_type: String? = null,
    val last_completed_at: String? = null,
    val last_completed_by: String? = null,
    val age: Int? = null,
    val schedule_summary: String? = null,
    // Server-computed, read-only "who's up next in the rotation"; never sent by the client.
    val next_assignee: String? = null
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
 * Request body for `POST /v1/chores` (create), matching the real backend's `ChoreCreate` schema:
 * `name`/`schedule_type`/`schedule_config` are required; the rest default the same way the real
 * schema does.
 */
@Serializable
data class ChoreCreateRequestDto(
    val name: String,
    val schedule_type: String,
    val schedule_config: ScheduleConfig,
    val assignment_type: String = "open",
    val eligible_people: List<String> = emptyList(),
    val assignee: String? = null,
    val points: Int = 0,
    val disabled: Boolean = false
)

/**
 * Request body for `PUT /v1/chores/{id}` (update), matching the real backend's `ChoreUpdate`
 * schema — a true partial-update shape where every field is optional/nullable. This app's form
 * always submits the full set of fields it collected rather than a sparse diff (matching
 * chores-web's own `ChoreForm.jsx` submit behavior), but the type here stays nullable/optional to
 * match the real schema rather than `ChoreCreateRequestDto`'s required fields.
 *
 * `next_assignee` is deliberately omitted: although `ChoreUpdate` technically accepts it, it's a
 * server-computed "who's up next" value with no corresponding editable control in chores-web's
 * own edit form, so this app never sends it.
 */
@Serializable
data class ChoreUpdateRequestDto(
    val name: String? = null,
    val schedule_type: String? = null,
    val schedule_config: ScheduleConfig? = null,
    val assignment_type: String? = null,
    val eligible_people: List<String>? = null,
    val assignee: String? = null,
    val current_assignee: String? = null,
    val points: Int? = null,
    val disabled: Boolean? = null,
    val next_due: String? = null
)

/**
 * Request body for `POST /v1/chores/{id}/reassign`, matching the real backend's `ReassignBody`
 * schema — `assignee` is required/non-nullable here (unlike `SkipReassignBody.assignee`, which
 * the real schema does allow to be null).
 */
@Serializable
data class ReassignRequestDto(
    val assignee: String
)
