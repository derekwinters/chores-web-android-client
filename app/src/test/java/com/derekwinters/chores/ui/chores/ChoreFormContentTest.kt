package com.derekwinters.chores.ui.chores

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Issue #16 behavior: chore create/edit form fields + Save/Cancel actions (area: ui, android).
 * Exercises [ChoreFormContent] directly (no Hilt component needed) — see
 * ChoreFormViewModelTest/ChoreFormStateTest for validation and save-wiring coverage.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChoreFormContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun choreFormContent_typingName_updatesFormState() {
        var latest = ChoreFormState()
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = listOf("alice", "bob"),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Name").performTextInput("Dishes")

        assert(latest.name == "Dishes")
    }

    @Test
    fun choreFormContent_cancel_invokesCallback() {
        var cancelled = false
        composeTestRule.setContent {
            ChoreFormContent(
                formState = ChoreFormState(),
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = {},
                onSave = {},
                onCancel = { cancelled = true }
            )
        }

        composeTestRule.onNodeWithText("Cancel").performScrollTo().performClick()

        assert(cancelled)
    }

    @Test
    fun choreFormContent_saveError_showsMessage() {
        composeTestRule.setContent {
            ChoreFormContent(
                formState = ChoreFormState(),
                availablePeople = emptyList(),
                saveState = UiState.Error("Name is required"),
                isEditMode = false,
                onFormChange = {},
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Name is required").assertExists()
    }

    // Issue #32: Next Due switches from free-text entry to a Material3 DatePickerDialog
    // (edit-mode only).

    @Test
    fun choreFormContent_nextDueField_tapIconOpensDatePicker() {
        composeTestRule.setContent {
            ChoreFormContent(
                formState = ChoreFormState(nextDue = "2026-07-05"),
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = true,
                onFormChange = {},
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Next Due").performScrollTo().performClick()

        // Just "OK" -- the form's own Cancel button also matches the text "Cancel", so asserting
        // on the dialog's Cancel button by text alone would be ambiguous.
        composeTestRule.onNodeWithText("OK").assertExists()
    }

    @Test
    fun choreFormContent_nextDueDatePicker_confirmingPreSelectedValue_keepsIsoDate() {
        var latest = ChoreFormState(nextDue = "2026-07-05")
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = true,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Next Due").performScrollTo().performClick()
        composeTestRule.onNodeWithText("OK").performClick()

        assert(latest.nextDue == "2026-07-05")
    }

    @Test
    fun choreFormContent_nextDueDatePicker_nullValue_opensWithNoSelectionAndDoesNotSetADate() {
        var latest = ChoreFormState(nextDue = null)
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = true,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Next Due").performScrollTo().performClick()
        composeTestRule.onNodeWithText("OK").performClick()

        assert(latest.nextDue == null)
    }

    // Issue #111: date picker includes a "Today" shortcut that immediately commits today's date.

    @Test
    fun choreFormContent_nextDueDatePicker_tapToday_setsNextDueToToday() {
        var latest = ChoreFormState(nextDue = "2020-01-01")
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = true,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Next Due").performScrollTo().performClick()

        // Confirmed via prior CI diagnostics: exactly one "Today" node exists with a properly
        // bound OnClick action -- the button is wired up correctly. But performClick() (a real
        // coordinate-based touch/gesture simulation) doesn't reliably resolve for this node
        // under Robolectric, since it lives inside Material3 DatePickerDialog's own dialog
        // window alongside "Cancel" -- a known class of Robolectric/Dialog-window hit-testing
        // flakiness, not a production bug. Invoke the already-confirmed semantics OnClick action
        // directly instead, bypassing gesture/hit-test dispatch entirely.
        val todayNode = composeTestRule.onAllNodesWithText("Today").fetchSemanticsNodes().single()
        val clickAction = todayNode.config.getOrNull(SemanticsActions.OnClick)?.action
        checkNotNull(clickAction) { "Today node has no OnClick action" }
        clickAction.invoke()

        // Match production's UTC-based "today" (see ChoreFormScreen.kt's Today button), not the
        // system-default-zone LocalDate.now(), so this assertion can't drift from what the button
        // actually computes.
        val expectedToday = LocalDate.now(ZoneOffset.UTC).toString()
        assert(latest.nextDue == expectedToday) {
            "expected nextDue=$expectedToday (UTC today), got ${latest.nextDue}"
        }
    }

    // Issue #100: weekly day picker uses named day-abbreviation pills (Mon..Sun) instead of
    // numeric 0-6 checkboxes; the underlying weeklyDays indices are unchanged.

    @Test
    fun choreFormContent_weeklySchedule_showsNamedDayPillsInsteadOfNumericLabels() {
        composeTestRule.setContent {
            ChoreFormContent(
                formState = ChoreFormState(scheduleType = ScheduleType.WEEKLY),
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = {},
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Mon").assertExists()
        composeTestRule.onNodeWithText("Wed").assertExists()
        composeTestRule.onNodeWithText("Sun").assertExists()
    }

    @Test
    fun choreFormContent_weeklySchedule_tappingUnselectedDayPill_addsUnderlyingWeekdayIndex() {
        var latest = ChoreFormState(scheduleType = ScheduleType.WEEKLY)
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        // "Wed" is index 3 in the 0=Sun..6=Sat data model the pills display over.
        composeTestRule.onNodeWithText("Wed").performScrollTo().performClick()

        assert(latest.weeklyDays == setOf(3))
    }

    @Test
    fun choreFormContent_weeklySchedule_tappingSelectedDayPill_removesUnderlyingWeekdayIndex() {
        var latest = ChoreFormState(scheduleType = ScheduleType.WEEKLY, weeklyDays = setOf(3))
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Wed").performScrollTo().performClick()

        assert(latest.weeklyDays == emptySet<Int>())
    }

    // Issue #103: Constraints section gains a collapsible header (chevron toggle, matching
    // ChoresStatsPanel/ActivityLogScreen's expand/collapse convention) and a "weekdays only"
    // sub-picker backed by `weekdayConstraint`. These tests use ScheduleType.MONTHLY (not
    // WEEKLY) so the only "Mon".."Sun" day pills on screen are this sub-picker's -- with
    // WEEKLY selected, the "Days of week" picker higher up the form would also render pills
    // with the same text, making onNodeWithText ambiguous.

    @Test
    fun choreFormContent_constraintsSection_defaultsExpanded_showingEvenOddAndWeekdaysOnlyControls() {
        composeTestRule.setContent {
            ChoreFormContent(
                formState = ChoreFormState(scheduleType = ScheduleType.MONTHLY),
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = {},
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Even days only").assertExists()
        composeTestRule.onNodeWithText("Weekdays only").assertExists()
        composeTestRule.onNodeWithText("Wed").assertExists()
    }

    @Test
    fun choreFormContent_constraintsHeader_tappingChevron_collapsesSectionHidingWeekdaysOnlyPicker() {
        composeTestRule.setContent {
            ChoreFormContent(
                formState = ChoreFormState(scheduleType = ScheduleType.MONTHLY),
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = {},
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Collapse constraints").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Weekdays only").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Expand constraints").assertExists()
    }

    @Test
    fun choreFormContent_weekdaysOnlyPicker_tappingUnselectedDayPill_addsToWeekdayConstraint() {
        var latest = ChoreFormState(scheduleType = ScheduleType.MONTHLY)
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        // "Wed" is index 3 in the 0=Sun..6=Sat data model, same indexing as weeklyDays.
        composeTestRule.onNodeWithText("Wed").performScrollTo().performClick()

        assert(latest.weekdayConstraint == setOf(3))
    }

    @Test
    fun choreFormContent_weekdaysOnlyPicker_tappingSelectedDayPill_removesFromWeekdayConstraint() {
        var latest = ChoreFormState(scheduleType = ScheduleType.MONTHLY, weekdayConstraint = setOf(3))
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Wed").performScrollTo().performClick()

        assert(latest.weekdayConstraint == emptySet<Int>())
    }

    @Test
    fun choreFormContent_weeklySchedule_hidesWeekdaysOnlySubPicker_toAvoidRedundantDayPicker() {
        // Regression test for a real collision, not just a test-authoring issue: under WEEKLY,
        // the main "Days of week" picker (issue #100) and the Constraints "weekdays only"
        // sub-picker (issue #103) both back onto weekday selection, so showing both at once is
        // a redundant, confusing pair of controls -- and, concretely, renders two sets of
        // identically-labeled Mon..Sun pills on screen simultaneously.
        composeTestRule.setContent {
            ChoreFormContent(
                formState = ChoreFormState(scheduleType = ScheduleType.WEEKLY),
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = {},
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Even days only").assertExists()
        composeTestRule.onNodeWithText("Weekdays only").assertDoesNotExist()
        composeTestRule.onAllNodesWithText("Wed").assertCountEquals(1)
    }

    // Issue #105: Constraints section exposes a control to choose the condition-not-met
    // behavior (skip vs. delay), matching web. Single-select RadioButton pair, direct-set
    // pattern (like the even/odd constraint), not a Set-membership toggle.

    @Test
    fun choreFormContent_constraintsSection_showsSkipDelayControl_defaultingToSkip() {
        composeTestRule.setContent {
            ChoreFormContent(
                formState = ChoreFormState(scheduleType = ScheduleType.MONTHLY),
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = {},
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("If constraint isn't met").assertExists()
        composeTestRule.onNodeWithText("Skip").assertExists()
        composeTestRule.onNodeWithText("Delay").assertExists()
    }

    @Test
    fun choreFormContent_tappingDelay_setsConstraintNotMetBehaviorToDelay() {
        var latest = ChoreFormState(scheduleType = ScheduleType.MONTHLY)
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Delay").performScrollTo().performClick()

        assert(latest.constraintNotMetBehavior == ConstraintBehavior.DELAY)
    }

    @Test
    fun choreFormContent_tappingSkip_setsConstraintNotMetBehaviorBackToSkip() {
        var latest = ChoreFormState(scheduleType = ScheduleType.MONTHLY, constraintNotMetBehavior = ConstraintBehavior.DELAY)
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Skip").performScrollTo().performClick()

        assert(latest.constraintNotMetBehavior == ConstraintBehavior.SKIP)
    }

    @Test
    fun choreFormContent_weeklySchedule_showsSkipDelayControlWithNoLabelCollision() {
        // Regression guard for the #103 class of bug: WEEKLY renders the most other stuff in
        // this area (the main "Days of week" picker higher up, plus the Constraints section's
        // even/odd control), so assert the new Skip/Delay control's labels each match exactly
        // once and don't collide with anything already on screen under WEEKLY specifically.
        composeTestRule.setContent {
            ChoreFormContent(
                formState = ChoreFormState(scheduleType = ScheduleType.WEEKLY),
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = {},
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onAllNodesWithText("Skip").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Delay").assertCountEquals(1)
        composeTestRule.onNodeWithText("If constraint isn't met").assertExists()
        // The weekdays-only sub-picker stays hidden under WEEKLY (issue #103), and the new
        // control doesn't reintroduce the Mon..Sun collision it was hidden to avoid.
        composeTestRule.onNodeWithText("Weekdays only").assertDoesNotExist()
        composeTestRule.onAllNodesWithText("Wed").assertCountEquals(1)
    }

    @Test
    fun choreFormContent_yearlySchedule_hidesConstraintsSectionEntirely() {
        composeTestRule.setContent {
            ChoreFormContent(
                formState = ChoreFormState(scheduleType = ScheduleType.YEARLY),
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = {},
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Weekdays only").assertDoesNotExist()
        composeTestRule.onNodeWithText("If constraint isn't met").assertDoesNotExist()
    }

    // Issue #108: points selector and eligible-people picker restyled as pill toggle buttons.
    // The points selector was already a FilterChip (a Material3 pill-shaped toggle) and keeps
    // that widget, just with a fuller pill shape -- these tests are a functional regression
    // check. The eligible-people picker moves from CheckboxRow to FilterChip-based pills, so its
    // tests exercise the new Set-membership toggle wiring for both AssignmentType.OPEN and
    // AssignmentType.ROTATING (both use the shared EligiblePeoplePillRow).

    @Test
    fun choreFormContent_tappingUnselectedPointOption_setsPoints() {
        var latest = ChoreFormState(points = 1)
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = emptyList(),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("5").performScrollTo().performClick()

        assert(latest.points == 5)
    }

    @Test
    fun choreFormContent_openAssignment_tappingUnselectedPersonPill_addsToEligiblePeople() {
        var latest = ChoreFormState(assignmentType = AssignmentType.OPEN)
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = listOf("alice", "bob"),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("alice").performScrollTo().performClick()

        assert(latest.eligiblePeople == setOf("alice"))
    }

    @Test
    fun choreFormContent_openAssignment_tappingSelectedPersonPill_removesFromEligiblePeople() {
        var latest = ChoreFormState(assignmentType = AssignmentType.OPEN, eligiblePeople = setOf("alice"))
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = listOf("alice", "bob"),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("alice").performScrollTo().performClick()

        assert(latest.eligiblePeople == emptySet<String>())
    }

    @Test
    fun choreFormContent_rotatingAssignment_tappingUnselectedPersonPill_addsToEligiblePeople() {
        var latest = ChoreFormState(assignmentType = AssignmentType.ROTATING)
        composeTestRule.setContent {
            ChoreFormContent(
                formState = latest,
                availablePeople = listOf("alice", "bob"),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = { update -> latest = update(latest) },
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("bob").performScrollTo().performClick()

        assert(latest.eligiblePeople == setOf("bob"))
    }

    @Test
    fun choreFormContent_fixedAssignment_doesNotRenderEligiblePeoplePills() {
        composeTestRule.setContent {
            ChoreFormContent(
                formState = ChoreFormState(assignmentType = AssignmentType.FIXED),
                availablePeople = listOf("alice", "bob"),
                saveState = UiState.Idle,
                isEditMode = false,
                onFormChange = {},
                onSave = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Eligible people (optional)").assertDoesNotExist()
        composeTestRule.onNodeWithText("Rotation (2+ people)").assertDoesNotExist()
    }
}
