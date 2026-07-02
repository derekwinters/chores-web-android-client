package com.derekwinters.chores.ui.log

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.ui.UiState
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #19 behaviors: row expand shows amendment diffs, pagination controls (area: ui, android).
 * Exercises [ActivityLogContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ActivityLogContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val amendment = LogEntry(
        id = 1,
        timestamp = "2026-07-01",
        targetType = "chore",
        action = "updated",
        actor = "alice",
        targetName = "Dishes",
        reassignedTo = null,
        fieldName = "points",
        oldValue = "5",
        newValue = "8"
    )

    @Test
    fun activityLogContent_expandAmendment_showsFieldDiff() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("updated: Dishes").performClick()

        composeTestRule.onNodeWithText("points: 5 -> 8").assertExists()
    }

    @Test
    fun activityLogContent_previousPageDisabledOnFirstPage() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(emptyList(), total = 0, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Previous").assertIsNotEnabled()
    }
}
