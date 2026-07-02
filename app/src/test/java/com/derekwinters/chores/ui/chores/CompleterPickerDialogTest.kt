package com.derekwinters.chores.ui.chores

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.chores.Person
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behavior: Completer-picker dialog for chores where current_assignee == null, mirroring
 * chores-web's CompleteWithActorModal.jsx (area: ui, android, network)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class CompleterPickerDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val people = listOf(Person("alice", "Alice"), Person("bob", "Bob"))

    @Test
    fun completerPickerDialog_listsPeopleByName() {
        composeTestRule.setContent {
            CompleterPickerDialog(
                choreName = "Vacuum",
                people = people,
                onConfirm = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Who completed \"Vacuum\"?").assertExists()
        composeTestRule.onNodeWithText("Alice").assertExists()
        composeTestRule.onNodeWithText("Bob").assertExists()
    }

    @Test
    fun completerPickerDialog_selectingPersonAndConfirming_invokesOnConfirmWithUsername() {
        var confirmedUsername: String? = null

        composeTestRule.setContent {
            CompleterPickerDialog(
                choreName = "Vacuum",
                people = people,
                onConfirm = { confirmedUsername = it },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Bob").performClick()
        composeTestRule.onNodeWithText("Complete").performClick()

        assert(confirmedUsername == "bob")
    }

    @Test
    fun completerPickerDialog_cancel_invokesOnDismiss() {
        var dismissed = false

        composeTestRule.setContent {
            CompleterPickerDialog(
                choreName = "Vacuum",
                people = people,
                onConfirm = {},
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()

        assert(dismissed)
    }
}
