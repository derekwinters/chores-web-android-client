package com.derekwinters.chores.ui.chores

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.chores.Chore
import com.derekwinters.chores.common.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behavior: Chore list screen renders name/assignee-or-Completer/points/state/next_due, and
 * exposes a Complete action per chore (area: ui, network)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChoreListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val assignedChore = Chore(
        id = 1, name = "Dishes", currentAssignee = "alice", points = 5, state = "due", nextDue = "2026-07-05"
    )
    private val unassignedChore = Chore(
        id = 2, name = "Vacuum", currentAssignee = null, points = 3, state = "overdue", nextDue = null
    )

    @Test
    fun choreListScreen_success_rendersChoreDetails() {
        composeTestRule.setContent {
            ChoreListScreen(
                uiState = UiState.Success(listOf(assignedChore, unassignedChore)),
                onCompleteClicked = {},
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("Dishes").assertExists()
        composeTestRule.onNodeWithText("alice").assertExists()
        composeTestRule.onNodeWithText("5 points").assertExists()
        composeTestRule.onNodeWithText("Next due: 2026-07-05").assertExists()

        composeTestRule.onNodeWithText("Vacuum").assertExists()
        composeTestRule.onNodeWithText("Needs a Completer").assertExists()
        composeTestRule.onNodeWithText("3 points").assertExists()
    }

    @Test
    fun choreListScreen_completeButton_invokesOnCompleteClickedWithThatChore() {
        var clicked: Chore? = null

        composeTestRule.setContent {
            ChoreListScreen(
                uiState = UiState.Success(listOf(assignedChore)),
                onCompleteClicked = { clicked = it },
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("Complete").performClick()

        assert(clicked == assignedChore)
    }

    @Test
    fun choreListScreen_error_showsMessageAndRetryButton() {
        var retried = false

        composeTestRule.setContent {
            ChoreListScreen(
                uiState = UiState.Error("Couldn't reach the server — check your connection and server URL"),
                onCompleteClicked = {},
                onRetry = { retried = true }
            )
        }

        composeTestRule.onNodeWithText("Couldn't reach the server — check your connection and server URL")
            .assertExists()
        composeTestRule.onNodeWithText("Retry").performClick()

        assert(retried)
    }

    @Test
    fun choreListScreen_emptySuccess_showsEmptyState() {
        composeTestRule.setContent {
            ChoreListScreen(
                uiState = UiState.Success(emptyList()),
                onCompleteClicked = {},
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("No chores yet").assertExists()
    }
}
