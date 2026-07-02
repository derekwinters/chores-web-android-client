package com.derekwinters.chores.ui.chores

import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.model.ChoreDraft

/** Issue #16 radio choices, matching chores-web's ChoreForm.jsx field names verbatim. */
object AssignmentType {
    const val OPEN = "open"
    const val FIXED = "fixed"
    const val ROTATING = "rotating"
    val ALL = listOf(OPEN, FIXED, ROTATING)
}

object ScheduleType {
    const val WEEKLY = "weekly"
    const val MONTHLY = "monthly"
    const val YEARLY = "yearly"
    const val INTERVAL = "interval"
    val ALL = listOf(WEEKLY, MONTHLY, YEARLY, INTERVAL)
}

object MonthlyMode {
    const val DAY_OF_MONTH = "day_of_month"
    const val NTH_WEEKDAY = "nth_weekday"
}

object ConstraintBehavior {
    const val SKIP = "skip"
    const val DELAY = "delay"
}

/**
 * Issue #16: full create/edit form state. Mirrors [ChoreDraft] field-for-field but keeps values
 * in edit-friendly shapes (e.g. `pointsIndex` into [com.derekwinters.chores.data.model.CHORE_POINT_OPTIONS]
 * instead of the raw point value, `weekdayConstraintText` isn't needed since weekday sets are
 * already simple `Set<Int>`).
 */
data class ChoreFormState(
    val name: String = "",
    val points: Int = 1,
    val enabled: Boolean = true,
    val nextDue: String? = null,
    val assignmentType: String = AssignmentType.FIXED,
    val assignee: String? = null,
    val eligiblePeople: Set<String> = emptySet(),
    val rotation: Set<String> = emptySet(),
    val currentAssignee: String? = null,
    val assignedToNext: String? = null,
    val scheduleType: String = ScheduleType.WEEKLY,
    val weeklyDays: Set<Int> = emptySet(),
    val everyOtherWeek: Boolean = false,
    val monthlyMode: String = MonthlyMode.DAY_OF_MONTH,
    val dayOfMonth: Int? = null,
    val nthWeekdayIndex: Int? = null,
    val nthWeekdayDay: Int? = null,
    val month: Int? = null,
    val intervalDays: Int? = null,
    val evenOddConstraint: String? = null,
    val weekdayConstraint: Set<Int> = emptySet(),
    val constraintNotMetBehavior: String = ConstraintBehavior.SKIP,
    val isEditMode: Boolean = false
) {
    fun toDraft(): ChoreDraft = ChoreDraft(
        name = name.trim(),
        points = points,
        enabled = enabled,
        nextDue = nextDue,
        assignmentType = assignmentType,
        assignee = assignee.takeIf { assignmentType == AssignmentType.FIXED },
        eligiblePeople = eligiblePeople.toList(),
        rotation = rotation.toList(),
        currentAssignee = currentAssignee,
        assignedToNext = assignedToNext,
        scheduleType = scheduleType,
        weeklyDays = weeklyDays.toList(),
        everyOtherWeek = everyOtherWeek,
        monthlyMode = monthlyMode.takeIf { scheduleType == ScheduleType.MONTHLY },
        dayOfMonth = dayOfMonth,
        nthWeekdayIndex = nthWeekdayIndex,
        nthWeekdayDay = nthWeekdayDay,
        month = month.takeIf { scheduleType == ScheduleType.YEARLY },
        intervalDays = intervalDays,
        evenOddConstraint = evenOddConstraint.takeIf { scheduleType != ScheduleType.YEARLY },
        weekdayConstraint = if (scheduleType != ScheduleType.YEARLY) weekdayConstraint.toList() else emptyList(),
        constraintNotMetBehavior = constraintNotMetBehavior.takeIf { scheduleType != ScheduleType.YEARLY }
    )
}

fun Chore.toFormState(): ChoreFormState = ChoreFormState(
    name = name,
    points = points,
    enabled = enabled,
    nextDue = nextDue,
    assignmentType = assignmentType ?: AssignmentType.FIXED,
    assignee = currentAssignee,
    eligiblePeople = eligiblePeople.toSet(),
    rotation = rotation.toSet(),
    currentAssignee = currentAssignee,
    assignedToNext = assignedToNext,
    scheduleType = scheduleType ?: ScheduleType.WEEKLY,
    weeklyDays = weeklyDays.toSet(),
    everyOtherWeek = everyOtherWeek,
    monthlyMode = monthlyMode ?: MonthlyMode.DAY_OF_MONTH,
    dayOfMonth = dayOfMonth,
    nthWeekdayIndex = nthWeekdayIndex,
    nthWeekdayDay = nthWeekdayDay,
    month = month,
    intervalDays = intervalDays,
    evenOddConstraint = evenOddConstraint,
    weekdayConstraint = weekdayConstraint.toSet(),
    constraintNotMetBehavior = constraintNotMetBehavior ?: ConstraintBehavior.SKIP,
    isEditMode = true
)

/**
 * Issue #16 validation rules: "name required; weekly needs ≥1 day; interval ≥1; not both
 * even+odd [not applicable — evenOddConstraint is a single nullable field so this is structurally
 * impossible]; fixed needs an assignee; rotating needs ≥2 eligible people; current/next assignee
 * must be within the rotation."
 */
fun ChoreFormState.validate(): List<String> {
    val errors = mutableListOf<String>()
    if (name.isBlank()) errors += "Name is required"

    when (assignmentType) {
        AssignmentType.FIXED -> if (assignee.isNullOrBlank()) errors += "Fixed assignment requires an assignee"
        AssignmentType.ROTATING -> {
            if (rotation.size < 2) errors += "Rotating assignment requires at least 2 people"
            if (currentAssignee != null && currentAssignee !in rotation) {
                errors += "Current assignee must be in the rotation"
            }
            if (assignedToNext != null && assignedToNext !in rotation) {
                errors += "Assigned-to-next must be in the rotation"
            }
        }
        AssignmentType.OPEN -> Unit
    }

    when (scheduleType) {
        ScheduleType.WEEKLY -> if (weeklyDays.isEmpty()) errors += "Weekly schedule requires at least 1 day"
        ScheduleType.INTERVAL -> if ((intervalDays ?: 0) < 1) errors += "Interval must be at least 1 day"
        else -> Unit
    }

    return errors
}
