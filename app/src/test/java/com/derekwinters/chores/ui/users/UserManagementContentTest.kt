package com.derekwinters.chores.ui.users

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.Person
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #18 behaviors: grouped Administrators/Members list, create user, and per-user History
 * link (area: ui, android). Exercises [UserManagementContent] directly (no Hilt component
 * needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class UserManagementContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val admin = Person(1, "admin", "Admin", isAdmin = true, goal7d = 12, goal30d = 50)
    private val member = Person(2, "alice", "Alice", isAdmin = false, goal7d = 12, goal30d = 50)

    @Test
    fun userManagementContent_groupsAdminsAndMembers() {
        composeTestRule.setContent {
            UserManagementContent(
                uiState = UiState.Success(listOf(admin, member)),
                actionState = UiState.Idle,
                onCreate = { _, _ -> },
                onUpdate = { _, _, _, _, _, _, _ -> },
                onDelete = {},
                onDismissActionError = {},
                onHistoryClick = {}
            )
        }

        composeTestRule.onNodeWithText("Administrators").assertExists()
        composeTestRule.onNodeWithText("Admin").assertExists()
        composeTestRule.onNodeWithText("Members").assertExists()
        composeTestRule.onNodeWithText("Alice").assertExists()
    }

    @Test
    fun userManagementContent_createUser_invokesCallback() {
        var created: Pair<String, String>? = null
        composeTestRule.setContent {
            UserManagementContent(
                uiState = UiState.Success(listOf(admin)),
                actionState = UiState.Idle,
                onCreate = { name, password -> created = name to password },
                onUpdate = { _, _, _, _, _, _, _ -> },
                onDelete = {},
                onDismissActionError = {},
                onHistoryClick = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Add user").performClick()
        composeTestRule.onNodeWithText("Display Name").performTextInput("Bob")
        composeTestRule.onNodeWithText("Password").performTextInput("secret123")
        composeTestRule.onNodeWithText("Create").performClick()

        assert(created == ("Bob" to "secret123"))
    }

    @Test
    fun userManagementContent_historyClick_invokesCallbackWithUsername() {
        var historyUsername: String? = null
        composeTestRule.setContent {
            UserManagementContent(
                uiState = UiState.Success(listOf(admin)),
                actionState = UiState.Idle,
                onCreate = { _, _ -> },
                onUpdate = { _, _, _, _, _, _, _ -> },
                onDelete = {},
                onDismissActionError = {},
                onHistoryClick = { historyUsername = it }
            )
        }

        composeTestRule.onNodeWithText("History").performClick()

        assert(historyUsername == "admin")
    }

    @Test
    fun userManagementContent_displayName_isVisuallyLargerThanUsername() {
        composeTestRule.setContent {
            UserManagementContent(
                uiState = UiState.Success(listOf(admin)),
                actionState = UiState.Idle,
                onCreate = { _, _ -> },
                onUpdate = { _, _, _, _, _, _, _ -> },
                onDelete = {},
                onDismissActionError = {},
                onHistoryClick = {}
            )
        }

        val nameHeight = composeTestRule.onNodeWithText("Admin").getUnclippedBoundsInRoot().height
        val usernameHeight = composeTestRule.onNodeWithText("admin").getUnclippedBoundsInRoot().height

        assert(nameHeight > usernameHeight)
    }
}
