package com.derekwinters.chores.ui.dashboard

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #12/#17 behaviors: Due Now/Due Soon deep links and tapping a card to open User Detail
 * (area: ui, android). Exercises [DashboardContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DashboardContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val card = DashboardCard(
        personId = 1,
        username = "alice",
        displayName = "Alice",
        points7d = 10,
        goal7d = 12,
        points30d = 40,
        goal30d = 50,
        dueNowCount = 2,
        dueSoonCount = 1
    )

    @Test
    fun dashboardContent_tapCard_navigatesToUserDetail() {
        var navigated: Pair<Int, String>? = null
        composeTestRule.setContent {
            DashboardContent(
                uiState = UiState.Success(listOf(card)),
                navActions = DashboardNavActions(onNavigateToUserDetail = { id, username -> navigated = id to username })
            )
        }

        composeTestRule.onNodeWithText("Alice").performClick()

        assert(navigated == (1 to "alice"))
    }

    @Test
    fun dashboardContent_tapDueNow_navigatesToChoresWithAssignee() {
        var navigated: Pair<String?, String?>? = null
        composeTestRule.setContent {
            DashboardContent(
                uiState = UiState.Success(listOf(card)),
                navActions = DashboardNavActions(onNavigateToChores = { assignee, dueWithin -> navigated = assignee to dueWithin })
            )
        }

        composeTestRule.onNodeWithText("Due Now: 2").performClick()

        assert(navigated == ("alice" to null))
    }
}
