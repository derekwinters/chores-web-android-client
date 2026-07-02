package com.derekwinters.chores.ui.login

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.common.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behavior: Login screen has server URL + username/password fields, calls onLogin on submit
 * (area: android, ui, network)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_submittingForm_invokesOnLoginWithEnteredValues() {
        var capturedServerUrl: String? = null
        var capturedUsername: String? = null
        var capturedPassword: String? = null

        composeTestRule.setContent {
            LoginScreen(
                uiState = null,
                onLogin = { serverUrl, username, password ->
                    capturedServerUrl = serverUrl
                    capturedUsername = username
                    capturedPassword = password
                }
            )
        }

        composeTestRule.onNodeWithText("Server URL").performTextInput("http://192.168.1.20:8000")
        composeTestRule.onNodeWithText("Username").performTextInput("alice")
        composeTestRule.onNodeWithText("Password").performTextInput("hunter2")
        composeTestRule.onNodeWithText("Log In").performClick()

        assert(capturedServerUrl == "http://192.168.1.20:8000")
        assert(capturedUsername == "alice")
        assert(capturedPassword == "hunter2")
    }

    @Test
    fun loginScreen_errorState_displaysErrorMessage() {
        composeTestRule.setContent {
            LoginScreen(
                uiState = UiState.Error("Session expired, please log in"),
                onLogin = { _, _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Session expired, please log in").assertExists()
    }

    @Test
    fun loginScreen_loadingState_disablesLoginButton() {
        composeTestRule.setContent {
            LoginScreen(
                uiState = UiState.Loading,
                onLogin = { _, _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Log In").assertDoesNotExist()
    }
}
