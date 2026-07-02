package com.derekwinters.chores.ui.chores

import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.model.ChoreDraft
import com.derekwinters.chores.data.network.dto.ScheduleConfig

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
 * already simple `Set<Int>`), and keeps the schedule fields flat here (nested into
 * [ScheduleConfig] only when building a draft) since that's simpler for per-field form editing.
 *
 * There is no separate "rotation" concept: [eligiblePeople] backs both the "open" assignment
 * type's optional eligible-to-complete checklist and the "rotating" type's rotation-membership
 * checklist, matching the real backend's single `eligible_people` field (see [Chore]'s KDoc).
 */
data class ChoreFormState(
    val name: String = "",
    val points: Int = 1,
    val disabled: Boolean = false,
    val nextDue: String? = null,
    val assignmentType: String = AssignmentType.FIXED,
    val assignee: String? = null,
    val eligiblePeople: Set<String> = emptySet(),
    val currentAssignee: String? = null,
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
        disabled = disabled,
        nextDue = nextDue,
        assignmentType = assignmentType,
        assignee = assignee.takeIf { assignmentType == AssignmentType.FIXED },
        eligiblePeople = eligiblePeople.toList(),
        currentAssignee = currentAssignee,
        scheduleType = scheduleType,
        scheduleConfig = ScheduleConfig(
            weekly_days = weeklyDays.toList(),
            every_other_week = everyOtherWeek,
            monthly_mode = monthlyMode.takeIf { scheduleType == ScheduleType.MONTHLY },
            day_of_month = dayOfMonth,
            nth_weekday_index = nthWeekdayIndex,
            nth_weekday_day = nthWeekdayDay,
            month = month.takeIf { scheduleType == ScheduleType.YEARLY },
            interval_days = intervalDays,
            even_odd_constraint = evenOddConstraint.takeIf { scheduleType != ScheduleType.YEARLY },
            weekday_constraint = if (scheduleType != ScheduleType.YEARLY) weekdayConstraint.toList() else emptyList(),
            constraint_not_met_behavior = constraintNotMetBehavior.takeIf { scheduleType != ScheduleType.YEARLY }
        )
    )
}

fun Chore.toFormState(): ChoreFormState {
    val config = scheduleConfig
    return ChoreFormState(
        name = name,
        points = points,
        disabled = disabled,
        nextDue = nextDue,
        assignmentType = assignmentType ?: AssignmentType.FIXED,
        assignee = assignee ?: currentAssignee,
        eligiblePeople = eligiblePeople.toSet(),
        currentAssignee = currentAssignee,
        scheduleType = scheduleType ?: ScheduleType.WEEKLY,
        weeklyDays = config.weekly_days?.toSet() ?: emptySet(),
        everyOtherWeek = config.every_other_week ?: false,
        monthlyMode = config.monthly_mode ?: MonthlyMode.DAY_OF_MONTH,
        dayOfMonth = config.day_of_month,
        nthWeekdayIndex = config.nth_weekday_index,
        nthWeekdayDay = config.nth_weekday_day,
        month = config.month,
        intervalDays = config.interval_days,
        evenOddConstraint = config.even_odd_constraint,
        weekdayConstraint = config.weekday_constraint?.toSet() ?: emptySet(),
        constraintNotMetBehavior = config.constraint_not_met_behavior ?: ConstraintBehavior.SKIP,
        isEditMode = true
    )
}

/**
 * Issue #16 validation rules: "name required; weekly needs ≥1 day; interval ≥1; not both
 * even+odd [not applicable — evenOddConstraint is a single nullable field so this is structurally
 * impossible]; fixed needs an assignee; rotating needs ≥2 eligible people; current assignee must
 * be within [ChoreFormState.eligiblePeople]."
 */
fun ChoreFormState.validate(): List<String> {
    val errors = mutableListOf<String>()
    if (name.isBlank()) errors += "Name is required"

    when (assignmentType) {
        AssignmentType.FIXED -> if (assignee.isNullOrBlank()) errors += "Fixed assignment requires an assignee"
        AssignmentType.ROTATING -> {
            if (eligiblePeople.size < 2) errors += "Rotating assignment requires at least 2 people"
            if (currentAssignee != null && currentAssignee !in eligiblePeople) {
                errors += "Current assignee must be in the rotation"
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
