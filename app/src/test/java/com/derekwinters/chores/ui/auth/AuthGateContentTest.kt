package com.derekwinters.chores.ui.auth

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #11 behavior: server URL entry gates Setup vs. Login (area: ui, android). Exercises
 * [AuthGateContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class AuthGateContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun authGateContent_enterServerUrl_submitsTrimmedUrl() {
        var captured: String? = null
        composeTestRule.setContent {
            AuthGateContent(
                state = AuthGateState.EnterServerUrl,
                onCheckServer = { captured = it },
                onRetry = {},
                loginContent = { Text("Login for $it") },
                setupContent = { Text("Setup for $it") }
            )
        }

        composeTestRule.onNodeWithText("Server URL").performTextInput("http://chores.example.com")
        composeTestRule.onNodeWithText("Continue").performClick()

        assert(captured == "http://chores.example.com")
    }

    @Test
    fun authGateContent_showLogin_rendersLoginSlot() {
        composeTestRule.setContent {
            AuthGateContent(
                state = AuthGateState.ShowLogin("http://chores.example.com"),
                onCheckServer = {},
                onRetry = {},
                loginContent = { Text("Login for $it") },
                setupContent = { Text("Setup for $it") }
            )
        }

        composeTestRule.onNodeWithText("Login for http://chores.example.com").assertExists()
    }

    @Test
    fun authGateContent_showSetup_rendersSetupSlot() {
        composeTestRule.setContent {
            AuthGateContent(
                state = AuthGateState.ShowSetup("http://chores.example.com"),
                onCheckServer = {},
                onRetry = {},
                loginContent = { Text("Login for $it") },
                setupContent = { Text("Setup for $it") }
            )
        }

        composeTestRule.onNodeWithText("Setup for http://chores.example.com").assertExists()
    }

    @Test
    fun authGateContent_error_showsRetryButton() {
        var retried = false
        composeTestRule.setContent {
            AuthGateContent(
                state = AuthGateState.Error("Unable to reach the server."),
                onCheckServer = {},
                onRetry = { retried = true },
                loginContent = { Text("Login for $it") },
                setupContent = { Text("Setup for $it") }
            )
        }

        composeTestRule.onNodeWithText("Unable to reach the server.").assertExists()
        composeTestRule.onNodeWithText("Retry").performClick()
        assert(retried)
    }
}
