package com.derekwinters.chores.ui.chores

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #16 validation behavior: "name required; weekly needs ≥1 day; interval ≥1; fixed needs
 * an assignee; rotating needs ≥2 eligible people; current/next assignee must be within the
 * rotation."
 */
class ChoreFormStateTest {

    @Test
    fun validate_blankName_isError() {
        val state = ChoreFormState(name = "", assignmentType = AssignmentType.FIXED, assignee = "alice")
        assertTrue(state.validate().any { it.contains("Name") })
    }

    @Test
    fun validate_fixedWithoutAssignee_isError() {
        val state = ChoreFormState(name = "Dishes", assignmentType = AssignmentType.FIXED, assignee = null)
        assertTrue(state.validate().any { it.contains("Fixed") })
    }

    @Test
    fun validate_rotatingWithFewerThanTwoPeople_isError() {
        val state = ChoreFormState(name = "Dishes", assignmentType = AssignmentType.ROTATING, eligiblePeople = setOf("alice"))
        assertTrue(state.validate().any { it.contains("Rotating") })
    }

    @Test
    fun validate_rotatingCurrentAssigneeOutsideRotation_isError() {
        val state = ChoreFormState(
            name = "Dishes",
            assignmentType = AssignmentType.ROTATING,
            eligiblePeople = setOf("alice", "bob"),
            currentAssignee = "carol"
        )
        assertTrue(state.validate().any { it.contains("Current assignee") })
    }

    @Test
    fun validate_openAssignment_needsNoAssignee() {
        val state = ChoreFormState(name = "Dishes", assignmentType = AssignmentType.OPEN, weeklyDays = setOf(1))
        assertEquals(emptyList<String>(), state.validate())
    }

    @Test
    fun validate_weeklyWithoutDays_isError() {
        val state = ChoreFormState(
            name = "Dishes",
            assignmentType = AssignmentType.OPEN,
            scheduleType = ScheduleType.WEEKLY,
            weeklyDays = emptySet()
        )
        assertTrue(state.validate().any { it.contains("Weekly") })
    }

    @Test
    fun validate_intervalLessThanOne_isError() {
        val state = ChoreFormState(
            name = "Dishes",
            assignmentType = AssignmentType.OPEN,
            scheduleType = ScheduleType.INTERVAL,
            intervalDays = 0
        )
        assertTrue(state.validate().any { it.contains("Interval") })
    }

    @Test
    fun validate_validFixedWeeklyChore_hasNoErrors() {
        val state = ChoreFormState(
            name = "Dishes",
            assignmentType = AssignmentType.FIXED,
            assignee = "alice",
            scheduleType = ScheduleType.WEEKLY,
            weeklyDays = setOf(1, 3, 5)
        )
        assertEquals(emptyList<String>(), state.validate())
    }

    @Test
    fun toDraft_openAssignment_omitsAssignee() {
        val state = ChoreFormState(name = "Dishes", assignmentType = AssignmentType.OPEN, assignee = "alice")
        assertEquals(null, state.toDraft().assignee)
    }

    @Test
    fun toDraft_fixedAssignment_includesAssignee() {
        val state = ChoreFormState(name = "Dishes", assignmentType = AssignmentType.FIXED, assignee = "alice")
        assertEquals("alice", state.toDraft().assignee)
    }

    @Test
    fun toDraft_yearlySchedule_omitsConstraints() {
        val state = ChoreFormState(
            name = "Dishes",
            assignmentType = AssignmentType.OPEN,
            scheduleType = ScheduleType.YEARLY,
            evenOddConstraint = "even",
            weekdayConstraint = setOf(1, 2)
        )
        val draft = state.toDraft()
        assertEquals(null, draft.scheduleConfig.even_odd_constraint)
        assertEquals(emptyList<Int>(), draft.scheduleConfig.weekday_constraint)
    }
}
