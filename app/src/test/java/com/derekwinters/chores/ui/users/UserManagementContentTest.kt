package com.derekwinters.chores.ui.users

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.Person
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Reads the requested [androidx.compose.ui.text.TextStyle.fontSize] directly off the node's
 * text layout result, rather than measuring rendered pixel bounds — Robolectric's headless text
 * measurement doesn't reliably scale reported bounds with font size, so bounds-based "is this
 * visually bigger" assertions are flaky here even when the styling is correct.
 */
private fun SemanticsNodeInteraction.textFontSizeSp(): Float {
    val results = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
    return results.first().layoutInput.style.fontSize.value
}

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

        // Issue #90: section headers render uppercase ("ADMINISTRATORS"/"MEMBERS").
        composeTestRule.onNodeWithText("ADMINISTRATORS").assertExists()
        composeTestRule.onNodeWithText("Admin").assertExists()
        composeTestRule.onNodeWithText("MEMBERS").assertExists()
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

    /** Issue #86 behavior: user row shows a visible Edit action (area: ui). */
    @Test
    fun userManagementContent_editIcon_opensEditDialog() {
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

        composeTestRule.onNodeWithTag("personEdit_${admin.id}").performClick()

        composeTestRule.onNodeWithText("Edit User").assertExists()
    }

    /** Issue #86 behavior: user row shows a visible Delete action styled in red (area: ui). */
    @Test
    fun userManagementContent_deleteIcon_confirmThenInvokesCallback() {
        var deletedId: Int? = null
        composeTestRule.setContent {
            UserManagementContent(
                uiState = UiState.Success(listOf(admin)),
                actionState = UiState.Idle,
                onCreate = { _, _ -> },
                onUpdate = { _, _, _, _, _, _, _ -> },
                onDelete = { deletedId = it },
                onDismissActionError = {},
                onHistoryClick = {}
            )
        }

        composeTestRule.onNodeWithTag("personDelete_${admin.id}").performClick()
        composeTestRule.onNodeWithText("Delete this user?").assertExists()
        composeTestRule.onNodeWithText("Delete").performClick()

        assert(deletedId == admin.id)
    }

    @Test
    fun userManagementContent_displayName_usesLargerTypographyThanUsername() {
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

        // Issue #82 added a role pill whose label ("Admin") collides with this fixture's display
        // name, so the display-name node is looked up by its dedicated test tag rather than text.
        val nameFontSize = composeTestRule.onNodeWithTag("personDisplayName_${admin.id}", useUnmergedTree = true).textFontSizeSp()
        val usernameFontSize = composeTestRule.onNodeWithText("admin", useUnmergedTree = true).textFontSizeSp()

        assert(nameFontSize > usernameFontSize)
    }

    /** Issue #82 behavior: each user row displays an admin/member role pill badge (area: ui). */
    @Test
    fun userManagementContent_personRow_showsRolePillBadge() {
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

        composeTestRule.onNodeWithTag("personRolePill_${admin.id}", useUnmergedTree = true).assertTextEquals("Admin")
        composeTestRule.onNodeWithTag("personRolePill_${member.id}", useUnmergedTree = true).assertTextEquals("Member")
    }

    /** Issue #78 behavior: each user row displays an avatar circle with the name's initial (area: ui). */
    @Test
    fun userManagementContent_personRow_showsAvatarInitial() {
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

        // Both "Admin" and "Alice" start with "A", so both rows' avatars render "A".
        composeTestRule.onAllNodesWithText("A", useUnmergedTree = true).assertCountEquals(2)
    }

    /** Issue #90 behavior: section headers render uppercase (area: ui). */
    @Test
    fun userManagementContent_sectionHeaders_renderUppercase() {
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

        composeTestRule.onNodeWithText("ADMINISTRATORS").assertExists()
        composeTestRule.onNodeWithText("MEMBERS").assertExists()
        // Lowercase/title-case originals must not appear verbatim as the header text.
        composeTestRule.onNodeWithText("Administrators").assertDoesNotExist()
        composeTestRule.onNodeWithText("Members").assertDoesNotExist()
    }
}
