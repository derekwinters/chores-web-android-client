package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.network.dto.ChoreRequestDto

/**
 * Domain model for a chore (issue #5, extended by #13/#14/#15/#16 with the schedule/assignment/
 * constraint fields the Chores screen's filters, stats panel, card detail, and create/edit form
 * need).
 *
 * `currentAssignee == null` means the chore has no fixed assignee — the chore list shows the
 * "Completer" placeholder for it, and completing it requires picking a Completer from
 * [eligiblePeople] (see CompleterPickerDialog).
 */
data class Chore(
    val id: Int,
    val name: String,
    val points: Int,
    val state: String,
    val nextDue: String?,
    val currentAssignee: String?,
    val eligiblePeople: List<String>,
    val enabled: Boolean = true,
    val assignmentType: String? = null,
    val scheduleType: String? = null,
    val scheduleSummary: String? = null,
    val rotation: List<String> = emptyList(),
    val assignedToNext: String? = null,
    val weeklyDays: List<Int> = emptyList(),
    val everyOtherWeek: Boolean = false,
    val monthlyMode: String? = null,
    val dayOfMonth: Int? = null,
    val nthWeekdayIndex: Int? = null,
    val nthWeekdayDay: Int? = null,
    val month: Int? = null,
    val intervalDays: Int? = null,
    val evenOddConstraint: String? = null,
    val weekdayConstraint: List<Int> = emptyList(),
    val constraintNotMetBehavior: String? = null
) {
    val needsCompleterSelection: Boolean get() = currentAssignee == null
    val isDue: Boolean get() = state == "due"
}

fun ChoreDto.toDomain(): Chore = Chore(
    id = id,
    name = name,
    points = points,
    state = state,
    nextDue = next_due,
    currentAssignee = current_assignee,
    eligiblePeople = eligible_people,
    enabled = enabled,
    assignmentType = assignment_type,
    scheduleType = schedule_type,
    scheduleSummary = schedule_summary,
    rotation = rotation,
    assignedToNext = assigned_to_next,
    weeklyDays = weekly_days,
    everyOtherWeek = every_other_week,
    monthlyMode = monthly_mode,
    dayOfMonth = day_of_month,
    nthWeekdayIndex = nth_weekday_index,
    nthWeekdayDay = nth_weekday_day,
    month = month,
    intervalDays = interval_days,
    evenOddConstraint = even_odd_constraint,
    weekdayConstraint = weekday_constraint,
    constraintNotMetBehavior = constraint_not_met_behavior
)

/**
 * Issue #16: create/edit form submission payload. Only the fields relevant to [assignmentType]/
 * [scheduleType] are meaningful; the form is responsible for validating those combinations before
 * building a draft (see ChoreFormValidation).
 */
data class ChoreDraft(
    val name: String,
    val points: Int,
    val enabled: Boolean,
    val nextDue: String?,
    val assignmentType: String,
    val assignee: String?,
    val eligiblePeople: List<String>,
    val rotation: List<String>,
    val currentAssignee: String?,
    val assignedToNext: String?,
    val scheduleType: String,
    val weeklyDays: List<Int>,
    val everyOtherWeek: Boolean,
    val monthlyMode: String?,
    val dayOfMonth: Int?,
    val nthWeekdayIndex: Int?,
    val nthWeekdayDay: Int?,
    val month: Int?,
    val intervalDays: Int?,
    val evenOddConstraint: String?,
    val weekdayConstraint: List<Int>,
    val constraintNotMetBehavior: String?
)

fun ChoreDraft.toRequestDto(): ChoreRequestDto = ChoreRequestDto(
    name = name,
    points = points,
    enabled = enabled,
    next_due = nextDue,
    assignment_type = assignmentType,
    assignee = assignee,
    eligible_people = eligiblePeople,
    rotation = rotation,
    current_assignee = currentAssignee,
    assigned_to_next = assignedToNext,
    schedule_type = scheduleType,
    weekly_days = weeklyDays,
    every_other_week = everyOtherWeek,
    monthly_mode = monthlyMode,
    day_of_month = dayOfMonth,
    nth_weekday_index = nthWeekdayIndex,
    nth_weekday_day = nthWeekdayDay,
    month = month,
    interval_days = intervalDays,
    even_odd_constraint = evenOddConstraint,
    weekday_constraint = weekdayConstraint,
    constraint_not_met_behavior = constraintNotMetBehavior
)

/** Fibonacci point values chores-web restricts the Points dropdown to (issue #16). */
val CHORE_POINT_OPTIONS = listOf(1, 2, 3, 5, 8, 13)
