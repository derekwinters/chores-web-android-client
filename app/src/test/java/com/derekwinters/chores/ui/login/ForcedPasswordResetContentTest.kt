package com.derekwinters.chores.ui.login

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
 * Issue #11 behavior: "set new password" form (min 8 chars, confirm) (area: ui, android).
 * Exercises [ForcedPasswordResetContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ForcedPasswordResetContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun forcedPasswordReset_tooShort_disablesSubmit() {
        composeTestRule.setContent {
            ForcedPasswordResetContent(uiState = UiState.Idle, onSubmit = {}, onCancel = {})
        }

        composeTestRule.onNodeWithText("New Password").performTextInput("short")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("short")

        composeTestRule.onNodeWithText("Password must be at least 8 characters").assertExists()
        composeTestRule.onNodeWithText("Set New Password").assertIsNotEnabled()
    }

    @Test
    fun forcedPasswordReset_validAndMatching_submits() {
        var submitted: String? = null
        composeTestRule.setContent {
            ForcedPasswordResetContent(uiState = UiState.Idle, onSubmit = { submitted = it }, onCancel = {})
        }

        composeTestRule.onNodeWithText("New Password").performTextInput("newpassword123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("newpassword123")
        composeTestRule.onNodeWithText("Set New Password").performClick()

        assert(submitted == "newpassword123")
    }

    @Test
    fun forcedPasswordReset_cancel_invokesCallback() {
        var cancelled = false
        composeTestRule.setContent {
            ForcedPasswordResetContent(uiState = UiState.Idle, onSubmit = {}, onCancel = { cancelled = true })
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        assert(cancelled)
    }
}
