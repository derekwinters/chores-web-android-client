package com.derekwinters.chores.ui.chores

import com.derekwinters.chores.data.model.Chore
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Sentinel assignee value for chores with no fixed assignee (issue #13). */
const val UNASSIGNED_FILTER_VALUE = "Unassigned"

enum class ChoreStateFilter { ALL, DUE, COMPLETE }

enum class DueWithinFilter { ALL, TODAY, NEXT_3_DAYS, NEXT_7_DAYS, NEXT_30_DAYS }

enum class EnabledFilter { ALL, ENABLED, DISABLED }

/**
 * Issue #13 behavior: "Filters: schedule type, assignment type, assignee (multi-select,
 * including a synthetic 'Unassigned' option), state (due/complete), 'due within' ... enabled/
 * disabled status", plus live search. Also carries the Dashboard deep-link shape (issue #12:
 * assignee + due-within) so both screens share one filter model.
 */
data class ChoreFilters(
    val query: String = "",
    val scheduleType: String? = null,
    val assignmentType: String? = null,
    val assignees: Set<String> = emptySet(),
    val state: ChoreStateFilter = ChoreStateFilter.ALL,
    val dueWithin: DueWithinFilter = DueWithinFilter.ALL,
    val enabledFilter: EnabledFilter = EnabledFilter.ALL
) {
    val isActive: Boolean
        get() = query.isNotBlank() || scheduleType != null || assignmentType != null ||
            assignees.isNotEmpty() || state != ChoreStateFilter.ALL ||
            dueWithin != DueWithinFilter.ALL || enabledFilter != EnabledFilter.ALL
}

/** Issue #13: live, case-insensitive substring match on chore name plus the other filter fields. */
fun List<Chore>.applyFilters(filters: ChoreFilters, today: LocalDate = LocalDate.now()): List<Chore> = filter { chore ->
    matchesQuery(chore, filters.query) &&
        matchesScheduleType(chore, filters.scheduleType) &&
        matchesAssignmentType(chore, filters.assignmentType) &&
        matchesAssignees(chore, filters.assignees) &&
        matchesState(chore, filters.state) &&
        matchesDueWithin(chore, filters.dueWithin, today) &&
        matchesEnabled(chore, filters.enabledFilter)
}

private fun matchesQuery(chore: Chore, query: String): Boolean =
    query.isBlank() || chore.name.contains(query, ignoreCase = true)

private fun matchesScheduleType(chore: Chore, scheduleType: String?): Boolean =
    scheduleType == null || chore.scheduleType == scheduleType

private fun matchesAssignmentType(chore: Chore, assignmentType: String?): Boolean =
    assignmentType == null || chore.assignmentType == assignmentType

private fun matchesAssignees(chore: Chore, assignees: Set<String>): Boolean {
    if (assignees.isEmpty()) return true
    val choreAssignee = chore.currentAssignee ?: UNASSIGNED_FILTER_VALUE
    return choreAssignee in assignees
}

private fun matchesState(chore: Chore, state: ChoreStateFilter): Boolean = when (state) {
    ChoreStateFilter.ALL -> true
    ChoreStateFilter.DUE -> chore.isDue
    ChoreStateFilter.COMPLETE -> !chore.isDue
}

private fun matchesDueWithin(chore: Chore, dueWithin: DueWithinFilter, today: LocalDate): Boolean {
    if (dueWithin == DueWithinFilter.ALL) return true
    val dueDate = chore.nextDue?.let(::parseDateOrNull) ?: return false
    val maxDaysAhead = when (dueWithin) {
        DueWithinFilter.TODAY -> 0
        DueWithinFilter.NEXT_3_DAYS -> 3
        DueWithinFilter.NEXT_7_DAYS -> 7
        DueWithinFilter.NEXT_30_DAYS -> 30
        DueWithinFilter.ALL -> return true
    }
    // "Today incl. overdue" and every other window include anything already past due.
    return !dueDate.isAfter(today.plusDays(maxDaysAhead.toLong()))
}

private fun matchesEnabled(chore: Chore, enabledFilter: EnabledFilter): Boolean = when (enabledFilter) {
    EnabledFilter.ALL -> true
    EnabledFilter.ENABLED -> chore.enabled
    EnabledFilter.DISABLED -> !chore.enabled
}

private fun parseDateOrNull(value: String): LocalDate? = try {
    LocalDate.parse(value)
} catch (e: DateTimeParseException) {
    null
}

/**
 * Issue #13 sort behavior: "due chores before complete chores, then ascending by next_due
 * (chores with no due date sort last), then alphabetical by name".
 */
fun List<Chore>.sortedForChoresScreen(): List<Chore> = sortedWith(
    compareByDescending<Chore> { it.isDue }
        .thenBy(nullsLast()) { it.nextDue?.let(::parseDateOrNull) }
        .thenBy { it.name.lowercase() }
)

/** Issue #13: assignee options for the multi-select filter, including "Unassigned" when relevant. */
fun List<Chore>.availableAssigneeOptions(): List<String> {
    val named = mapNotNull { it.currentAssignee }.distinct().sorted()
    val hasUnassigned = any { it.currentAssignee == null }
    return if (hasUnassigned) named + UNASSIGNED_FILTER_VALUE else named
}

fun List<Chore>.availableScheduleTypes(): List<String> = mapNotNull { it.scheduleType }.distinct().sorted()

fun List<Chore>.availableAssignmentTypes(): List<String> = mapNotNull { it.assignmentType }.distinct().sorted()
