package com.derekwinters.chores.ui.chores

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Issue #14 behavior: collapsible stats panel (area: ui, android). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChoresStatsPanelContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val stats = ChoresStats(totalEnabledChores = 5, totalPoints = 27, completedLast7Days = 14, dueNext7DaysPoints = 8)

    @Test
    fun statsPanel_expanded_showsAllFourStats() {
        composeTestRule.setContent {
            ChoresStatsPanelContent(uiState = UiState.Success(stats), initiallyExpanded = true)
        }

        composeTestRule.onNodeWithText("5").assertExists()
        composeTestRule.onNodeWithText("27").assertExists()
        composeTestRule.onNodeWithText("14").assertExists()
        composeTestRule.onNodeWithText("8").assertExists()
    }

    @Test
    fun statsPanel_collapseThenExpand_togglesVisibility() {
        composeTestRule.setContent {
            ChoresStatsPanelContent(uiState = UiState.Success(stats), initiallyExpanded = true)
        }

        composeTestRule.onNodeWithContentDescription("Collapse stats panel").performClick()
        composeTestRule.onNodeWithText("27").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Expand stats panel").performClick()
        composeTestRule.onNodeWithText("27").assertExists()
    }

    @Test
    fun statsPanel_valueText_isVisuallyLargerThanLabelText() {
        composeTestRule.setContent {
            ChoresStatsPanelContent(uiState = UiState.Success(stats), initiallyExpanded = true)
        }

        val labelHeight = composeTestRule.onNodeWithText("Total Chores").getUnclippedBoundsInRoot().height
        val valueHeight = composeTestRule.onNodeWithText("5").getUnclippedBoundsInRoot().height

        assert(valueHeight > labelHeight)
    }
}
