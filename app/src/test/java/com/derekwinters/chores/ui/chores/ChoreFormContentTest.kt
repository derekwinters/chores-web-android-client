package com.derekwinters.chores.ui.chores

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

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
    }
}
