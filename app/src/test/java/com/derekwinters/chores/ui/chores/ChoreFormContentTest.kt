package com.derekwinters.chores.ui.chores

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

        composeTestRule.onNodeWithText("Cancel").performClick()

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
}
