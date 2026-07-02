package com.derekwinters.chores.ui.chores

import com.derekwinters.chores.data.model.Chore
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Issue #13 behaviors: live search, filters (schedule type, assignment type, assignee incl.
 * "Unassigned", state, due-within, enabled status), and the due/next_due/name sort order.
 */
class ChoreFiltersTest {

    private fun chore(
        id: Int,
        name: String,
        state: String = "due",
        nextDue: String? = null,
        currentAssignee: String? = "alice",
        enabled: Boolean = true,
        scheduleType: String? = "weekly",
        assignmentType: String? = "fixed"
    ) = Chore(
        id = id,
        name = name,
        points = 5,
        state = state,
        nextDue = nextDue,
        currentAssignee = currentAssignee,
        eligiblePeople = listOf("alice", "bob"),
        enabled = enabled,
        assignmentType = assignmentType,
        scheduleType = scheduleType
    )

    @Test
    fun applyFilters_query_isCaseInsensitiveSubstringMatch() {
        val chores = listOf(chore(1, "Dishes"), chore(2, "Trash"))
        assertEquals(listOf("Dishes"), chores.applyFilters(ChoreFilters(query = "dish")).map { it.name })
    }

    @Test
    fun applyFilters_assignees_matchesUnassignedSentinel() {
        val chores = listOf(chore(1, "Dishes", currentAssignee = "alice"), chore(2, "Trash", currentAssignee = null))
        val result = chores.applyFilters(ChoreFilters(assignees = setOf(UNASSIGNED_FILTER_VALUE)))
        assertEquals(listOf("Trash"), result.map { it.name })
    }

    @Test
    fun applyFilters_state_filtersDueVsComplete() {
        val chores = listOf(chore(1, "Dishes", state = "due"), chore(2, "Trash", state = "complete"))
        assertEquals(listOf("Dishes"), chores.applyFilters(ChoreFilters(state = ChoreStateFilter.DUE)).map { it.name })
        assertEquals(listOf("Trash"), chores.applyFilters(ChoreFilters(state = ChoreStateFilter.COMPLETE)).map { it.name })
    }

    @Test
    fun applyFilters_dueWithin_includesOverdueAndExcludesFartherOut() {
        val today = LocalDate.of(2026, 7, 2)
        val chores = listOf(
            chore(1, "Overdue", nextDue = "2026-06-30"),
            chore(2, "DueToday", nextDue = "2026-07-02"),
            chore(3, "DueIn5Days", nextDue = "2026-07-07"),
            chore(4, "NoDueDate", nextDue = null)
        )

        val result = chores.applyFilters(ChoreFilters(dueWithin = DueWithinFilter.NEXT_3_DAYS), today = today)

        assertEquals(listOf("Overdue", "DueToday"), result.map { it.name })
    }

    @Test
    fun applyFilters_enabledFilter_filtersDisabledChores() {
        val chores = listOf(chore(1, "Dishes", enabled = true), chore(2, "Trash", enabled = false))
        assertEquals(listOf("Trash"), chores.applyFilters(ChoreFilters(enabledFilter = EnabledFilter.DISABLED)).map { it.name })
    }

    @Test
    fun sortedForChoresScreen_ordersDueBeforeComplete_thenByNextDue_thenByName() {
        val chores = listOf(
            chore(1, "Zebra", state = "due", nextDue = "2026-07-10"),
            chore(2, "Apple", state = "complete", nextDue = "2026-07-01"),
            chore(3, "Mango", state = "due", nextDue = null),
            chore(4, "Banana", state = "due", nextDue = "2026-07-05")
        )

        val sorted = chores.sortedForChoresScreen().map { it.name }

        assertEquals(listOf("Banana", "Zebra", "Mango", "Apple"), sorted)
    }

    @Test
    fun availableAssigneeOptions_includesUnassignedWhenPresent() {
        val chores = listOf(chore(1, "Dishes", currentAssignee = "bob"), chore(2, "Trash", currentAssignee = null))
        assertEquals(listOf("bob", UNASSIGNED_FILTER_VALUE), chores.availableAssigneeOptions())
    }
}
