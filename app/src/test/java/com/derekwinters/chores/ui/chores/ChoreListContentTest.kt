package com.derekwinters.chores.ui.chores

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behaviors: "Chore list screen: GET /chores, render name/assignee-or-Completer/points/state/
 * next_due" and "Complete-chore action ... with Completer-picker dialog when
 * current_assignee == null" (area: ui, android, network). Exercises [ChoreListContent] directly
 * (no Hilt component needed) — see ChoreListViewModelTest for the ViewModel-level state machine.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChoreListContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val assignedChore = Chore(
        id = 1,
        name = "Dishes",
        points = 5,
        state = "due",
        nextDue = "2026-07-05",
        currentAssignee = "alice",
        eligiblePeople = listOf("alice", "bob")
    )
    private val unassignedChore = Chore(
        id = 2,
        name = "Trash",
        points = 3,
        state = "due",
        nextDue = null,
        currentAssignee = null,
        eligiblePeople = listOf("alice", "bob")
    )

    @Test
    fun choreListContent_rendersNameAssigneePointsStateNextDue() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Dishes").assertExists()
        composeTestRule.onNodeWithText("alice").assertExists()
        composeTestRule.onNodeWithText("5 points").assertExists()
        composeTestRule.onNodeWithText("due").assertExists()
        composeTestRule.onNodeWithText("Next due: 2026-07-05").assertExists()
    }

    @Test
    fun choreListContent_unassignedChore_showsCompleterPlaceholder() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(unassignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Completer").assertExists()
    }

    @Test
    fun choreListContent_completingAssignedChore_completesWithoutDialog() {
        var completed: Pair<Chore, String?>? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { chore, completedBy -> completed = chore to completedBy }
            )
        }

        composeTestRule.onNodeWithText("Complete").performClick()

        assert(completed == (assignedChore to null))
    }

    @Test
    fun choreListContent_completingUnassignedChore_opensCompleterPickerAndConfirms() {
        var completed: Pair<Chore, String?>? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(unassignedChore)),
                completingChoreId = null,
                onComplete = { chore, completedBy -> completed = chore to completedBy }
            )
        }

        composeTestRule.onNodeWithText("Complete").performClick()
        composeTestRule.onNodeWithText("Who completed this?").assertExists()

        composeTestRule.onNodeWithText("bob").performClick()
        composeTestRule.onNodeWithText("Confirm").performClick()

        assert(completed == (unassignedChore to "bob"))
    }

    @Test
    fun choreListContent_errorState_showsErrorMessage() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Error("Something went wrong. Please try again."),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Something went wrong. Please try again.").assertExists()
    }

    @Test
    fun choreListContent_search_invokesOnQueryChange() {
        var query: String? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                onQueryChange = { query = it }
            )
        }

        composeTestRule.onNodeWithText("Search chores").performTextInput("dish")

        assert(query == "dish")
    }

    @Test
    fun choreListContent_activeFilters_showsCountAndClearAction() {
        var cleared = false
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                totalCount = 2,
                filters = ChoreFilters(query = "dish"),
                onFiltersChange = { cleared = it == ChoreFilters() }
            )
        }

        composeTestRule.onNodeWithText("Showing 1 of 2 chores").assertExists()
        composeTestRule.onNodeWithText("Clear filters").performClick()

        assert(cleared)
    }

    @Test
    fun choreListContent_expandDueChore_showsSkipAndHistoryNotMarkDue() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()

        composeTestRule.onNodeWithText("Skip").assertExists()
        composeTestRule.onNodeWithText("History").assertExists()
        composeTestRule.onNodeWithText("Mark Due Now").assertDoesNotExist()
    }

    @Test
    fun choreListContent_expandNotDueChore_showsMarkDueNotSkip() {
        val notDueChore = assignedChore.copy(state = "not_due")
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(notDueChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()

        composeTestRule.onNodeWithText("Mark Due Now").assertExists()
        composeTestRule.onNodeWithText("Skip").assertDoesNotExist()
    }

    @Test
    fun choreListContent_skipAction_invokesCallback() {
        var skipped: Chore? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                onSkip = { skipped = it }
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()
        composeTestRule.onNodeWithText("Skip").performClick()

        assert(skipped == assignedChore)
    }

    @Test
    fun choreListContent_deleteAction_requiresConfirmation() {
        var deleted: Chore? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                onDelete = { deleted = it }
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("This also removes all points history for this chore and cannot be undone.").assertExists()

        composeTestRule.onAllNodesWithText("Delete")[1].performClick()

        assert(deleted == assignedChore)
    }

    @Test
    fun choreListContent_historyAction_invokesCallback() {
        var history: Chore? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                onHistory = { history = it }
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()
        composeTestRule.onNodeWithText("History").performClick()

        assert(history == assignedChore)
    }
}
