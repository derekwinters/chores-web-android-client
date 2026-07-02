package com.derekwinters.chores.ui.setup

import androidx.compose.ui.test.assertIsNotEnabled
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
 * Issue #11 behavior: "First-run setup ... Display Name + Password... a 'Require Authentication'
 * toggle" (area: ui, android). Exercises [SetupContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SetupContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setupContent_mismatchedPasswords_disablesSubmit() {
        composeTestRule.setContent {
            SetupContent(uiState = UiState.Idle, onCreateAccount = { _, _, _ -> })
        }

        composeTestRule.onNodeWithText("Username").performTextInput("admin")
        composeTestRule.onNodeWithText("Password").performTextInput("secret123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("different")

        composeTestRule.onNodeWithText("Passwords do not match").assertExists()
        composeTestRule.onNodeWithText("Create Account").assertIsNotEnabled()
    }

    @Test
    fun setupContent_validInput_submitsWithToggleValue() {
        var captured: Triple<String, String, Boolean>? = null
        composeTestRule.setContent {
            SetupContent(
                uiState = UiState.Idle,
                onCreateAccount = { username, password, requireAuth -> captured = Triple(username, password, requireAuth) }
            )
        }

        composeTestRule.onNodeWithText("Username").performTextInput("admin")
        composeTestRule.onNodeWithText("Password").performTextInput("secret123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("secret123")
        composeTestRule.onNodeWithText("Create Account").performClick()

        assert(captured == Triple("admin", "secret123", true))
    }
}
